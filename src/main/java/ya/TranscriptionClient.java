package ya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

class TranscriptionClient {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectMapper objectMapper;

	private final S3Client s3;

	private final AmqpTemplate template;

	private final String audioBucket, requestsQueue;

	TranscriptionClient(ObjectMapper objectMapper, S3Client s3, AmqpTemplate template, String audioBucket,
			String requestsQueue) {
		this.objectMapper = objectMapper;
		this.s3 = s3;
		this.template = template;
		this.audioBucket = audioBucket;
		this.requestsQueue = requestsQueue;
	}

	String transcribe(String uid, Resource resource) {
		// s3
		var ext = resource.getFilename().substring(1 + resource.getFilename().lastIndexOf('.'));
		var path = uid + "." + ext;
		this.upload(this.audioBucket, path, resource);
		var json = this.json(Map.of("path", "s3://" + this.audioBucket + "/" + path));
		var build = MessageBuilder.withBody(Objects.requireNonNull(json).getBytes(Charset.defaultCharset())).build();
		var replyMessage = template.sendAndReceive(this.requestsQueue, build);
		var replyBytes = replyMessage.getBody();
		return new String(replyBytes);
	}

	private String json(Object o) {
		try {
			return this.objectMapper.writeValueAsString(o);
		}
		catch (Throwable throwable) {
			//
		}
		return null;
	}

	private void upload(String bucketName, String objectKey, Resource resource) {
		log.info("writing to bucket named [" + bucketName + "] with object key [" + objectKey + "]");
		try (var inputStream = resource.getInputStream()) {
			var putObject = PutObjectRequest.builder().bucket(bucketName).key(objectKey).metadata(Map.of()).build();
			s3.putObject(putObject, RequestBody.fromInputStream(inputStream, resource.contentLength()));
			log.info("wrote to bucket named [" + bucketName + "] with object key [" + objectKey + "]");
		}
		catch (Throwable e) {
			throw new RuntimeException(
					"couldn't upload the resource [" + objectKey + "] to bucket name [" + bucketName + "]", e);
		}
	}

}
