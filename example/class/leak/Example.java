public class Example {
	private String source() {
		return "secret";// in
	}

	private void sink(String toBeLeaked) {
		System.out.println("LEAK: " + toBeLeaked);// in
	}

	public static void main(String[] args) {
		Example ex = new Example();// in
		ex.leak();// in
	}

	private void leak() {
		String variable = source();// in

		StringBuilder sb = new StringBuilder();// in
		sb.append("salt1-");// in
		sb.append(variable);// in
		sb.append("-salt2");// in

		sink(sb.toString());// in
	}
}