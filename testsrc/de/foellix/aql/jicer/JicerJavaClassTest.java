package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.Log;
import de.foellix.aql.helper.Helper;

@Tag("longLasting")
public class JicerJavaClassTest {
	@BeforeAll
	public static void setup() {
		Log.setLogLevel(Log.VERBOSE);
	}

	@Test
	public void test01() {
		// Leak example
		final String location = "example/class/leak";
		test(new File("example/class/leak", "Example.class"),
				"-d normal -dg -m show -si -to \"Statement('println')->App('" + location + "/Example.class')\"");
	}

	@Test
	public void test02() {
		final String location = "example/class/1";
		test(new File("example/class/1", "Example.class"),
				"-d normal -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/Example.class')\"");
	}

	@Test
	public void test03() {
		final String location = "example/class/2";
		test(new File("example/class/2", "Example2.class"),
				"-d debug -dg -m slice -si -to \"Statement('println')->App('" + location + "/Example2.class')\"");
	}

	@Test
	public void testArray() {
		final String location = "example/class/array";
		test(new File("example/class/array", "ExampleArray.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/ExampleArray.class')\"");
	}

	@Test
	public void test05() {
		final String location = "example/class/integer";
		test(new File("example/class/integer", "ExampleTestFooBar.class"),
				"-d debug -dg -m slice -si -to \"Statement('println')->App('" + location
						+ "/ExampleTestFooBar.class')\"");
	}

	@Test
	public void testList() {
		final String location = "example/class/list";
		test(new File("example/class/list", "ExampleList.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/ExampleList.class')\"");
	}

	@Test
	public void testStringBuilder() {
		final String location = "example/class/stringbuilder";
		test(new File("example/class/stringbuilder", "ExampleStringBuilder.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleStringBuilder.class')\"");
	}

	@Test
	public void testStringBuilderReassign() {
		final String location = "example/class/stringbuilderreassign";
		test(new File("example/class/stringbuilderreassign", "ExampleStringBuilderReassign.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleStringBuilderReassign.class')\"");
	}

	@Test
	public void testStatic() {
		final String location = "example/class/static";
		test(new File("example/class/static", "ExampleStaticFieldSensitivity.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleStaticFieldSensitivity.class')\"");
	}

	@Test
	public void testFieldSensitivity() {
		final String location = "example/class/fieldsensitivity";
		test(new File("example/class/fieldsensitivity", "ExampleFieldSensitivity.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleFieldSensitivity.class')\"");
	}

	@Test
	public void testBranching() {
		final String location = "example/class/branching";
		test(new File("example/class/branching", "ExampleBranching.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleBranching.class')\"");
	}

	@Test
	public void test11() {
		final String location = "example/class/branching";
		test(new File("example/class/branching", "ExampleBranchingStatic.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleBranchingStatic.class')\"");
	}

	@Test
	public void testInnerClass() {
		final String location = "example/class/innerclass";
		test(new File("example/class/innerclass", "ExampleInnerClass.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleInnerClass.class')\"");
	}

	@Test
	public void testAlias() {
		final String location = "example/class/alias";
		test(new File("example/class/alias", "ExampleAlias.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/ExampleAlias.class')\"");
	}

	@Test
	public void testException1() {
		final String location = "example/class/exception";
		test(new File("example/class/exception", "ExampleException1.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleException1.class')\"");
	}

	@Test
	public void testException2() {
		final String location = "example/class/exception";
		test(new File("example/class/exception", "ExampleException2.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExampleException2.class')\"");
	}

	@Test
	public void testMap() {
		final String location = "example/class/map";
		test(new File("example/class/map", "ExampleMap.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/ExampleMap.class')\"");
	}

	@Test
	public void testStack() {
		final String location = "example/class/stack";
		test(new File("example/class/stack", "ExampleStack.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location + "/ExampleStack.class')\"");
	}

	@Test
	public void testPathSensitivity() {
		final String location = "example/class/pathsensitivity";
		test(new File("example/class/pathsensitivity", "ExamplePathSensitivity.class"),
				"-d debug -dg -m slice -si -to \"Statement('logNormal')->App('" + location
						+ "/ExamplePathSensitivity.class')\"");
	}

	private void test(File apkFile, String runCmd) {
		boolean noException = true;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			new Jicer(args).jice();
		} catch (final Exception e) {
			noException = false;
		}
		assertTrue(noException);
	}
}