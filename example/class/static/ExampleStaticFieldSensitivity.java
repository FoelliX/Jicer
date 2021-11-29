public class ExampleStaticFieldSensitivity {
	public static String s;

	public static void main(String[] args) {
		ExampleStaticFieldSensitivity ex = new ExampleStaticFieldSensitivity();// in
		ex.example();// in
	}

	public void example() {
		addSecret("secret");// in
		logNormal(s);// in
	}

	public static void addSecret(String secret) {
		s = secret;// in
	}

	public void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}
