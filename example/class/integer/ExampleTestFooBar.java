public class ExampleTestFooBar {
	public static void main(String[] args) {
		ExampleTestFooBar ex = new ExampleTestFooBar();// in
		ex.example();// in
	}

	public void example() {
		int x = foo1(1);// wdc
		final int y = bar(2);// in
		int z = foo2(2);// wdc
		x = 2;// wdc
		z = x + y;// in
		logNormal(z);// in
	}

	public static void logNormal(int msg) {
		System.out.println(msg);// wdc
	}

	static int foo1(int x) {
		return x + 2;// wdc
	}

	static int foo2(int x) {
		return x + 4;// wdc
	}

	static int bar(int x) {
		return x + 3;// in
	}
}