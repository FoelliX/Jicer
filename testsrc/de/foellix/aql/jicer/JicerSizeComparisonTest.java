package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.Log;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.jicer.statistics.Statistics;

@Tag("systemIsSetup")
public class JicerSizeComparisonTest {
	private static int lastResult = -1;

	private static StringBuilder sb = new StringBuilder("\n\n--- Test result ---\n");

	@AfterAll
	public static void after() {
		Log.msg(sb.toString(), Log.NORMAL);
	}

	@Test
	public void test01() {
		test("-m sliceout -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test02() {
		test("-m slice -run -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test03() {
		test("-m slice -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test04() {
		test("-m slice -incomplete -si -from \"Statement('getSimSerialNumber')->App('example/debug.apk')\" -to \"Statement('startActivity')->App('example/debug.apk')\"");
	}

	@Test
	public void test05() {
		lastResult = -1; // Different app then in test 04
		test("-m sliceout -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_name#QUOTE#, $r2)', 43)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test06() {
		test("-m slice -run -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_name#QUOTE#, $r2)', 43)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test07() {
		test("-m slice -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_name#QUOTE#, $r2)', 43)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('example/TaintBench/save_me.apk')\"");
	}

	@Test
	public void test08() {
		test("-m slice -incomplete -from \"Statement('$r6 = virtualinvoke $r5.<android.widget.EditText: android.text.Editable getText()>()', 134)->Method('<com.savemebeta.Analyse$1: void onClick(android.view.View)>')->Class('com.savemebeta.Analyse$1')->App('example/TaintBench/save_me.apk')\" -to \"Statement('virtualinvoke $r5.<android.content.ContentValues: void put(java.lang.String,java.lang.String)>(#QUOTE#user_name#QUOTE#, $r2)', 43)->Method('<com.savemebeta.DatabaseOperationslogin: void putInformation(com.savemebeta.DatabaseOperationslogin,java.lang.String,java.lang.String,java.lang.String)>')->Class('com.savemebeta.DatabaseOperationslogin')->App('example/TaintBench/save_me.apk')\"");
	}

	private void test(String runCmd) {
		boolean noException = true;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			new Jicer(args).jice();
			final int result = Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED).getCounter();
			final String line = "Before: " + lastResult + "; After: " + result + "\n";
			Log.msg("\nTest-Result --> " + line, Log.NORMAL);
			sb.append(line);
			assertTrue(result >= lastResult);
			lastResult = result;
		} catch (final Exception e) {
			noException = false;
		}

		assertTrue(noException);
	}
}
