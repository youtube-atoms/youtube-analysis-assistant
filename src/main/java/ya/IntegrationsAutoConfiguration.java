package ya;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

@Configuration
class IntegrationsAutoConfiguration {

	private final Engine engine;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<String> audio = Set.of("mp3", "wav");

    private final Set<String> video = Set.of("mp4", "mov");

	IntegrationsAutoConfiguration(Engine engine) {
		this.engine = engine;
	}

	@Bean
    InitializingBean directoryInitialization(YaProperties properties) {
        return () -> {

            record IO(File in, File out) {
            }

            var video = properties.application().video();
            var audio = properties.application().audio();
            var map = Map.of(//
                    "video", new IO(video.input(), video.output()), //
                    "audio", new IO(audio.input(), audio.output())//
            );
            for (var k : map.keySet()) {
                var io = map.get(k);
                log.info("==================================");
                log.info(k.toUpperCase(Locale.ROOT));
                log.info(k + '=' + io);
                for (var f : Set.of(io.in(), io.out()))
                    Assert.state(f.exists() || f.mkdirs(), "the directory [" + f.getAbsolutePath() + "] must exist");
            }
        };
    }

    @Bean
    IntegrationFlow videoToAudioIntegrationFlow(YaProperties properties) {
        var video = properties.application().video();
        return integrationFlow(video.input(), video.output(), file -> engine.isValidFile(file, this.video),
                (payload, headers) -> engine. videoToAudio(payload, Objects.requireNonNull(headers.getId()).toString()),
                message -> message.getHeaders().getId() + ".mp3");
    }

    @Bean
    IntegrationFlow audioToTranscriptIntegrationFlow(YaProperties properties, YaAiClient ai) {
        var audio = properties.application().audio();
        return integrationFlow(audio.input(), audio.output(), f -> engine.isValidFile(f, this.audio),
                (payload, headers) -> engine.audioToTranscript(ai, payload,
                        Objects.requireNonNull(headers.getId()).toString()),
                message -> message.getHeaders().getId() + ".txt");
    }

    private IntegrationFlow integrationFlow(File input, File output, Predicate<File> matchIncomingFile,
                                            GenericHandler<File> fileGenericHandler, FileNameGenerator fileNameGenerator) {
        Assert.state(input.exists(), "the input directory does not exist");
        Assert.state(output.exists(), "the output directory does not exist");
        var inbound = Files.inboundAdapter(input)
                .autoCreateDirectory(true)
                .filterFunction(matchIncomingFile::test)
                .preventDuplicates(true);
        var outbound = Files.outboundAdapter(output)
                .fileNameGenerator(fileNameGenerator)
                .fileExistsMode(FileExistsMode.REPLACE)
                .deleteSourceFiles(true);
        return IntegrationFlow.from(inbound).handle(File.class, fileGenericHandler).handle(outbound).get();
    }

}

@Component
class Engine {

    private final Logger log = LoggerFactory.getLogger(getClass());


    File audioToTranscript(YaAiClient ai, File payload, String id) {
        try {
            var transcript = ai.transcribe(new FileSystemResource(payload));
            var tmp = File.createTempFile("transcript-" + id, ".txt");
            try (var out = new FileWriter(tmp)) {
                FileCopyUtils.copy(transcript, out);
            }
            return tmp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isValidFile(File file, Set<String> extensions) {
        var name = file.getName();
        var lastIndexOf = name.lastIndexOf(".");
        Assert.state(lastIndexOf != -1, "there has to be a file extension in the input file");
        var ext = name.substring(lastIndexOf);
        return extensions.contains(ext.substring(1).toLowerCase(Locale.ROOT).trim());
    }

    File videoToAudio(File inputFile, String uid) {
        try {
            var uuid = Objects.requireNonNull(uid);
            var encodedOutputFile = new File(inputFile.getParentFile(), uuid + ".mp3");
            log.info("encoded output file [" + encodedOutputFile.getAbsolutePath() + "]");
            var ffmpegCommand = List.of("ffmpeg", "-i", inputFile.getAbsolutePath(),
                    encodedOutputFile.getAbsolutePath());
            var exitCode = new ProcessBuilder(ffmpegCommand).inheritIO().start().waitFor();
            Assert.state(exitCode == 0, "the transcoding process did not exit successfully");
            Assert.state(encodedOutputFile.exists(),
                    "the output file [" + encodedOutputFile.getAbsolutePath() + "] was never written");
            return encodedOutputFile;
        } //
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}