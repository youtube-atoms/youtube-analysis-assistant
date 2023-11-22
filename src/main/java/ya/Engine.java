package ya;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

// not sure if i'll use this functionality in a EIP flow or
// something and whether it makes sense to keep it loose
// and granular, so for now it's a package private bean
class Engine {

	private final Logger log = LoggerFactory.getLogger(getClass());

	File audioToTranscript(AiClient aiClient, File payload, String id) {
		try {
			var transcript = aiClient.transcribe(new FileSystemResource(payload));
			var tmp = File.createTempFile("transcript-" + id, ".txt");
			try (var out = new FileWriter(tmp)) {
				FileCopyUtils.copy(transcript, out);
			}
			return tmp;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
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
