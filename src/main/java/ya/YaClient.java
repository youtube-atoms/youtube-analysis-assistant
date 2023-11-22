package ya;

import org.springframework.core.io.Resource;

public interface YaClient {

    record Ya( Resource audio, Resource video, Resource image,
               String transcript) {
    }

    Ya analyseVideo( String uid, Resource video);

    Ya analyseAudio( String uid, Resource audio);
}


