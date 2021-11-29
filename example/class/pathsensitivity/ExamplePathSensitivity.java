public class ExamplePathSensitivity {
	public static void main(String[] args) {
		ExamplePathSensitivity ex = new ExamplePathSensitivity();// in
		ex.example();// in
	}

	public void example() {
		String s = "secret1";// in
		if (1 == 2) {// out
			s = "secret2";// out
		}
		logNormal(s);// in
	}

	public static void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	public static void logErr(String msg) {
		System.err.println(msg);// out
	}
}