package ya;

import org.springframework.core.io.Resource;

/**
 * low level access to AI primitives
 */
public interface AiClient {

	String transcribe(Resource audio);

	String chat(String prompt);

	Resource render(String prompt, ImageSize imageSize);

}
