package ya;

import org.springframework.core.io.Resource;

/**
 * low level access to AI primitives
 */
public interface YaAiClient {

	String transcribe(Resource audio);

	String talk(String prompt);

	Resource render(String prompt, ImageSize imageSize);

}
