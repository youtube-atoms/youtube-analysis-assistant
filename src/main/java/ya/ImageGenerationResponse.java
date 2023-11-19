package ya;

import java.util.List;

record ImageGenerationResponse(long created, List<Image> data) {
}
