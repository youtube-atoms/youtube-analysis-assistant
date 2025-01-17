package app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;
import ya.ImageSize;
import ya.AiClient;

import java.io.FileOutputStream;

@SpringBootTest
class AnalysisAiClientTestClientClient {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	void transcribe(@Autowired AiClient yac) {
		var transcript = yac.transcribe(new ClassPathResource("sample-audio.mp3"));
		Assertions.assertTrue(StringUtils.hasText(transcript));
		log("transcribe", transcript);
	}

	@Test
	void talk(@Autowired AiClient ya) {
		var joke = ya.chat("tell me a joke?");
		Assertions.assertTrue(StringUtils.hasText(joke));
		log("talk", joke);
	}

	@Test
	void render(@Autowired AiClient yaaClient) throws Exception {
		var prompt = """
				Thumbnail Design: A vibrant background with code snippets and the Java coffee cup logo.
				 The text  "Mastering Java 21 & Spring" in bold, modern font. Icons representing different IDEs
				 like IntelliJ IDEA and VS Code. A 'play' button symbol conveying that the blog post contains
				 a video tutorial.
				""";
		var generationResponse = yaaClient.render(prompt, ImageSize.SIZE_1024x1024);
		log("render", generationResponse.toString());
		try (var inputStream = generationResponse.getInputStream();
				var outputStream = new FileOutputStream(
						SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/out.jpg"))) {
			FileCopyUtils.copy(inputStream, outputStream);
		}
	}

	private void log(String m, Object o) {
		log.info(m + " [" + o + "]");
	}

}