package de.foellix.aql.jicer;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.system.BackupAndReset;

@Tag("systemIsSetup")
public class InputEdgesTest {
	protected static boolean doAnalysis;
	protected static String debug;

	@BeforeAll
	public static void before() {
		doAnalysis = true;
		// debug = "short";
		// debug = "normal";
		debug = "debug";
		// debug = "detailed";
		BackupAndReset.resetOutputDirectories();
	}

	@Test
	public void test01() {
		// Jicer running example (forward)
		final String apk = "debugJicerRunEx.apk";
		JicerAndAnalysisTest.test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -ie example/debugJicerRunExInputEdges.xml",
				false);
	}

	@Test
	public void test02() {
		// Jicer running example (backward)
		final String apk = "debugJicerRunEx.apk";
		JicerAndAnalysisTest.test(new File("example", apk),
				"-d " + debug + " -m slice -si -to \"Statement('wtf')->App('example/" + apk
						+ "')\" -ie example/debugJicerRunExInputEdges.xml",
				false);
	}

	@Test
	public void test03() {
		// Jicer running example (both)
		final String apk = "debugJicerRunEx.apk";
		JicerAndAnalysisTest.test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('wtf')->App('example/" + apk
						+ "')\" -ie example/debugJicerRunExInputEdges.xml",
				false);
	}
}