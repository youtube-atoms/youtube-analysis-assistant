package ya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Configuration
class IntegrationsAutoConfiguration {

	@Bean
	IntegrationFlow incomingVideoIntegration(YoutubeAnalysisProperties properties, AnalysisClient yac,
			ObjectMapper objectMapper) {
		var in = properties.application().videos();
		for (var f : List.of(in))
			this.ensure(f);
		var inboundChannelAdapterSpec = Files//
			.inboundAdapter(in)//
			.filterFunction(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".mp4"))//
			.autoCreateDirectory(true)//
			.preventDuplicates(true);
		return IntegrationFlow//
			.from(inboundChannelAdapterSpec)//
			.handle(File.class, (payload, headers) -> {
				var id = Objects.requireNonNull(headers.getId()).toString();
				var projectDirectory = new File(in, id);
				ensure(projectDirectory);
				var payloadToProcess = new File(projectDirectory, payload.getName());
				Assert.state(payload.renameTo(payloadToProcess) && payloadToProcess.exists(),
						"the file [" + payload.getAbsolutePath() + "] you moved to ["
								+ payloadToProcess.getAbsolutePath() + "] does not exist");
				var analysis = yac.analyzeVideo(id, new FileSystemResource(payloadToProcess));
				analyzeVideo(projectDirectory, analysis, objectMapper);
				return null;
			})//
			.get();
	}

	private Resource resourceFor(String transcript) {
		return new InputStreamResource(new ByteArrayInputStream(transcript.getBytes()));
	}

	private void analyzeVideo(File root, AnalysisClient.Analysis youtubeAnalysis, ObjectMapper objectMapper) {
		try {

			var caJson = objectMapper.writeValueAsString(youtubeAnalysis.contentAnalysis());

			var map = Map.of("image.webp", youtubeAnalysis.image(), //
					"transcript.txt", resourceFor(youtubeAnalysis.transcript()), //
					"contentAnalysis.json", resourceFor(caJson), //
					"audio.mp3", youtubeAnalysis.audio()//
			); //
			for (var k : map.keySet()) {
				try (var inputStream = map.get(k).getInputStream();
						var outputStream = new FileOutputStream(new File(root, k))) {
					FileCopyUtils.copy(inputStream, outputStream);
				}
			}
		} //
		catch (Throwable throwable) {
			throw new RuntimeException("couldn't process the " + AnalysisClient.Analysis.class.getName());
		}

	}

	private void ensure(File d) {
		Assert.notNull(d, "the directory must be non-null");
		if (d.isDirectory())
			d = d.getParentFile();
		Assert.state(d.exists() || d.mkdirs(),
				"the directory [" + d.getAbsolutePath() + "] does not exist and could not be created");
	}

	@Bean
	AnalysisClient yaClient(AiClient yac, Engine engine, ObjectMapper objectMapper) {
		return new DefaultAnalysisClient(yac, engine, objectMapper);
	}

	@Bean
	Engine engine() {
		return new Engine();
	}

}
