public class ExampleStringBuilderReassign {
	public static void main(String[] args) {
		final ExampleStringBuilderReassign ex = new ExampleStringBuilderReassign();// in
		ex.example();// in
	}

	public void example() {
		StringBuilder sb = new StringBuilder();// in
		sb = sb.append("secret");// in
		logError(sb.toString());// out
		logNormal(sb.toString());// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	private void logError(String msg) {
		System.err.println(msg);// out
	}
}