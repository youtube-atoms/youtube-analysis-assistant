package ya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class DefaultAnalysisClient implements AnalysisClient {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AiClient yac;

	private final Engine engine;

	private final ObjectMapper objectMapper;

	DefaultAnalysisClient(AiClient yac, Engine engine, ObjectMapper objectMapper) {
		this.yac = yac;
		this.engine = engine;
		this.objectMapper = objectMapper;
	}

	@Override
	public Analysis analyzeVideo(String id, Resource video) {
		try {
			var audio = this.engine.videoToAudio(video.getFile(), id);
			return analyzeAudio(id, new FileSystemResource(audio));
		} //
		catch (Throwable throwable) {
			throw new RuntimeException("oops!", throwable);
		}
	}

	private String findXmlContent(String body, String tag) {
		try {
			Assert.hasText(tag, "the tag can't be empty");
			Assert.hasText(body, "the body can't be empty");
			var before = "<" + tag + ">";
			var after = "</" + tag + ">";
			Assert.state(body.contains(before) && body.contains(after), "there must be XML tags of the type described in this string");
			var start = body.indexOf(before) + before.length();
			var stop = body.indexOf(after);
			return body.substring(start, stop);
		}//
		catch (Throwable throwable) {
			log.error("couldn't parse [" + body + "] for the XML tag [" + tag + "]");
			return null;
		}
	}

	private Map<String, Object> mapFrom(String xml) {
		var m = new HashMap<String, Object>();
		for (var tagName : Set.of("description", "twitter", "linkedin", "viral-potential", "content-enhancement",
				"thumbnail-design", "seo-tags", "engaging-title", "summary")) {
			var result = findXmlContent(xml, tagName);
			if (StringUtils.hasText(result))
				m.put(tagName, result);
		}
		return m;
	}

	@Override
	public Analysis analyzeAudio(String id, Resource audio) {
		try {
			var transcript = FileCopyUtils
				.copyToString(new FileReader(this.engine.audioToTranscript(this.yac, audio.getFile(), id)));

			var questions = """
					    You are a expert content editor. Your first task is to provide a concise 4-6 sentence 6th grade reading level summary of the given text as if you were preparing an introduction for a personal blog post.

					    Begin your summary with a phrase such as 'In this post' or 'In this interview,' setting the stage for what the reader can expect.

					    %s

					    Provide Summary Here, wrapped in XML tags <summary> and </summary>:



					    Your second task is to provide your responses to the following inquiries in the form of bullet points:

					    Answer Inquiries Here:

						Engaging Title, wrapped in XML tags <engaging-title> and </engaging-title>: Propose a catchy and appealing title that encapsulates the essence of the content.
						SEO Tags, wrapped in XML tags <seo-tags> and </seo-tags>: Identify a list of SEO-friendly tags, seperated by commas, that are relevant to the content and could improve its searchability.
						Thumbnail Design, wrapped in XML tags <thumbnail-design> and </thumbnail-design>: Describe the elements of an eye-catching thumbnail that would compel viewers to click.
						Content Enhancement, wrapped in XML tags <content-enhancement> and </content-enhancement>: Offer specific suggestions on how the content could be improved for viewer engagement and retention.
						Viral Potential Segment, wrapped in XML tags <viral-potential> and </viral-potential>: Identify the best section that might have the potential to be engaging or entertaining for a short-form viral video based on factors like humor, uniqueness, relatability, or other notable elements.  Provide the text section and explain why.
						Create and provide an engaging viral LinkedIn post that would entice viewers to watch the video, wrapped in XML tags <linkedin> and </linkedin>
						Create and provide an engaging viral Twitter post that would entice viewers to watch the video, wrapped in XML tags <twitter> and </twitter>
						Create and provide a summary description of the video that would entice viewers to watch the video, wrapped in XML tags <description> and </description>

					""";

			var analysis = "<analysis>" + this.yac.chat(questions.formatted(transcript)) + "</analysis>";
			System.out.println(analysis);
			var map = mapFrom(analysis);

			for (var k : map.keySet()) {
				var value = (String) map.get(k);
				if (StringUtils.hasText(value)) {
					for (var xml : Set.of("<" + k + ">", "</" + k + ">")) {
						if (value.contains(xml)) {
							var parts = value.split(xml);
							var sb = new StringBuilder();
							for (var p : parts) sb.append(p);
							value = sb.toString();
						}
					}
				}
				map.put(k, value);
			}

			replaceMapKey(map, "content-enhancement",
					input -> cleanArrayOfStrings(input.contains("\n") ? input.split("\n") : new String[]{input}));
			replaceMapKey(map, "seo-tags", seoTags -> cleanArrayOfStrings(seoTags.contains(",") ? seoTags.split(",") : new String[]{seoTags}));

			for (var st : (String[]) map.get("seo-tags"))
				log.info("seo tag suggestion: [" + st + ']');

			var thumbnailPrompt = (String) (map.getOrDefault("thumbnail-design", null));
			var thumbResource = (Resource) (thumbnailPrompt == null ? null
					: this.yac.render(thumbnailPrompt, ImageSize.SIZE_1024x1792));

			for (var k : map.keySet()) {
				var v = map.get(k);
				if (v instanceof String str) {
					map.put(k, str.trim());
				}
			}
			var json = objectMapper.writeValueAsString(map);
			log.info("json from serializing the Map<String,Object>: [" + json + "]");
			var contentAnalysis = objectMapper.readValue(json, Analysis.ContentAnalysis.class);
			return new Analysis(contentAnalysis, audio, null, thumbResource, transcript);
		} //
		catch (Throwable throwable) {
			throw new RuntimeException("oops!", throwable);
		}
	}

	private String[] cleanArrayOfStrings(String[] input) {
		var list = new ArrayList<String>();
		for (var i : input)
			if (StringUtils.hasText(i))
				list.add(i.trim());
		return list.toArray(new String[0]);
	}

	private void replaceMapKey(Map<String, Object> map, String key, Function<String, String[]> function) {
		if (map.containsKey(key)) {
			if (map.get(key) instanceof String s) {
				var arr = function.apply(s);
				map.put(key, arr);
			}
		}
	}

}
