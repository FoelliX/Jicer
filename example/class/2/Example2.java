public class Example2 {

  public static void main(String[] args) {
    int x = foo1(1);
    int y = bar(2);
    int z = foo2(2);
    x = 2;
    //x = y+x;
    z = x+y;
    baz(x + y);
  }

  public static void baz(int z) {
    System.out.println(z);
  }

  static int foo1(int x) {
    return x + 2;
  }
  
  static int foo2(int x) {
	    return x + 4;
  }

  static int bar(int x) {
    return x + 3;
  }
}