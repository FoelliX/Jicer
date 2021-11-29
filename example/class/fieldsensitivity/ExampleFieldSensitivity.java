public class ExampleFieldSensitivity {
	public String s;

	public static void main(String[] args) {
		ExampleFieldSensitivity ex = new ExampleFieldSensitivity();// in
		ex.example();// in
	}

	public void example() {
		addSecret("secret");// in
		logNormal(s);// in
	}

	public void addSecret(String secret) {
		s = secret;// in
	}

	public void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}
