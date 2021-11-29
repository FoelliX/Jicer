public class Example {
	/*
	 * in  : Must be in the slice
	 * out : Must NOT be in the slice
	 * wdc : We do not care if it is in the slice or not  
	 */
	
	public static void main(String[] args) {
		Example ex = new Example(); // in
		ex.example(); // in
		
	}

	public void example() {
		final StringBuilder sb1 = new StringBuilder(); // in
		sb1.append("Test"); // in
		logError("Does not belong to slice!\n"); // out
		logNormal("Belongs to slice: " + sb1.toString()); // in
		logError("Does also not belong to slice!\n"); // out
		logNormal("Also belongs to slice when last: " + sb1.toString()); // wdc
	}
	
	private void logNormal(String msg) {
		System.out.println("msg"); // wdc
	}
	
	private void logError(String msg) {
		System.err.println("msg"); // in
	}
}