public class ExampleException1 {
	private String s;

	public static void main(String[] args) {
		ExampleException1 ex = new ExampleException1();// in
		ex.example();// in
	}

	private void example() {
		try {// wdc
			s = "secret";// in
			throw new NullPointerException();// wdc
		} catch (Exception e) {// wdc
			logNormal(s);// in
		}// wdc
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}