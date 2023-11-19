# Youtube Assistant Client

Provides a set of tools to make developing and promoting videos on Youtube easier.

The first interface is `YaAiClient`, which provides integrations with speech-to-text (the better to derive a video's
transcript), generative chat (the better to ask natural language questions about a video's transcript), and image
generation (the better to design a video's thumbnail)

```java
public interface YaAiClient {

    String transcribe(Resource audio);

    String chat(String prompt);

    Resource render(String prompt, ImageSize imageSize);

}
```

The transcription support defers to an
instance [of the Whisper-powered transcription module, deployable as a service here](https://github.com/youtube-atoms/whisper-transcription-service).
Be sure to run this first. In order to run the Whisper Transcription Service, you'll need to furnish S3 and RabbitMQ
credentials to both the consumer fo this library _and_ the Whisper Transcription Service.

