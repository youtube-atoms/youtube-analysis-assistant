package ya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class DefaultAnalysisClient implements AnalysisClient {

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

					    Answer Inquiries Here, being sure to use CDATA sections for all the content you've been instructed to put into XML tags:

						Engaging Title, wrapped in XML tags <engaging-title> and </engaging-title>: Propose a catchy and appealing title that encapsulates the essence of the content.
						SEO Tags, wrapped in XML tags <seo-tags> and </seo-tags>: Identify a list of SEO-friendly tags that are relevant to the content and could improve its searchability.
						Thumbnail Design, wrapped in XML tags <thumbnail-design> and </thumbnail-design>: Describe the elements of an eye-catching thumbnail that would compel viewers to click.
						Content Enhancement, wrapped in XML tags <content-enhancement> and </content-enhancement>: Offer specific suggestions on how the content could be improved for viewer engagement and retention.
						Viral Potential Segment, wrapped in XML tags <viral-potential> and </viral-potential>: Identify the best section that might have the potential to be engaging or entertaining for a short-form viral video based on factors like humor, uniqueness, relatability, or other notable elements.  Provide the text section and explain why.
						Create and provide an engaging viral LinkedIn post that would entice viewers to watch the video, wrapped in XML tags <linkedin> and </linkedin>
						Create and provide an engaging viral Twitter post that would entice viewers to watch the video, wrapped in XML tags <twitter> and </twitter>
						Create and provide a summary description of the video that would entice viewers to watch the video, wrapped in XML tags <description> and </description>

					""";

			var analysis = "<analysis>" + this.yac.chat(questions.formatted(transcript)) + "</analysis>";
			System.out.println(analysis);
			var factory = DocumentBuilderFactory.newInstance();
			var builder = factory.newDocumentBuilder();
			var document = builder.parse(new ByteArrayInputStream(analysis.getBytes()));
			var map = new HashMap<String, Object>();
			var nodeList = document.getDocumentElement().getChildNodes();
			for (var i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) instanceof Element element) {
					var tagName = element.getTagName();
					var textContent = element.getTextContent();
					map.put(tagName, textContent);
				}
			}

			replace(map, "content-enhancement",
					input -> input.contains("\n") ? input.split("\n") : new String[] { input });
			replace(map, "seo-tags", seoTags -> seoTags.contains(",") ? seoTags.split(",") : new String[] { seoTags });

			for (var st : (String[]) map.get("seo-tags"))
				System.out.println("st: " + st);

			var thumbnailPrompt = (String) (map.getOrDefault("thumbnail-design", null));
			var thumbResource = (Resource) (thumbnailPrompt == null ? null
					: this.yac.render(thumbnailPrompt, ImageSize.SIZE_1024x1792));
			var json = objectMapper.writeValueAsString(map);
			var contentAnalysis = objectMapper.readValue(json, Analysis.ContentAnalysis.class);
			return new Analysis(contentAnalysis, audio, null, thumbResource, transcript);
		} //
		catch (Throwable throwable) {
			throw new RuntimeException("oops!", throwable);
		}
	}

	private void replace(Map<String, Object> map, String key, Function<String, String[]> function) {
		if (map.containsKey(key)) {
			if (map.get(key) instanceof String s) {
				var arr = function.apply(s);
				map.put(key, arr);
			}
		}
	}

}
