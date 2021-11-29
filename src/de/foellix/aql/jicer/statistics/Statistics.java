package de.foellix.aql.jicer.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.io.Files;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.FileHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.jicer.Parameters;
import de.foellix.aql.jicer.dg.DependenceGraph;

public class Statistics {
	private static final File STATISTICS_FILE = new File("statistics.csv");
	private static final int MAX_LINES = 300;

	public static final String TIMER_BUILDING_PDGS = "PDG construction: ";
	public static final String TIMER_BUILDING_SDG = "Building SDG: ";
	public static final String TIMER_SLICING = "Slicing overall: ";
	public static final String TIMER_SLICING_SDG = "Slicing SDG: ";
	public static final String TIMER_SLICING_CFG = "Slicing CFG: ";
	public static final String TIMER_SLICING_TO = "Slicing to: ";
	public static final String TIMER_SLICING_FROM = "Slicing from: ";
	public static final String TIMER_SLICING_EXTRAS = "Slicing extras: ";
	public static final String TIMER_OUTPUT_WRITING = "Output writing: ";
	public static final String TIMER_SIGNING = "Signing: ";
	public static final String TIMER_REST = "Rest: ";
	public static final String TIMER_OVERALL = "Overall: ";

	public static final String COUNTER_SLICING = "Overall steps: ";
	public static final String COUNTER_SLICING_TO = "To target steps: ";
	public static final String COUNTER_SLICING_FROM = "From target steps: ";
	public static final String COUNTER_SLICING_EXTRAS = "Extras steps: ";
	public static final String COUNTER_SLICING_SIZE = "Overall size: ";
	public static final String COUNTER_SLICING_SIZE_TO = "To target size: ";
	public static final String COUNTER_SLICING_SIZE_FROM = "From target size: ";
	public static final String COUNTER_SLICING_SIZE_EXTRAS = "Extras size: ";
	public static final String COUNTER_PDGS = "PDGs: ";
	public static final String COUNTER_SDG_EDGES = "SDG Edges: ";
	public static final String COUNTER_CLASSES = "Classes: ";
	public static final String COUNTER_METHODS = "Methods: ";
	public static final String COUNTER_METHODS_SLICED = "Methods sliced: ";
	public static final String COUNTER_METHODS_STUMPED = "Methods stumped: ";
	public static final String COUNTER_STATEMENTS = "Statements: ";
	public static final String COUNTER_STATEMENTS_SLICED = "Statements sliced: ";

	public static String[] COUNTER_SDG_TYPED_EDGES;

	private boolean recordStatistics;
	private String identifier;
	private Map<String, Timer> timers;
	private Map<String, Counter> counters;
	private List<String> storedAllLines;
	private int storedLine;

	private static Statistics instance = new Statistics();

	private Statistics() {
		COUNTER_SDG_TYPED_EDGES = new String[14];
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL_ENTRY] = "Control-Entry: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL] = "Control: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_DATA] = "Data: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_IN] = "Parameter-In: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_OUT] = "Paramenter-Out: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALL] = "Call: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_SUMMARY] = "Summary: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_FIELD_DATA] = "Field-Data: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_STATIC_FIELD_DATA] = "Static-Field-Data: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK] = "Callback: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_DEFINITION] = "Callback-Definition: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_ALTERNATIVE] = "Callback-Alternative: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_EXCEPTION] = "Exception: ";
		COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_INPUT] = "Input: ";

		this.recordStatistics = true;
		this.identifier = null;
		this.timers = new HashMap<>();
		this.counters = new HashMap<>();
		this.storedAllLines = null;
		this.storedLine = -1;
	}

	public static void init(Reference from, Reference to, Parameters parameters) {
		instance.identifier = Log.date() + "," + parameters.getInputApkFile().getName() + ","
				+ Helper.toString(from).replace(",", "#COMMA#") + "," + Helper.toString(to).replace(",", "#COMMA#");
		instance.recordStatistics = parameters.isRecordStatistics();
	}

	public static Timer getTimer(String title) {
		if (!instance.timers.containsKey(title)) {
			instance.timers.put(title, new Timer(title));
		}
		return instance.timers.get(title);
	}

	public static Counter getCounter(String title) {
		if (!instance.counters.containsKey(title)) {
			instance.counters.put(title, new Counter());
		}
		return instance.counters.get(title);
	}

	public static String getStatistics() {
		return getStatistics("\n", true);
	}

	public static String getStatistics(String separator, boolean withTitles) {
		final StringBuilder sb = new StringBuilder();

		if (withTitles) {
			sb.append("\n*** Runtimes (hours:minutes:seconds.milliseconds) ***\n");
		}
		sb.append(
				(withTitles ? TIMER_BUILDING_PDGS : "") + getTimer(TIMER_BUILDING_PDGS).getTimeAsString() + separator);
		sb.append((withTitles ? TIMER_BUILDING_SDG : "") + getTimer(TIMER_BUILDING_SDG).getTimeAsString() + separator);
		sb.append((withTitles ? TIMER_SLICING : "") + getTimer(TIMER_SLICING).getTimeAsString() + separator);
		sb.append((withTitles ? "\t- " + TIMER_SLICING_SDG : "") + getTimer(TIMER_SLICING_SDG).getTimeAsString()
				+ separator);
		sb.append((withTitles ? "\t\t- " + TIMER_SLICING_TO : "") + getTimer(TIMER_SLICING_TO).getTimeAsString()
				+ separator);
		sb.append((withTitles ? "\t\t- " + TIMER_SLICING_FROM : "") + getTimer(TIMER_SLICING_FROM).getTimeAsString()
				+ separator);
		sb.append((withTitles ? "\t\t- " + TIMER_SLICING_EXTRAS : "") + getTimer(TIMER_SLICING_EXTRAS).getTimeAsString()
				+ separator);
		sb.append((withTitles ? "\t- " + TIMER_SLICING_CFG : "") + getTimer(TIMER_SLICING_CFG).getTimeAsString()
				+ separator);
		sb.append((withTitles ? TIMER_OUTPUT_WRITING : "") + getTimer(TIMER_OUTPUT_WRITING).getTimeAsString()
				+ separator);
		sb.append((withTitles ? TIMER_REST : "") + getTimer(TIMER_REST).getTimeAsString() + separator);
		sb.append((withTitles ? TIMER_OVERALL : "") + getTimer(TIMER_OVERALL).getTimeAsString() + separator);

		if (withTitles) {
			sb.append("\n*** SDG Slicing steps ***\n");
		}
		sb.append((withTitles ? COUNTER_SLICING : "") + getCounter(COUNTER_SLICING).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_TO : "") + getCounter(COUNTER_SLICING_TO).getCounter()
				+ separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_FROM : "") + getCounter(COUNTER_SLICING_FROM).getCounter()
				+ separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_EXTRAS : "") + getCounter(COUNTER_SLICING_EXTRAS).getCounter()
				+ separator);

		if (withTitles) {
			sb.append("\n*** SDG Slicing sizes ***\n");
		}
		sb.append((withTitles ? COUNTER_SLICING_SIZE : "") + getCounter(COUNTER_SLICING_SIZE).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_SIZE_TO : "")
				+ getCounter(COUNTER_SLICING_SIZE_TO).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_SIZE_FROM : "")
				+ getCounter(COUNTER_SLICING_SIZE_FROM).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SLICING_SIZE_EXTRAS : "")
				+ getCounter(COUNTER_SLICING_SIZE_EXTRAS).getCounter() + separator);

		if (withTitles) {
			sb.append("\n*** Graph Statistics ***\n");
		}
		sb.append((withTitles ? COUNTER_PDGS : "") + getCounter(COUNTER_PDGS).getCounter() + separator);
		sb.append((withTitles ? COUNTER_SDG_EDGES : "") + getCounter(COUNTER_SDG_EDGES).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL_ENTRY] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL_ENTRY]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_DATA] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_DATA]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_IN] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_IN]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_OUT] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_OUT]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALL] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALL]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_SUMMARY] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_SUMMARY]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_FIELD_DATA] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_FIELD_DATA]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_STATIC_FIELD_DATA] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_STATIC_FIELD_DATA]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_DEFINITION] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_DEFINITION]).getCounter()
				+ separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_ALTERNATIVE] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_ALTERNATIVE]).getCounter()
				+ separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_EXCEPTION] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_EXCEPTION]).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_INPUT] : "")
				+ getCounter(COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_INPUT]).getCounter() + separator);

		if (withTitles) {
			sb.append("\n*** Slicing Result ***\n");
		}
		sb.append((withTitles ? COUNTER_CLASSES : "") + getCounter(COUNTER_CLASSES).getCounter() + separator);
		sb.append((withTitles ? COUNTER_METHODS : "") + getCounter(COUNTER_METHODS).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_METHODS_SLICED : "") + getCounter(COUNTER_METHODS_SLICED).getCounter()
				+ separator);
		sb.append((withTitles ? "\t- " + COUNTER_METHODS_STUMPED : "")
				+ getCounter(COUNTER_METHODS_STUMPED).getCounter() + separator);
		sb.append((withTitles ? COUNTER_STATEMENTS : "") + getCounter(COUNTER_STATEMENTS).getCounter() + separator);
		sb.append((withTitles ? "\t- " + COUNTER_STATEMENTS_SLICED : "")
				+ getCounter(COUNTER_STATEMENTS_SLICED).getCounter());
		if (withTitles) {
			sb.append("\n");
		}

		return sb.toString();
	}

	private static void toFile() {
		// Updated content
		final StringBuilder sb = new StringBuilder();
		sb.append(instance.identifier).append(",").append(getStatistics(",", false)).append("\n");

		// Old content
		final List<String> allLines = getAllLines();
		if (allLines.size() >= (MAX_LINES + 2)) {
			final File backup = FileHelper.makeUnique(STATISTICS_FILE);
			try {
				Files.move(STATISTICS_FILE, backup);
			} catch (final IOException e) {
				Log.warning("Could not move/split statistics file (" + STATISTICS_FILE.getAbsolutePath() + ") to: "
						+ backup.getAbsolutePath() + Log.getExceptionAppendix(e));
			}
			allLines.clear();
		}

		// Write file
		try {
			final FileOutputStream fos = new FileOutputStream(STATISTICS_FILE);
			if (allLines.isEmpty()) {
				// New File
				fos.write(
						("General Info,,,,Runtimes (hours:minutes:seconds.milliseconds),,,,,,,,,,,SDG Slicing steps,,,,SDG Slicing sizes,,,,Graph Statistics,,,,,,,,,,,,,,Slicing Result,,,,,\nStarted,Apk,From,To,"
								+ TIMER_BUILDING_PDGS + "," + TIMER_BUILDING_SDG + "," + TIMER_SLICING + ","
								+ TIMER_SLICING_SDG + "," + TIMER_SLICING_TO + "," + TIMER_SLICING_FROM + ","
								+ TIMER_SLICING_EXTRAS + "," + TIMER_SLICING_CFG + "," + TIMER_OUTPUT_WRITING + ","
								+ TIMER_REST + "," + TIMER_OVERALL + "," + COUNTER_SLICING + "," + COUNTER_SLICING_TO
								+ "," + COUNTER_SLICING_FROM + "," + COUNTER_SLICING_EXTRAS + "," + COUNTER_SLICING_SIZE
								+ "," + COUNTER_SLICING_SIZE_TO + "," + COUNTER_SLICING_SIZE_FROM + ","
								+ COUNTER_SLICING_SIZE_EXTRAS + "," + COUNTER_PDGS + "," + COUNTER_SDG_EDGES + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL_ENTRY] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_DATA] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_IN] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_OUT] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALL] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_SUMMARY] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_FIELD_DATA] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_STATIC_FIELD_DATA] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_DEFINITION] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_ALTERNATIVE] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_EXCEPTION] + ","
								+ COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_INPUT] + "," + COUNTER_CLASSES + ","
								+ COUNTER_METHODS + "," + COUNTER_METHODS_SLICED + "," + COUNTER_METHODS_STUMPED + ","
								+ COUNTER_STATEMENTS + "," + COUNTER_STATEMENTS_SLICED + "\n").replace(": ", "")
										.getBytes());
				fos.write(sb.toString().getBytes());
			} else {
				// Update line in file
				final int lineNo = getLine(allLines, instance.identifier);
				if (lineNo == -1) {
					allLines.add(sb.toString());
				} else {
					allLines.set(lineNo, sb.toString());
				}
				for (final String line : allLines) {
					fos.write(line.getBytes());
				}
			}
			fos.close();
		} catch (final IOException e) {
			Log.warning("Could not write to statistics file: " + STATISTICS_FILE.getAbsolutePath()
					+ Log.getExceptionAppendix(e));
		}
	}

	private static int getLine(List<String> allLines, String identifier) {
		if (instance.storedLine > 0) {
			return instance.storedLine;
		}

		if (!allLines.isEmpty()) {
			int counter = 0;
			for (final String line : allLines) {
				if (line.startsWith(identifier)) {
					instance.storedLine = counter;
					return instance.storedLine;
				} else {
					counter++;
				}
			}
		}
		return -1;
	}

	private static List<String> getAllLines() {
		if (instance.storedAllLines != null) {
			return instance.storedAllLines;
		}

		final List<String> allLines = new LinkedList<>();
		if (STATISTICS_FILE.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(STATISTICS_FILE))) {
				String line;
				while ((line = br.readLine()) != null) {
					allLines.add(line + "\n");
				}
				instance.storedAllLines = allLines;
			} catch (final IOException e) {
				Log.warning("Could not read statistics file: " + STATISTICS_FILE.getAbsolutePath()
						+ Log.getExceptionAppendix(e));
			}
		}
		return allLines;
	}

	public static String getSlicingResult() {
		final String separator = ",\n";
		final String spacer = "\t";
		return "*** Absolute ***\nClasses:" + spacer + getCounter(COUNTER_CLASSES).getCounter() + separator + "Methods:"
				+ spacer + getCounter(COUNTER_METHODS).getCounter() + " - ("
				+ getCounter(COUNTER_METHODS_SLICED).getCounter() + " + "
				+ getCounter(COUNTER_METHODS_STUMPED).getCounter() + ") = "
				+ (getCounter(COUNTER_METHODS).getCounter() - (getCounter(COUNTER_METHODS_SLICED).getCounter()
						+ getCounter(COUNTER_METHODS_STUMPED).getCounter()))
				+ separator + "Statements:" + spacer + getCounter(COUNTER_STATEMENTS).getCounter() + " - "
				+ getCounter(COUNTER_STATEMENTS_SLICED).getCounter() + " = "
				+ (getCounter(COUNTER_STATEMENTS).getCounter() - getCounter(COUNTER_STATEMENTS_SLICED).getCounter())
				+ "\n\n*** Relative ***\nMethods:" + spacer + "100% - ("
				+ percent(getCounter(COUNTER_METHODS).getCounter(), getCounter(COUNTER_METHODS_SLICED).getCounter())
				+ "% + "
				+ percent(getCounter(COUNTER_METHODS).getCounter(), getCounter(COUNTER_METHODS_STUMPED).getCounter())
				+ "%) = "
				+ (100 - percent(getCounter(COUNTER_METHODS).getCounter(),
						(getCounter(COUNTER_METHODS_SLICED).getCounter()
								+ getCounter(COUNTER_METHODS_STUMPED).getCounter())))
				+ "%" + separator + "Statements:" + spacer + "100% - "
				+ percent(getCounter(COUNTER_STATEMENTS).getCounter(),
						getCounter(COUNTER_STATEMENTS_SLICED).getCounter())
				+ "% = " + (100 - percent(getCounter(COUNTER_STATEMENTS).getCounter(),
						getCounter(COUNTER_STATEMENTS_SLICED).getCounter()))
				+ "%";
	}

	private static int percent(float all, float subset) {
		return (int) ((subset / all) * 100f);
	}

	public static void reset() {
		instance.recordStatistics = true;
		instance.identifier = null;
		instance.timers.clear();
		instance.counters.clear();
		instance.storedAllLines = null;
		instance.storedLine = -1;
	}

	public static void update() {
		if (instance.recordStatistics) {
			Statistics.toFile();
		}
	}
}