package ya;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.io.File;
import java.time.Duration;

@ConfigurationProperties(prefix = "ya")
record YaProperties(@NonNull Whisper whisper, @NonNull Duration clientTimeout, @NonNull Application application) {

	record Application(@NonNull Audio audio, @NonNull Video video) {

		record Audio(File input, File output) {
		}

		record Video(File input, File output) {
		}

	}

	record Whisper(@NonNull Rabbitmq rabbitmq, @NonNull S3 s3) {
		record Rabbitmq(boolean initializeBroker, String requestsQueue) {
		}

		record S3(String region, String accessKey, String accessKeySecret, String audioBucket) {
		}
	}

}
