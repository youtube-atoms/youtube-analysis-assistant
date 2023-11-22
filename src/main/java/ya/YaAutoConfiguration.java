package ya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.autoconfigure.openai.OpenAiProperties;
import org.springframework.ai.client.AiClient;
import org.springframework.amqp.core.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(YoutubeAnalysisProperties.class)
class YaAutoConfiguration {

	@Bean
	ApplicationRunner rabbitMqDestinationInitializationRunner(AmqpAdmin amqpAdmin,
			YoutubeAnalysisProperties properties) {
		return args -> {

			if (!properties.whisper().rabbitmq().initializeBroker())
				return;

			var key = properties.whisper().rabbitmq().requestsQueue();
			var requests = properties.whisper().rabbitmq().requestsQueue();
			var q = QueueBuilder.durable(requests).build();
			var exchange = ExchangeBuilder.directExchange(requests).durable(true).build();
			var binding = BindingBuilder.bind(q).to(exchange).with(key).noargs();
			amqpAdmin.declareQueue(q);
			amqpAdmin.declareExchange(exchange);
			amqpAdmin.declareBinding(binding);
		};
	}

	@Bean
	S3Client s3Client(YoutubeAnalysisProperties properties) {
		var api = properties.whisper();
		var key = api.s3().accessKey();
		var secret = api.s3().accessKeySecret();
		var creds = AwsBasicCredentials.create(key, secret);
		return S3Client.builder()
			.region(Region.of(api.s3().region()))
			.credentialsProvider(StaticCredentialsProvider.create(creds))
			.forcePathStyle(true)
			.build();
	}

	@Bean
	TranscriptionClient transcriptClient(S3Client s3, AmqpTemplate template, ObjectMapper objectMapper,
			YoutubeAnalysisProperties properties) {
		var api = properties.whisper();
		return new TranscriptionClient(objectMapper, s3, template, api.s3().audioBucket(),
				api.rabbitmq().requestsQueue());
	}

	@Bean
	DefaultAiClient defaultYaClient(RestTemplate restTemplate, AiClient aiClient, OpenAiProperties aiProperties,
			TranscriptionClient tc) {
		return new DefaultAiClient(restTemplate, aiClient, aiProperties.getApiKey(), tc);
	}

	@Bean
	JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return new JdkClientHttpRequestFactory();
	}

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder, YoutubeAnalysisProperties properties,
			ClientHttpRequestFactory jdkClientHttpRequestFactory) {
		return restTemplateBuilder.requestFactory(() -> jdkClientHttpRequestFactory)
			.setReadTimeout(properties.clientTimeout()) //
			.setConnectTimeout(properties.clientTimeout()) //
			.build();
	}

}
