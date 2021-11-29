public class ExampleAlias {
	private String f;

	public static void main(String[] args) {
		ExampleAlias ex = new ExampleAlias();// in
		ex.example();// in
	}

	private void example() {
		ExampleAlias a = new ExampleAlias();// in
		ExampleAlias b = a;// in
		b.f = "secret";// in
		logNormal(a.f);// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}
}