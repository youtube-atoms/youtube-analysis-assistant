package ya;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.Resource;

/**
 * high level access to tools to analyze and support a video's content.
 */
public interface AnalysisClient {

	record Analysis(ContentAnalysis analysis, Resource audio, Resource video, Resource image, String transcript) {
		//
		record ContentAnalysis(String twitter, //
				@JsonProperty("engaging-title") String engagingTitle, //
				String summary, //
				String description, //
				String linkedin, //
				@JsonProperty("seo-tags") String[] seoTags, //
				@JsonProperty("thumbnail-design") String thumbnailDesign, //
				@JsonProperty("viral-potential") String viralPotential, //
				@JsonProperty("content-enhancement") String[] contentEnhancement) {
		}
	}

	Analysis analyzeVideo(String uid, Resource video);

	Analysis analyzeAudio(String uid, Resource audio);

}
