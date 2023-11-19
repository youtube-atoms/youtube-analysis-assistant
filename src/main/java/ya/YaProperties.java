package ya;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ya")
record YaProperties(Whisper whisper, Duration clientTimeout) {

	record Whisper(Rabbitmq rabbitmq, S3 s3) {
		record Rabbitmq(boolean initializeBroker, String requestsQueue) {
		}

		record S3(String region, String accessKey, String accessKeySecret, String audioBucket) {
		}

	}

}
