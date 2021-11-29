package de.foellix.aql.jicer;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.FileHelper;
import de.foellix.aql.helper.Helper;

public class ParameterParser {
	private static boolean titleShown = false;

	public static Parameters parseArguments(String[] args) {
		return parseArguments(args, true);
	}

	public static Parameters parseArguments(String[] args, boolean exitOnError) {
		// Information
		if (!titleShown) {
			final String authorStr1 = "Author: " + Properties.info().AUTHOR;
			final String authorStr2 = "(" + Properties.info().AUTHOR_EMAIL + ")";
			final String centerspace1 = "               "
					.substring(15 - Math.max(0, ((int) Math.floor((27 - authorStr1.length()) / 2f))));
			final String centerspace2 = "               "
					.substring(15 - Math.max(0, ((int) Math.floor((27 - authorStr2.length()) / 2f))));

			Log.msg(ansi().bold().fg(GREEN).a("       _ _               \r\n" + "      | |_| ").reset()
					.a("v. " + Properties.info().VERSION).bold().fg(GREEN)
					.a("\r\n" + "      | |_  ___ ___ _ __ \r\n" + "   _  | | |/ __/ _ \\ '__|\r\n"
							+ "  | |_| | | (__| __/ |   \r\n" + "  \\_____|_|____\\___|_|   \r\n" + "\r\n"
							+ centerspace1)
					.reset().a(authorStr1 + "\r\n" + centerspace2 + authorStr2 + "\r\n"), Log.NORMAL);
			titleShown = true;
		}

		// Check JVM launch parameters
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		int xss = -1;
		int xmx = -1;
		for (final String arg : runtimeMXBean.getInputArguments()) {
			if (arg.toLowerCase().contains("-Xmx".toLowerCase())) {
				xmx = toMB(arg.toLowerCase().replace("-Xmx".toLowerCase(), ""));
				Parameters.getInstance().setMaxMemory(xmx);
			} else if (arg.toLowerCase().contains("-Xss".toLowerCase())) {
				xss = toMB(arg.toLowerCase().replace("-Xss".toLowerCase(), ""));
				Parameters.getInstance().setStackSize(xss);
			}
			if (xss > 0 && xmx > 0) {
				break;
			}
		}
		if (xss < 2) {
			Log.warning(
					"The JVM's stack size might be to small. Please provide around 2 MB (Recommended: 4 MB). To do so, use the JVM launch parameter \"-Xss\" (e.g. -Xss4m)");
		}
		if (xmx < 8000) {
			Log.warning(
					"The JVM's heap size (maximum memory usage) might be to small. Please provide around 8 GB (Recommended: 96 GB). To do so, use the JVM launch parameter \"-Xmx\" (e.g. -Xmx96G)");
		}

		// Read Parameters
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-gui")) {
					continue;
				} else if (args[i].equals("-simpleinput") || args[i].equals("-simple") || args[i].equals("-si")) {
					Parameters.getInstance().setSimpleInput(true);
				} else if (args[i].equals("-dg") || args[i].equals("-draw") || args[i].equals("-drawGraphs")) {
					Parameters.getInstance().setDrawGraphs(true);
				} else if (args[i].equals("-eol") || args[i].equals("-excludeOrdinaryLibraries")) {
					Parameters.getInstance().setIncludeOrdinaryLibraryPackages(false);
				} else if (args[i].equals("-sol") || args[i].equals("-sliceOrdinaryLibraries")) {
					Parameters.getInstance().setIncludeOrdinaryLibraryPackages(true);
					Parameters.getInstance().setSliceOrdinaryLibraryPackages(true);
				} else if (args[i].equals("-s") || args[i].equals("-sign")) {
					if (!Parameters.getInstance().isSign()) {
						Parameters.getInstance().setSign(true);
						Parameters.getInstance().setSteps(Parameters.getInstance().getSteps() + 1);
					}
				} else if (args[i].equals("-i") || args[i].equals("-in") || args[i].equals("-incomplete")) {
					Parameters.getInstance().setIncomplete(true);
				} else if (args[i].equals("-ra") || args[i].equals("-run") || args[i].equals("-runnable")) {
					if (!Parameters.getInstance().isSign()) {
						Parameters.getInstance().setSign(true);
						Parameters.getInstance().setSteps(Parameters.getInstance().getSteps() + 1);
					}
					Parameters.getInstance().setRunnable(true);
				} else if (args[i].equals("-nff")) {
					Parameters.getInstance().setFieldFiltering(false);
				} else if (args[i].equals("-ncsr")) {
					Parameters.getInstance().setContextSensitiveRefinement(false);
				} else if (args[i].equals("-sts")) {
					Parameters.getInstance().setStrictThreadSensitivity(true);
				} else if (args[i].equals("-os") || args[i].equals("-overapproximateSummaries")) {
					Parameters.getInstance().setOverapproximateStubDroid(true);
				} else if (args[i].equals("-ns") || args[i].equals("-nostats") || args[i].equals("-nostatistics")) {
					Parameters.getInstance().setRecordStatistics(false);
				} else {
					if (args[i].equals("-from") || args[i].equals("-to")) {
						if (args[i].equals("-from")) {
							Parameters.getInstance().setFrom(parseReferenceArgument(args[i + 1], true));
						} else {
							Parameters.getInstance().setTo(parseReferenceArgument(args[i + 1], false));
						}
					} else if (args[i].equals("-o") || args[i].equals("-out") || args[i].equals("-output")) {
						Parameters.getInstance()
								.setOutputFile(new File(CLIHelper.removeQuotesFromFileString(args[i + 1])));
					} else if (args[i].equals("-ie") || args[i].equals("-inputEdges")) {
						Parameters.getInstance()
								.setInputEdges(new File(CLIHelper.removeQuotesFromFileString(args[i + 1])));
					} else if (args[i].equals("-k") || args[i].equals("-limit") || args[i].equals("-klimit")
							|| args[i].equals("-k-limit")) {
						Parameters.getInstance().setkLimit(Integer.valueOf(args[i + 1]).intValue());
					} else if (args[i].equals("-f") || args[i].equals("-format")) {
						Parameters.getInstance().setOutputFormat(parseOutputFormat(args[i + 1]));
						if (Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_NONE) {
							Parameters.getInstance().setSteps(Parameters.getInstance().getSteps() - 2);
						}
					} else if (args[i].equals("-d") || args[i].equals("-debug")) {
						CLIHelper.evaluateLogLevel(args[i + 1]);
					} else if (args[i].equals("-mode") || args[i].equals("-m")) {
						Parameters.getInstance().setMode(args[i + 1]);
						if (Parameters.getInstance().getMode().equalsIgnoreCase(Parameters.MODE_SLICE_OUT)) {
							Parameters.getInstance().setMode(Parameters.MODE_SLICE_OUT);
						} else if (Parameters.getInstance().getMode().equalsIgnoreCase(Parameters.MODE_SHOW_SLICE)) {
							Parameters.getInstance().setMode(Parameters.MODE_SHOW_SLICE);
						} else {
							Parameters.getInstance().setMode(Parameters.MODE_SLICE);
						}
					} else {
						Log.error("Unknown argument: " + args[i]);
						System.exit(-1);
					}
					i++;
				}
			}
		}

		validateArguments(exitOnError);

		return Parameters.getInstance();
	}

	private static void validateArguments(boolean exitOnError) {
		if (Parameters.getInstance().getFrom() != null && Parameters.getInstance().getTo() != null) {
			Parameters.getInstance().setInputApkFile(new File(Parameters.getInstance().getFrom().getApp().getFile()));
			if (!EqualsHelper.equals(Parameters.getInstance().getFrom().getApp(),
					Parameters.getInstance().getTo().getApp())) {
				Log.warning("Cannot slice form one app (" + Parameters.getInstance().getFrom().getApp().getFile()
						+ ") to another (" + Parameters.getInstance().getTo().getApp().getFile()
						+ "). Ignoring \"-to\" parameter!");
				Parameters.getInstance().setTo(null);
			}
		} else if (Parameters.getInstance().getFrom() != null) {
			Parameters.getInstance().setInputApkFile(new File(Parameters.getInstance().getFrom().getApp().getFile()));
		} else if (Parameters.getInstance().getTo() != null) {
			Parameters.getInstance().setInputApkFile(new File(Parameters.getInstance().getTo().getApp().getFile()));
		} else {
			if (exitOnError) {
				Log.error(
						"No (valid) slicing input given. There are two options, which can be used at the same time, to do so (at least statement and app has to be referenced):\n\t-from <AnalysisTarget>\n\tOR\n\t-to <AnalysisTarget>\n(For more information about AQL-Query analysis' targets, visit: https://github.com/FoelliX/AQL-System/wiki/Questions#user-content-analysis-target)");
				System.exit(-1);
			}
		}

		if (Parameters.TEMP_OUTPUT_DIR.exists()) {
			if (!FileHelper.deleteDir(Parameters.TEMP_OUTPUT_DIR)) {
				if (exitOnError) {
					Log.error("Temorary directory (" + Parameters.TEMP_OUTPUT_DIR.getAbsolutePath()
							+ ") already exists and could not be deleted.");
					System.exit(-1);
				}
			}
		}
		Parameters.TEMP_OUTPUT_DIR.mkdirs();
	}

	private static int toMB(String input) {
		final int value = Integer.valueOf(input.substring(0, input.length() - 1)).intValue();
		if (input.endsWith("g")) {
			return value * 1000;
		} else if (input.endsWith("m")) {
			return value;
		} else if (input.endsWith("k")) {
			return (value / 1000);
		} else {
			return (value / 1000000);
		}
	}

	private static Reference parseReferenceArgument(String aqlQueryReference, boolean from) {
		Log.msg("Input Slicing criterion (" + (from ? "From" : "To") + "-Criterion): " + aqlQueryReference, Log.NORMAL);
		final String unVars = Helper.reportMissingVariables(aqlQueryReference);
		if (unVars == null) {
			final Reference ref = Helper.createReference(aqlQueryReference);
			if (ref.getApp() != null && ref.getApp().getFile() != null) {
				if (ref.getApp().getFile().endsWith(".class")) {
					Parameters.getInstance().setAndroidMode(false);
					Parameters.getInstance().setOutputFormat(Parameters.OUTPUT_FORMAT_CLASS);
				}
			}
			return ref;
		} else {
			Log.warning((from ? "From" : "To") + "-Criterion contains at least one unresolved Variable (" + unVars
					+ "). Ignoring this target!");
			return null;
		}
	}

	private static String parseOutputFormat(String format) {
		if (format.equalsIgnoreCase(Parameters.OUTPUT_FORMAT_NONE)) {
			return Parameters.OUTPUT_FORMAT_NONE;
		} else if (format.equalsIgnoreCase(Parameters.OUTPUT_FORMAT_JIMPLE)) {
			return Parameters.OUTPUT_FORMAT_JIMPLE;
		} else {
			if (Parameters.getInstance().isAndroidMode()) {
				return Parameters.OUTPUT_FORMAT_APK;
			} else {
				return Parameters.OUTPUT_FORMAT_CLASS;
			}
		}
	}
}