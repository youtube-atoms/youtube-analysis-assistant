package ya;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.Resource;

import java.net.URL;

record Image(@JsonProperty("revised_prompt") String revisedPrompt, URL url) {
}
