public class ExampleException2 {
	private String s;

	public static void main(String[] args) {
		ExampleException2 ex = new ExampleException2();// in
		ex.example();// in
	}

	private void example() {
		try {// wdc
			throw new NullPointerException();// wdc
		} catch (Exception e) {// wdc
			s = "secret";// in
		}// wdc
		logNormal(s);// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}