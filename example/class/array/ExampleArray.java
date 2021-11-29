public class ExampleArray {
	public static void main(String[] args) {
		final ExampleArray ex = new ExampleArray();// in
		ex.example();// in
	}

	private void example() {
		final String[] list = new String[2];// in
		list[0] = "secret";// in
		list[1] = "no secret";// out
		logError(list[1]);// out
		logNormal(list[0]);// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	private void logError(String msg) {
		System.err.println(msg);// out
	}
}