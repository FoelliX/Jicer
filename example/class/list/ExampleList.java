import java.util.ArrayList;
import java.util.List;

public class ExampleList {
	public static void main(String[] args) {
		final ExampleList ex = new ExampleList();// in
		ex.example();// in
	}

	public void example() {
		final List<String> list1 = new ArrayList<>();// in
		list1.add("secret");// in
		logNormal(list1.toString());// in
	}

	private void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	private void logError(String msg) {
		System.err.println(msg);// out
	}
}