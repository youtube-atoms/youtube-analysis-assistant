package ya;

public enum ImageSize {

	SIZE_1024x1024("1024x1024"), SIZE_1024x1792("1024x1792"), SIZE_1792x1024("1792x1024");

	private final String value;

	ImageSize(String s) {
		this.value = s;
	}

	String value() {
		return this.value;
	}

}
