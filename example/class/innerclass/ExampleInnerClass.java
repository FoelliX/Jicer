public class ExampleInnerClass {
	public static void main(String[] args) {
		ExampleInnerClass ex = new ExampleInnerClass();// in
		ex.example();// in
	}

	public void example() {
		StringObj s = new StringObj();// in
		s.value = "secret";// in
		logNormal(s.value);// in
	}

	public static void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	class StringObj {// in
		public String value;// in
	}// in
}