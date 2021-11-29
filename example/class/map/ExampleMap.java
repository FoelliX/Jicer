import java.util.HashMap;

public class ExampleMap {

	public static void main(String[] args) {
		ExampleMap ex = new ExampleMap();// in
		ex.example();// in
	}

	public void example() {
		HashMap<String, String> map = new HashMap<>();// in
		String key1 = "key1"; // in
		map.put(key1, "secret");// in
		String key2 = "key2"; // wdc
		map.put(key2, "no secret");// wdc
		logErr(map.get(key2));// out
		logNormal(map.get(key1));// in
	}

	public void logNormal(String msg) {
		System.out.println(msg);// wdc
	}

	public void logErr(String msg) {
		System.err.println(msg);// out
	}
}