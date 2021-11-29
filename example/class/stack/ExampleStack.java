import java.util.Stack;;

public class ExampleStack {

	public static void main(String[] args) {
		ExampleStack ex = new ExampleStack();// in
		ex.example();// in
	}

	public void example() {
		Stack<String> stack = new Stack<>();// in
		stack.push("secret");// in
		logErr(stack.isEmpty());// out
		logNormal(stack.pop());// in
	}

	public void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	public void logErr(boolean msg) {
		System.err.println(msg);// out
	}
}