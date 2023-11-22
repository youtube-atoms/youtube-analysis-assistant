package ya;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.FileReader;

@Component
class DefaultYaClient implements YaClient {

    private final YaAiClient yac;

    private final Engine engine;

    DefaultYaClient(YaAiClient yac, Engine engine) {
        this.yac = yac;
        this.engine = engine;
    }


    @Override
    public Ya analyseVideo(String id, Resource video) {
        try {
            var audio = this.engine.videoToAudio(video.getFile(), id);
            return analyseAudio(id, new FileSystemResource(audio));
        }//
        catch (Throwable throwable) {
            throw new RuntimeException("oops!", throwable);
        }
    }

    @Override
    public Ya analyseAudio(String id, Resource audio) {
        try {
            var transcript = FileCopyUtils.copyToString(new FileReader(
                    this.engine.audioToTranscript(this.yac, audio.getFile(), id)));

            var image = this.yac.render("""
                    create a prompt for a youtube video of ratio 
                    16:9 for a story with the following transcript:
                                        
                    %s
                    """.formatted(transcript), ImageSize.SIZE_1024x1792);

            return new Ya(audio, null, image, transcript);
        } catch (Throwable throwable) {
            throw new RuntimeException("oops!", throwable);
        }
    }
}
