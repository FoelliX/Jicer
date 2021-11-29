public class ExampleBranching {
	public static void main(String[] args) {
		ExampleBranching ex = new ExampleBranching();// in
		ex.example();// in
	}

	private void example() {
		String s = "secret1";// in
		if (1 == 1) {// wdc
			s = "secret2";// in
		}// wdc
		logNormal(s);// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}