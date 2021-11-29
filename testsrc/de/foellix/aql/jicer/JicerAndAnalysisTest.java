package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.system.AQLSystem;
import de.foellix.aql.system.BackupAndReset;
import de.foellix.aql.system.Options;

@Tag("longLasting")
@Tag("systemIsSetup")
public class JicerAndAnalysisTest {
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
		// Simple
		final String apk = "debug.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('startActivity')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test02() {
		// LifeCycle
		final String apk = "debugSF.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test03() {
		// LifeCycle
		final String apk = "debugSF.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test04() {
		// Static Field
		final String apk = "debugLC.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test05() {
		// Call Bcks
		final String apk = "debugCB.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test06() {
		// Exceptions
		final String apk = "debugEX.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test07() {
		// Exceptions
		final String apk = "debugEX2.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test08() {
		// Context-Sensitivity Forward
		final String apk = "debugCS.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test09() {
		// Context-Sensitivity Backward
		final String apk = "debugCS.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"", false,
				false, false);
	}

	@Test
	public void test10() {
		// Context-Sensitivity
		final String apk = "debugCS.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false, false, false);
	}

	@Test
	public void test11() {
		// Context-Sensitivity Forward (2 returns)
		final String apk = "debugCS2.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk + "')\"",
				false);
	}

	@Test
	public void test12() {
		// Context-Sensitivity Forward (2 interleaved methods)
		final String apk = "debugCS3.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('sendTextMessage')->App('example/" + apk + "')\"",
				false, false, false);
	}

	@Test
	public void test13() {
		// Jicer running example
		final String apk = "debugJicerRunEx.apk";
		test(new File("example", apk),
				"-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('example/" + apk
						+ "')\" -to \"Statement('startActivity')->App('example/" + apk + "')\"",
				false);
	}

	// @Test
	public void testAndroidStudioOutput() {
		final File apk = new File(
				"D:\\custom\\workspaceAS\\JicerRunningExample\\app\\build\\outputs\\apk\\debug\\app-debug.apk");
		test(apk, "-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('" + apk.getAbsolutePath()
				+ "')\" -to \"Statement('startActivity')->App('" + apk.getAbsolutePath() + "')\"", false);

		// test(apk, "-d " + debug + " -m slice -si -from \"Statement('getSimSerialNumber')->App('"
		// + apk.getAbsolutePath() + "')\"");

		// test(apk, "-d " + debug + " -m slice -si -to \"Statement('startActivity')->App('" + apk.getAbsolutePath()
		// + "')\"");
	}

	protected static void test(File apkFile, String runCmd, boolean checkFlowPrecisely) {
		test(apkFile, runCmd, checkFlowPrecisely, false, true, null);
	}

	protected static void test(File apkFile, String runCmd, boolean checkFlowPrecisely, boolean featuresTaintBench) {
		test(apkFile, runCmd, checkFlowPrecisely, featuresTaintBench, true, null);
	}

	protected static void test(File apkFile, String runCmd, boolean checkFlowPrecisely, boolean featuresTaintBench,
			boolean result) {
		test(apkFile, runCmd, checkFlowPrecisely, featuresTaintBench, result, null);
	}

	protected static void test(File apkFile, String runCmd, boolean checkFlowPrecisely, boolean featuresTaintBench,
			boolean result, String uses) {
		Log.msg("Jicer-Launch-Command: " + runCmd, Log.NORMAL);

		boolean noException = true;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			new Jicer(args).jice();
		} catch (final Exception e) {
			noException = false;
		}

		if (doAnalysis && noException) {
			try {
				// Reset AQL-System
				BackupAndReset.reset();
				final AQLSystem aqlSystem = new AQLSystem(new Options().setResetOutputDirectories(false));

				// Get sliced result
				Object sliced;
				try {
					sliced = aqlSystem.queryAndWait("Flows IN App('" + apkFile.getName() + "') "
							+ (featuresTaintBench ? " FEATURING 'TaintBench' " : "")
							+ (uses == null ? "" : "USES '" + uses + "' ") + "?").iterator().next();
				} catch (final NoSuchElementException | NullPointerException e) {
					sliced = null;
				}

				// Get original result
				Object original;
				try {
					original = aqlSystem.queryAndWait("Flows IN App('" + apkFile.getAbsolutePath() + "') "
							+ (featuresTaintBench ? " FEATURING 'TaintBench' " : "")
							+ (uses == null ? "" : "USES '" + uses + "' ") + "?").iterator().next();
				} catch (final NoSuchElementException | NullPointerException e) {
					original = null;
				}

				if (checkFlowPrecisely) {
					// Precise flow check
					final Answer toCheck = toAnswer(runCmd);
					boolean slicedCheck = contains((Answer) sliced, toCheck, false);
					boolean originalCheck = contains((Answer) original, toCheck, false);
					boolean howeverCheck = contains((Answer) original, (Answer) sliced, false);
					Log.msg("Precise-Flow-Check (NOT considering Linenumbers): " + slicedCheck + " (sliced), "
							+ originalCheck + " (original)"
							+ (howeverCheck
									? "\n(However, the original-answer seems to contained the \"sliced\"-answer.)"
									: ""),
							Log.NORMAL);
					slicedCheck = contains((Answer) sliced, toCheck, true);
					originalCheck = contains((Answer) original, toCheck, true);
					howeverCheck = contains((Answer) original, (Answer) sliced, true);
					Log.msg("Precise-Flow-Check (considering Linenumbers): " + slicedCheck + " (sliced), "
							+ originalCheck + " (original)"
							+ (howeverCheck
									? "\n(However, the original-answer seems to contained the \"sliced\"-answer.)"
									: ""),
							Log.NORMAL);
					assertEquals(result, slicedCheck);
					assertEquals(result, originalCheck);
				} else {
					// Simple contains check
					boolean containsCheck = contains((Answer) original, (Answer) sliced, false);
					Log.msg("Contains-Check (NOT considering Linenumbers): " + (result == containsCheck), Log.NORMAL);
					containsCheck = contains((Answer) original, (Answer) sliced, true);
					Log.msg("Contains-Check (considering Linenumbers): " + (result == containsCheck), Log.NORMAL);
					assertEquals(result, containsCheck);
				}
			} catch (final Exception e) {
				noException = false;
			}
		}

		assertTrue(noException);
	}

	private static Answer toAnswer(String runCmd) {
		// Read input
		String fromStr = runCmd.substring(runCmd.indexOf("-from \"") + 7);
		fromStr = fromStr.substring(0, fromStr.indexOf("\""));
		fromStr = CLIHelper.replaceNeedlesWithQuotes(fromStr);
		String toStr = runCmd.substring(runCmd.indexOf("-to \"") + 5);
		toStr = toStr.substring(0, toStr.indexOf("\""));
		toStr = CLIHelper.replaceNeedlesWithQuotes(toStr);

		// Create references
		final Reference from = Helper.createReference(fromStr);
		from.setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_FROM);
		final Reference to = Helper.createReference(toStr);
		to.setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_TO);

		// Create AQL-Answer
		final Flow flow = new Flow();
		flow.getReference().add(from);
		flow.getReference().add(to);
		final Flows flows = new Flows();
		flows.getFlow().add(flow);
		final Answer answer = new Answer();
		answer.setFlows(flows);

		return answer;
	}

	/**
	 * returns if "answer" contains one element of "contains"
	 */
	protected static boolean contains(final Answer answer, final Answer contains, boolean considerLinenumbers) {
		if (answer == null && contains != null) {
			return false;
		} else if (answer != null && contains == null) {
			return false;
		}

		final EqualsOptions options = new EqualsOptions();
		options.setOption(EqualsOptions.IGNORE_APP, true);
		options.setOption(EqualsOptions.CONSIDER_LINENUMBER, considerLinenumbers);

		if (contains.getFlows() != null && contains.getFlows().getFlow() != null) {
			if (answer.getFlows() != null && answer.getFlows().getFlow() != null) {
				if (answer.getFlows().getFlow().isEmpty() && contains.getFlows().getFlow().isEmpty()) {
					return true;
				} else {
					if (contains.getFlows().getFlow().isEmpty() && !answer.getFlows().getFlow().isEmpty()) {
						return false;
					}
					for (final Flow needle : contains.getFlows().getFlow()) {
						for (final Flow flow : answer.getFlows().getFlow()) {
							if (EqualsHelper.equals(needle, flow, options)) {
								return true;
							}
						}
					}
				}
			}
			return false;
		} else {
			return true;
		}
	}
}