package de.foellix.aql.jicer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.Statement;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.ManifestHelper;
import de.foellix.aql.helper.ManifestInfo;
import de.foellix.aql.helper.tools.APKSigner;
import de.foellix.aql.jicer.cfgslicer.CFGSlicer;
import de.foellix.aql.jicer.config.Config;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.Edge;
import de.foellix.aql.jicer.dg.GraphDrawer;
import de.foellix.aql.jicer.dg.InputEdgesHandler;
import de.foellix.aql.jicer.dg.PDGConnector;
import de.foellix.aql.jicer.dg.PDGHelper;
import de.foellix.aql.jicer.lifecycle.LifeCycleHandler;
import de.foellix.aql.jicer.sdgslicer.FieldCollector;
import de.foellix.aql.jicer.sdgslicer.SDGBackwardSlicer;
import de.foellix.aql.jicer.sdgslicer.SDGForwardSlicer;
import de.foellix.aql.jicer.soot.PDGTransformer;
import de.foellix.aql.jicer.soot.ReachingDefinition;
import de.foellix.aql.jicer.soot.SootHelper;
import de.foellix.aql.jicer.soot.ValueOrField;
import de.foellix.aql.jicer.statistics.Statistics;
import net.dongliu.apk.parser.ApkFile;
import soot.Body;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.ThisRef;
import soot.options.Options;

public class Jicer {
	private int stepCounter;
	private LifeCycleHandler lifeCycleHandler;

	private Unit targetTo = null;
	private Unit targetFrom = null;

	private boolean noDummyMain;
	private boolean fromToSliceIsEmpty;

	public Jicer(String[] args) {
		this();
		ParameterParser.parseArguments(CLIHelper.replaceNeedlesWithQuotes(args));
	}

	public Jicer() {
	}

	public void reset() {
		// Steps and targets
		this.stepCounter = 0;
		this.targetTo = null;
		this.targetFrom = null;

		// Alert flags
		this.fromToSliceIsEmpty = true;
		this.noDummyMain = false;

		G.reset();
		G.v().resetSpark();
		Data.getInstance().reset();
		Statistics.reset();
		PDGHelper.reset();
		SootHelper.reset();
		Log.reset();

		// Garbage collect
		System.gc();
	}

	public DependenceGraph jice() {
		// reinitialize
		reset();
		DependenceGraph sdgSliced = null;

		// Start overall timer
		Statistics.getTimer(Statistics.TIMER_OVERALL).start();

		// Initialize statistics
		Statistics.init(Parameters.getInstance().getFrom(), Parameters.getInstance().getTo(), Parameters.getInstance());

		// Setup Soot
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Setting up Soot",
				Log.NORMAL);
		if (Log.getLogLevel() >= Log.DEBUG) {
			Log.setSilence(false);
		} else {
			Log.setSilence(Log.SILENCE_LEVEL_MSG);
		}
		if (!setupSoot()) {
			System.exit(1);
		}

		// Evaluate input edges
		if (Parameters.getInstance().getInputEdges() != null) {
			InputEdgesHandler.evaluateInputEdges(Parameters.getInstance().getInputEdges());
		}

		// Prepare targets
		prepareTargets();

		// Setup PDG generation
		PackManager.v().getPack("jtp").add(new Transform("jtp.PDGTransformer", new PDGTransformer()));
		if (Parameters.getInstance().getFrom() != null) {
			SootHelper.getExceptionalIncludes().add(Parameters.getInstance().getFrom().getClassname());
		}
		if (Parameters.getInstance().getTo() != null) {
			SootHelper.getExceptionalIncludes().add(Parameters.getInstance().getTo().getClassname());
		}
		Log.setSilence(false);

		// Find slicing targets
		final boolean slicingTargetsFound = findSlicingTargets();
		if (slicingTargetsFound) {
			// Build PDGs (Run Soot)
			Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Constructing PDGs",
					Log.NORMAL);
			if (Log.getLogLevel() >= Log.DEBUG) {
				Log.setSilence(false);
			} else {
				Log.setSilence(Log.SILENCE_LEVEL_MSG);
			}
			Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).start();
			PackManager.v().runPacks();
			Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).stop();
			Statistics.update();
			Log.setSilence(false);

			// Handle Exceptions
			handleExceptions();

			// Build Dummy Main (FlowDroid)
			buildDummyMainPDG();

			// Add Callback edges
			addCallBackEdges();

			// Build SDG
			final DependenceGraph sdg = buildSDG();

			// Slice SDG
			sdgSliced = sliceSDG(sdg);

			// Slice CFG
			sliceCFG(sdgSliced, sdg);

			Statistics.getTimer(Statistics.TIMER_SLICING).stop();
			Statistics.update();

			// Write output
			Statistics.getTimer(Statistics.TIMER_OUTPUT_WRITING).start();
			if (Parameters.getInstance().getOutputFormat() != Parameters.OUTPUT_FORMAT_NONE) {
				writeOutput();
			}

			// Handle output
			if (Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_APK) {
				signAndMoveToOutput();
			} else if (Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_CLASS) {
				convertToClass();
			} else if (Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_JIMPLE) {
				moveJimpleOutput();
			}
			Statistics.getTimer(Statistics.TIMER_OUTPUT_WRITING).stop();
		}

		// Stop overall timer (and compute rest timer)
		Statistics.getTimer(Statistics.TIMER_OVERALL).stop();
		Statistics.getTimer(Statistics.TIMER_REST)
				.setTime(Statistics.getTimer(Statistics.TIMER_OVERALL).getTime()
						- (Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).getTime()
								+ Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).getTime()
								+ Statistics.getTimer(Statistics.TIMER_OUTPUT_WRITING).getTime()
								+ Statistics.getTimer(Statistics.TIMER_SLICING).getTime()
								+ Statistics.getTimer(Statistics.TIMER_SIGNING).getTime()));

		if (slicingTargetsFound && Parameters.getInstance().isRecordStatistics() && Log.logIt(Log.DEBUG)) {
			Log.msg(Statistics.getStatistics(), Log.DEBUG);
		}
		Statistics.update();

		if (slicingTargetsFound) {
			final StringBuilder sb = new StringBuilder(
					"Slicing result for \"" + Parameters.getInstance().getInputApkFile().getAbsolutePath() + "\":\n"
							+ Statistics.getSlicingResult());
			// Report if backward-forward intersection is empty
			if (Parameters.getInstance().getTo() != null && Parameters.getInstance().getFrom() != null
					&& this.fromToSliceIsEmpty) {
				if (Parameters.getInstance().getMode() == Parameters.MODE_SLICE_OUT
						|| Parameters.getInstance().isIncomplete()) {
					sb.append("\n\nCaution: There was no intersection between forward and backward slice!");
				} else {
					sb.append("\n\nCaution: From-Criterion is not contained in backward slice!");
				}
			}
			// Report failed dummy main generation
			if (this.noDummyMain) {
				sb.append(
						"\n\nCaution: Slice might be incomplete since FlowDroid was not able to generate a dummy main!");
			}
			// Report failed reaching definition analysis
			if (!Data.getInstance().getFailedRDs().isEmpty()) {
				sb.append("\n\nCaution: Reaching Definition analysis was stopped after "
						+ Parameters.getInstance().getkLimit() + " steps for "
						+ Data.getInstance().getFailedRDs().size() + " "
						+ (Data.getInstance().getFailedRDs().size() > 1 ? "methods" : "method")
						+ " (To set a higher limit use launch parameter \"-k\" - default: \"-k "
						+ Parameters.DEFAULT_K_LIMIT_FOR_ANALYSIS
						+ "\")! Used flow-insensitive analysis to approximate.");
				if (Log.logIt(Log.DEBUG)) {
					sb.append(" This happened for the following methods:");
					for (final String signature : Data.getInstance().getFailedRDs()) {
						sb.append("\n\t- " + signature.substring(1, signature.length() - 1));
					}
				}
			}
			Log.msg(sb.toString(), Log.IMPORTANT);
		}

		return sdgSliced;
	}

	private boolean setupSoot() {
		if (Log.logIt(Log.DEBUG_DETAILED)) {
			Options.v().set_debug(true);
			Options.v().set_verbose(true);
		}
		if (Parameters.getInstance().isAndroidMode()) {
			Options.v().set_src_prec(Options.src_prec_apk);
		} else {
			Options.v().set_src_prec(Options.src_prec_class);
		}
		switch (Parameters.getInstance().getOutputFormat()) {
			case Parameters.OUTPUT_FORMAT_NONE:
				Options.v().set_output_format(Options.output_format_none);
				break;
			case Parameters.OUTPUT_FORMAT_JIMPLE:
				Options.v().set_output_format(Options.output_format_jimple);
				break;
			case Parameters.OUTPUT_FORMAT_CLASS:
				Options.v().set_output_format(Options.output_format_jimple);
				break;
			default:
				Options.v().set_output_format(Options.output_format_dex);
				break;
		}
		Options.v().set_whole_program(true);
		if (Parameters.getInstance().isAndroidMode()) {
			Options.v().set_android_jars(Config.getInstance().platformsPath);
			Options.v().set_process_multiple_dex(true);
			Options.v().set_exclude(getExcludeList());
			Options.v().set_process_dir(
					Collections.singletonList(Parameters.getInstance().getInputApkFile().getAbsolutePath()));
		} else {
			Options.v().set_soot_classpath(getClasspath());
			Options.v().set_process_dir(Collections
					.singletonList(Parameters.getInstance().getInputApkFile().getParentFile().getAbsolutePath()));
		}
		Options.v().set_keep_line_number(true);
		Options.v().set_ignore_resolution_errors(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_no_writeout_body_releasing(true);
		Options.v().set_output_dir(Parameters.TEMP_OUTPUT_DIR.getAbsolutePath());

		try {
			Scene.v().loadNecessaryClasses();
		} catch (final Exception e) {
			Log.error("Soot could not load all necessary classes!" + Log.getExceptionAppendix(e));
			return false;
		}
		return true;
	}

	private String getClasspath() {
		final String javaHome;
		if (System.getenv("JAVA8_HOME") != null) {
			javaHome = System.getenv("JAVA8_HOME").replace("\\", "/");
		} else {
			javaHome = System.getenv("JAVA_HOME").replace("\\", "/");
		}
		final String classPathDelimiter = getClassPathDelimiter();
		final StringBuilder sootClasspath = new StringBuilder(
				Parameters.getInstance().getInputApkFile().getParentFile().getAbsolutePath().replace("\\", "/"));
		final File rtJar = new File(javaHome + "/jre/lib/rt.jar");
		if (rtJar.exists()) {
			sootClasspath.append(classPathDelimiter + rtJar.getAbsolutePath().replace("\\", "/"));
		} else {
			Log.error("Cannot find Java JDKs \"rt.jar\" (" + rtJar.getAbsolutePath() + ").");
		}
		final File jceJar = new File(javaHome + "/jre/lib/jce.jar");
		if (jceJar.exists()) {
			sootClasspath.append(classPathDelimiter + jceJar.getAbsolutePath().replace("\\", "/"));
		} else {
			Log.error("Cannot find Java JDKs \"jce.jar\" (" + jceJar.getAbsolutePath() + ").");
		}
		Log.msg("Setting soot's classpath to: " + sootClasspath, Log.DEBUG);
		return sootClasspath.toString();
	}

	private String getClassPathDelimiter() {
		final String system = System.getProperty("os.name").toLowerCase();
		if (system.contains("win")) {
			return ";";
		} else if (system.contains("nix") || system.contains("nux") || system.contains("aix") || system.contains("mac")
				|| system.contains("sunos")) {
			return ":";
		} else {
			Log.warning("Could not identify operating system.");
			if (new File(".").getAbsolutePath().contains(":")) {
				return ";";
			} else {
				return ":";
			}
		}
	}

	private List<String> getExcludeList() {
		final ManifestInfo manifestInfo = ManifestHelper.getInstance()
				.getManifest(Parameters.getInstance().getInputApkFile());
		final List<String> excludeList = new ArrayList<>();
		excludeList.addAll(Arrays.asList(new String[] { manifestInfo.getPkgName() + ".BuildConfig",
				manifestInfo.getPkgName() + ".R", manifestInfo.getPkgName() + ".R$*" }));
		if (!Parameters.getInstance().isIncludeOrdinaryLibraryPackages()) {
			for (final String exclude : SootHelper.getDefaultExcludes()) {
				excludeList.add(exclude + "*");
			}
		}
		Log.msg("Excluding: " + excludeList.toString(), Log.DEBUG);
		return excludeList;
	}

	private void prepareTargets() {
		if (Parameters.getInstance().getFrom() != null && Parameters.getInstance().getFrom().getClassname() != null
				&& !Parameters.getInstance().getFrom().getClassname().isEmpty()) {
			final SootClass sc = Scene.v().loadClassAndSupport(Parameters.getInstance().getFrom().getClassname());
			if (!sc.isApplicationClass() && sc.isLibraryClass()) {
				Log.msg("Slicing target (From) located in library class (" + sc + "). Making it an application class!",
						Log.NORMAL);
				sc.setApplicationClass();
			}
		}
		if (Parameters.getInstance().getTo() != null && Parameters.getInstance().getTo().getClassname() != null
				&& !Parameters.getInstance().getTo().getClassname().isEmpty()) {
			final SootClass sc = Scene.v().loadClassAndSupport(Parameters.getInstance().getTo().getClassname());
			if (!sc.isApplicationClass() && sc.isLibraryClass()) {
				Log.msg("Slicing target (To) located in library class (" + sc + "). Making it an application class!",
						Log.NORMAL);
				sc.setApplicationClass();
			}
		}
	}

	private boolean findSlicingTargets() {
		// Find slicing targets
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps()
				+ ": Identifying slicing criteria", Log.NORMAL);

		// Consider line-number
		if (Helper.getLineNumberSafe(Parameters.getInstance().getTo()) > 0
				|| Helper.getLineNumberSafe(Parameters.getInstance().getFrom()) > 0) {
			// Try 1: precise
			findSlicingTargets(false, true, false);
			if ((Parameters.getInstance().getTo() != null && this.targetTo == null)
					|| (Parameters.getInstance().getFrom() != null && this.targetFrom == null)) {
				// Try 2: generic
				findSlicingTargets(true, true, false);
			}
			if ((Parameters.getInstance().getTo() != null && this.targetTo == null)
					|| (Parameters.getInstance().getFrom() != null && this.targetFrom == null)) {
				// Try 3: in ordinary library
				findSlicingTargets(true, true, true);
			}
		}
		// Ignore line-number
		{
			// Try 1: precise
			if ((Parameters.getInstance().getTo() != null && this.targetTo == null)
					|| (Parameters.getInstance().getFrom() != null && this.targetFrom == null)) {
				findSlicingTargets(false, false, false);
			}
			if ((Parameters.getInstance().getTo() != null && this.targetTo == null)
					|| (Parameters.getInstance().getFrom() != null && this.targetFrom == null)) {
				// Try 2: generic
				findSlicingTargets(true, false, false);
			}
			if ((Parameters.getInstance().getTo() != null && this.targetTo == null)
					|| (Parameters.getInstance().getFrom() != null && this.targetFrom == null)) {
				// Try 3: in ordinary library
				findSlicingTargets(true, false, true);
			}
		}
		if (this.targetTo == null && this.targetFrom == null) {
			Log.error("No slicing target could be identified. Canceling execution! (Given input -> From: "
					+ (Parameters.getInstance().getFrom() != null ? Helper.toString(Parameters.getInstance().getFrom())
							: "-")
					+ ", To: "
					+ (Parameters.getInstance().getTo() != null ? Helper.toString(Parameters.getInstance().getTo())
							: "-")
					+ ")");
			return false;
		} else {
			if (Parameters.getInstance().getFrom() != null && this.targetFrom == null) {
				Log.error("\"From\" slicing target could not be identified. Canceling execution! (Given input -> From: "
						+ (Parameters.getInstance().getFrom() != null
								? Helper.toString(Parameters.getInstance().getFrom())
								: "-")
						+ ")");
				return false;
			} else if (Parameters.getInstance().getTo() != null && this.targetTo == null) {
				Log.error("\"To\" slicing target could not be identified. Canceling execution! (Given input -> To: "
						+ (Parameters.getInstance().getTo() != null ? Helper.toString(Parameters.getInstance().getTo())
								: "-")
						+ ")");
				return false;
			}
		}

		if (Parameters.getInstance().getFrom() != null) {
			final SootMethod method;
			if (Parameters.getInstance().isSimpleInput()) {
				method = SootHelper.getMethod(this.targetFrom);
			} else {
				method = null;
			}
			Log.msg("From-Criterion identified:\nStatement('" + this.targetFrom.toString() + "', "
					+ this.targetFrom.getJavaSourceStartLineNumber() + ")\n->Method('"
					+ Parameters.getInstance().getFrom().getMethod() + "')\n->Class('"
					+ Parameters.getInstance().getFrom().getClassname() + "')\n->App('"
					+ Parameters.getInstance().getFrom().getApp().getFile() + "')\nOriginal statement (Linenumber "
					+ Helper.getLineNumberSafe(Parameters.getInstance().getFrom().getStatement()) + "): "
					+ Helper.toString(Parameters.getInstance().getFrom().getStatement())
					+ (method != null
							? "\nOriginal method: " + method + "\nOriginal class: " + method.getDeclaringClass()
							: ""),
					Log.DEBUG);
		}
		if (Parameters.getInstance().getTo() != null) {
			final SootMethod method;
			if (Parameters.getInstance().isSimpleInput()) {
				method = SootHelper.getMethod(this.targetTo);
			} else {
				method = null;
			}
			Log.msg("To-Criterion identified:\nStatement('" + this.targetTo.toString() + "', "
					+ this.targetTo.getJavaSourceStartLineNumber() + ")\n->Method('"
					+ Parameters.getInstance().getTo().getMethod() + "')\n->Class('"
					+ Parameters.getInstance().getTo().getClassname() + "')\n->App('"
					+ Parameters.getInstance().getTo().getApp().getFile() + "')\nOriginal statement (Linenumber "
					+ Helper.getLineNumberSafe(Parameters.getInstance().getTo().getStatement()) + "): "
					+ Helper.toString(Parameters.getInstance().getTo().getStatement())
					+ (method != null
							? "\nOriginal method: " + method + "\nOriginal class: " + method.getDeclaringClass()
							: ""),
					Log.DEBUG);
		}

		return true;
	}

	private void findSlicingTargets(boolean weakComparison, boolean considerLineNumber,
			boolean searchOrdinaryLibraryClasses) {
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete() && (!Parameters.getInstance().isSimpleInput()
					|| (searchOrdinaryLibraryClasses && SootHelper.isOrdinaryLibraryClass(sc, false))
					|| !SootHelper.isOrdinaryLibraryClass(sc, false))) {
				for (final SootMethod sm : sc.getMethods()) {
					final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
					if (body == null) {
						continue;
					}
					for (final Unit unit : body.getUnits()) {
						if (unit instanceof SwitchStmt || unit instanceof GotoStmt || unit instanceof IfStmt) {
							continue;
						}
						if (isFromOrTo(Parameters.getInstance().getFrom(), unit, sm, sc, weakComparison,
								considerLineNumber)) {
							this.targetFrom = Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
							if (this.targetFrom == null) {
								this.targetFrom = unit;
							}
							Parameters.getInstance().getFrom().getStatement()
									.setLinenumber(unit.getJavaSourceStartLineNumber());
							if (Parameters.getInstance().getFrom().getClassname() == null
									|| Parameters.getInstance().getFrom().getClassname().isEmpty()) {
								Parameters.getInstance().getFrom().setClassname(sc.toString());
							}
							if (Parameters.getInstance().getFrom().getMethod() == null
									|| Parameters.getInstance().getFrom().getMethod().isEmpty()) {
								Parameters.getInstance().getFrom().setMethod(sm.toString());
							}
							if (Parameters.getInstance().getTo() == null || this.targetTo != null) {
								break;
							}
						}
						// No else here! In some seldom situations a slice which consists of one statement only may make sense.
						if (isFromOrTo(Parameters.getInstance().getTo(), unit, sm, sc, weakComparison,
								considerLineNumber)) {
							this.targetTo = Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
							if (this.targetTo == null) {
								this.targetTo = unit;
							}
							Parameters.getInstance().getTo().getStatement()
									.setLinenumber(unit.getJavaSourceStartLineNumber());
							if (Parameters.getInstance().getTo().getClassname() == null
									|| Parameters.getInstance().getTo().getClassname().isEmpty()) {
								Parameters.getInstance().getTo().setClassname(sc.toString());
							}
							if (Parameters.getInstance().getTo().getMethod() == null
									|| Parameters.getInstance().getTo().getMethod().isEmpty()) {
								Parameters.getInstance().getTo().setMethod(sm.toString());
							}
							if (Parameters.getInstance().getFrom() == null || this.targetFrom != null) {
								break;
							}
						}
					}
					if ((Parameters.getInstance().getFrom() == null || this.targetFrom != null)
							&& (Parameters.getInstance().getTo() == null || this.targetTo != null)) {
						break;
					}
				}
			}
			if ((Parameters.getInstance().getFrom() == null || this.targetFrom != null)
					&& (Parameters.getInstance().getTo() == null || this.targetTo != null)) {
				break;
			}
		}
	}

	private void handleExceptions() {
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Handling Exceptions",
				Log.NORMAL);

		// Explicitly handling exceptions thrown
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete() && (Parameters.getInstance().isSliceOrdinaryLibraryPackages()
					|| !SootHelper.isOrdinaryLibraryClass(sc, false))) {
				final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
				for (final SootMethod sm : snapshotIterator) {
					if (sm.isConcrete()) {
						final DependenceGraph pdg = Data.getInstance().getPDG(sm);
						final Body b = sm.retrieveActiveBody();
						// Get Targets
						for (final Trap t : b.getTraps()) {
							if (t.getHandlerUnit() instanceof IdentityStmt) {
								final IdentityStmt target = (IdentityStmt) t.getHandlerUnit();
								if (target.getRightOp() instanceof CaughtExceptionRef) {
									final Type targetType = target.getLeftOp().getType();

									// Add edges
									for (final Unit unit : b.getUnits()) {
										if (unit instanceof InvokeStmt) {
											final InvokeStmt castedUnit = (InvokeStmt) unit;
											final DependenceGraph pdgOfCalledMethod = Data.getInstance()
													.getPDG(castedUnit.getInvokeExpr().getMethod());
											if (pdgOfCalledMethod != null
													&& pdgOfCalledMethod.getExceptionsThrown(targetType) != null) {
												for (final Unit thrownBy : pdgOfCalledMethod
														.getExceptionsThrown(targetType)) {
													final Edge temp = new Edge(thrownBy, target,
															DependenceGraph.TYPE_EXCEPTION);
													Log.msg("Adding exception edge: " + temp, Log.DEBUG);
													pdg.addEdge(temp);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void buildDummyMainPDG() {
		Log.setSilence(false);

		if (Parameters.getInstance().isAndroidMode()) {
			// Build DummyMain PDG
			Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps()
					+ ": Constructing DummyMain PDG", Log.NORMAL);

			this.lifeCycleHandler = new LifeCycleHandler(Parameters.getInstance().getInputApkFile());
			int counter = 0;
			final int max = this.lifeCycleHandler.getDummyMainMethods().size();
			if (max <= 0) {
				this.noDummyMain = true;
			}
			for (final SootMethod dummyMain : this.lifeCycleHandler.getDummyMainMethods()) {
				Log.msg(++counter + "/" + max + ": Building PDG for " + dummyMain.getDeclaringClass().getName() + " -> "
						+ dummyMain.getName(), Log.NORMAL);
				final DependenceGraph pdg = PDGHelper.getPDG(dummyMain.getActiveBody());
				Data.getInstance().addPdg(dummyMain, pdg);
			}
		}
	}

	private void addCallBackEdges() {
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Adding Callbacks to PDGs",
				Log.NORMAL);

		if (this.lifeCycleHandler != null && Data.getInstance().getCallBackClasses() != null
				&& !Data.getInstance().getCallBackClasses().isEmpty()) {
			for (final DependenceGraph pdg : Data.getInstance()) {
				final Set<Edge> toAdd = new HashSet<>();
				for (final Unit useUnit : pdg) {
					if (useUnit instanceof Stmt) {
						final Stmt castedUseUnit = (Stmt) useUnit;
						if (castedUseUnit.containsInvokeExpr()) {
							final InvokeExpr invoke = castedUseUnit.getInvokeExpr();
							for (int i = 0; i < invoke.getMethod().getParameterCount(); i++) {
								if (Data.getInstance().getCallBackClasses()
										.contains(invoke.getMethod().getParameterType(i).toString())) {
									final Value useValue = invoke.getArg(i);
									if (SootHelper.isCallBackClass(useValue)) {
										// Step 1:
										for (final SootMethod sm : SootHelper
												.getAllAccessibleMethods(useValue.getType())) {
											if (SootHelper.isCallBackMethod(sm)
													&& !Data.getInstance().getEntryNodes(sm).isEmpty()) {
												final Set<Unit> entryNodes = Data.getInstance().getEntryNodes(sm);
												if (entryNodes.size() > 1) {
													Log.warning("Over-approximating due to " + entryNodes.size()
															+ " callback candidates when dealing with: " + useUnit
															+ " (Method: " + pdg.getReference() + ")");
												}
												for (final Unit entryNode : entryNodes) {
													toAdd.add(new Edge(useUnit, entryNode,
															DependenceGraph.TYPE_CALLBACK));
												}
												for (final Unit entryNode1 : entryNodes) {
													for (final Unit entryNode2 : entryNodes) {
														if (entryNode1 != entryNode2) {
															toAdd.add(new Edge(entryNode1, entryNode2,
																	DependenceGraph.TYPE_CALLBACK_ALTERNATIVE));
														}
													}
												}
											}
										}

										// Step 2:
										Set<Unit> defUnits = null;
										for (final ReachingDefinition rd : Data.getInstance().getDefValuesMap()
												.get(useUnit)) {
											if (rd.getValueOrField().equals(new ValueOrField(useValue))) {
												defUnits = rd.getUnits();
												break;
											}
										}
										if (defUnits != null) {
											for (final Unit defUnit : defUnits) {
												toAdd.add(new Edge(useUnit, defUnit,
														DependenceGraph.TYPE_CALLBACK_DEFINITION));
											}
										}
									}
								}
							}
						}
					}
				}
				for (final Edge edge : toAdd) {
					Log.msg("Adding "
							+ (edge.getType() == DependenceGraph.TYPE_CALLBACK ? "callback"
									: edge.getType() == DependenceGraph.TYPE_CALLBACK_DEFINITION ? "callback-definition"
											: "callback-alternative")
							+ " edge: " + DependenceGraph.nodeToString(edge.getFrom(), null).replace("\n", " ") + " -> "
							+ DependenceGraph.nodeToString(edge.getTo(), null).replace("\n", " "), Log.DEBUG);
					pdg.addEdge(edge);
				}
			}
		}
	}

	private DependenceGraph buildSDG() {
		// Build SDG
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Constructing SDG",
				Log.NORMAL);

		Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).start();
		final PDGConnector connector = new PDGConnector();
		final DependenceGraph sdg = connector.buildSDG();
		InputEdgesHandler.addInputEdges(sdg);
		Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).stop();

		Statistics.getCounter(Statistics.COUNTER_SDG_EDGES)
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL_ENTRY])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CONTROL])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_DATA])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_IN])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_PARAMETER_OUT])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALL])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_SUMMARY])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_FIELD_DATA])
						.getCounter())
				.increase(Statistics
						.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_STATIC_FIELD_DATA])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK])
						.getCounter())
				.increase(Statistics
						.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_DEFINITION])
						.getCounter())
				.increase(Statistics
						.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_CALLBACK_ALTERNATIVE])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_EXCEPTION])
						.getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[DependenceGraph.TYPE_INPUT])
						.getCounter());
		Statistics.update();

		return sdg;
	}

	private DependenceGraph sliceSDG(DependenceGraph sdg) {
		Statistics.getTimer(Statistics.TIMER_SLICING).start();
		Statistics.getTimer(Statistics.TIMER_SLICING_SDG).start();

		// Slice SDG
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Slicing SDG", Log.NORMAL);

		// Update targets
		if (this.targetFrom != null
				&& Data.getInstance().getReplacedNodesOriginalToReplacement().containsKey(this.targetFrom)) {
			this.targetFrom = Data.getInstance().getReplacedNodesOriginalToReplacement().get(this.targetFrom);
		}
		if (this.targetTo != null
				&& Data.getInstance().getReplacedNodesOriginalToReplacement().containsKey(this.targetTo)) {
			this.targetTo = Data.getInstance().getReplacedNodesOriginalToReplacement().get(this.targetTo);
		}

		// Slicing SDG
		DependenceGraph sdgSliced = sdg.clone();
		Set<Unit> toSlice = null;
		if (this.targetTo != null) {
			Log.msg("Slicing to:\nStatement('" + this.targetTo.toString() + "', "
					+ this.targetTo.getJavaSourceStartLineNumber() + ")\n->Method('"
					+ Parameters.getInstance().getTo().getMethod() + "')\n->Class('"
					+ Parameters.getInstance().getTo().getClassname() + "')\n->App('"
					+ Parameters.getInstance().getTo().getApp().getFile() + "')\nOriginal statement (Linenumber "
					+ Helper.getLineNumberSafe(Parameters.getInstance().getTo().getStatement()) + "): "
					+ Helper.toString(Parameters.getInstance().getTo().getStatement()), Log.IMPORTANT);
			Statistics.getTimer(Statistics.TIMER_SLICING_TO).start();
			final SDGBackwardSlicer slicer = new SDGBackwardSlicer(sdgSliced,
					Statistics.getCounter(Statistics.COUNTER_SLICING_TO), this.targetTo);
			toSlice = slicer.slice();
			Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_TO).increase(toSlice.size());
			sdgSliced.removeNodes(sdgSliced.notIn(toSlice));
			Statistics.getTimer(Statistics.TIMER_SLICING_TO).stop();
			Log.msg("done", Log.DEBUG);
		}
		Set<Unit> fromSlice = null;
		Set<Unit> sliceIgnoringFields = null;
		if (this.targetFrom != null) {
			if (Parameters.getInstance().getMode() == Parameters.MODE_SLICE_OUT
					|| Parameters.getInstance().isIncomplete() || Parameters.getInstance().isFieldFiltering()) {
				Log.msg("Slicing from:\nStatement('" + this.targetFrom.toString() + "')\n->Method('"
						+ Parameters.getInstance().getFrom().getMethod() + "', "
						+ this.targetFrom.getJavaSourceStartLineNumber() + ")\n->Class('"
						+ Parameters.getInstance().getFrom().getClassname() + "')\n->App('"
						+ Parameters.getInstance().getFrom().getApp().getFile() + "')\nOriginal statement (Linenumber "
						+ Helper.getLineNumberSafe(Parameters.getInstance().getFrom().getStatement()) + "): "
						+ Helper.toString(Parameters.getInstance().getFrom().getStatement()), Log.IMPORTANT);
				Statistics.getTimer(Statistics.TIMER_SLICING_FROM).start();
				final SDGForwardSlicer slicerWrtFields;
				if (this.targetTo != null && Parameters.getInstance().isFieldFiltering()) {
					final FieldCollector fc = new FieldCollector(sdgSliced, this.targetFrom);
					slicerWrtFields = new SDGForwardSlicer(sdgSliced,
							Statistics.getCounter(Statistics.COUNTER_SLICING_FROM), this.targetFrom,
							fc.collectFields());
				} else {
					slicerWrtFields = new SDGForwardSlicer(sdgSliced,
							Statistics.getCounter(Statistics.COUNTER_SLICING_FROM), this.targetFrom);
				}
				fromSlice = slicerWrtFields.slice();
				if (this.targetTo != null && Parameters.getInstance().getMode() != Parameters.MODE_SLICE_OUT
						&& !Parameters.getInstance().isIncomplete()) {
					final SDGForwardSlicer slicerIgnoringFields = new SDGForwardSlicer(sdgSliced,
							Statistics.getCounter(Statistics.COUNTER_SLICING_FROM), this.targetFrom);
					sliceIgnoringFields = slicerIgnoringFields.slice();
				}
				Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_FROM).increase(fromSlice.size());
				if (sliceIgnoringFields != null) {
					sliceIgnoringFields.removeAll(fromSlice);
					sdgSliced.removeNodes(sliceIgnoringFields);
				} else {
					final Set<Unit> remove = sdgSliced.notIn(fromSlice);
					sdgSliced.removeNodes(remove);
				}
				Statistics.getTimer(Statistics.TIMER_SLICING_FROM).stop();
				Log.msg("done", Log.DEBUG);
			}
		}
		Set<Unit> contextSensitiveRefined = null;
		if (toSlice != null && fromSlice != null && Parameters.getInstance().isContextSensitiveRefinement()
				&& Parameters.getInstance().getMode() != Parameters.MODE_SLICE_OUT
				&& !Parameters.getInstance().isIncomplete()) {
			contextSensitiveRefined = JicerExtension.contextSensitiveRefinement(sdgSliced, this.targetTo, toSlice,
					fromSlice);
		}
		final Set<Unit> sliceItself = new HashSet<>(sdgSliced.getAllNodes());
		if (this.targetFrom != null) {
			if (Parameters.getInstance().getMode() == Parameters.MODE_SLICE_OUT
					|| Parameters.getInstance().isIncomplete()) {
				this.fromToSliceIsEmpty = sliceItself.size() == 0 || this.targetTo == null || this.targetFrom == null;
			} else {
				this.fromToSliceIsEmpty = !sdgSliced.getAllNodes().contains(this.targetFrom);
			}
		}

		// Slicing SDG (slice mode only)
		if ((Parameters.getInstance().getMode() == Parameters.MODE_SLICE
				|| Parameters.getInstance().getMode() == Parameters.MODE_SHOW_SLICE)
				&& !Parameters.getInstance().isIncomplete()) {
			final Set<Unit> toKeep = new HashSet<>(sdgSliced.getAllNodes());

			sliceExtras(sdg, toKeep);

			// Set complete slice
			final DependenceGraph sdgSlicedComplete = sdg.clone();
			sdgSlicedComplete.removeNodes(sdgSlicedComplete.notIn(toKeep));
			sdgSliced = sdgSlicedComplete;
		}

		Statistics.getCounter(Statistics.COUNTER_SLICING)
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_TO).getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_FROM).getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_EXTRAS).getCounter());

		Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE)
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_TO).getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_FROM).getCounter())
				.increase(Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_EXTRAS).getCounter());

		// Draw slicing SDG
		if (Parameters.getInstance().isDrawGraphs()) {
			final GraphDrawer gdSDGsliced = new GraphDrawer("SDG", sdg, sdgSliced);
			gdSDGsliced.setHighlightIn(sliceItself);
			gdSDGsliced.setHighlightOut1(sliceIgnoringFields);
			gdSDGsliced.setHighlightOut2(contextSensitiveRefined);
			gdSDGsliced.drawGraph("data/temp/graphs/sdg_slicing", 0);
		}

		Statistics.getTimer(Statistics.TIMER_SLICING_SDG).stop();

		return sdgSliced;
	}

	private void sliceExtras(DependenceGraph sdg, Set<Unit> toKeep) {
		Log.msg("Extra Slices required?", Log.DEBUG);
		final Set<Unit> toSliceExtra = new HashSet<>();
		final Set<Unit> visited = new HashSet<>();
		for (final Unit unit : toKeep) {
			Set<ReachingDefinition> rds = Data.getInstance().getDefValuesMap().get(unit);
			if (rds == null && Data.getInstance().getReplacedNodesReplacementToOriginal().get(unit) != null) {
				rds = Data.getInstance().getDefValuesMap()
						.get(Data.getInstance().getReplacedNodesReplacementToOriginal().get(unit));
			}
			if (!visited.contains(unit) && rds != null) {
				visited.add(unit);

				// Get all used values
				final Set<ValueOrField> useValues = new HashSet<>();
				for (final ValueBox vb : unit.getUseBoxes()) {
					final Value v = vb.getValue();
					if ((v instanceof Local || v instanceof Ref)
							&& !(v instanceof ParameterRef || v instanceof ThisRef)) {
						useValues.add(new ValueOrField(v));
					}
				}
				if (unit instanceof Stmt) {
					final Stmt castedUnit = (Stmt) unit;
					if (castedUnit.containsInvokeExpr()) {
						if (castedUnit.getInvokeExpr() instanceof InstanceInvokeExpr) {
							useValues
									.add(new ValueOrField(((InstanceInvokeExpr) castedUnit.getInvokeExpr()).getBase()));
						}
					}
				}

				// Is defined?
				for (final ValueOrField useValue : useValues) {
					boolean found = false;
					for (final ReachingDefinition rd : rds) {
						if (rd.getValueOrField().equals(useValue)) {
							for (Unit defUnit : rd.getUnits()) {
								if (defUnit != null) {
									if (Data.getInstance().getReplacedNodesOriginalToReplacement()
											.containsKey(defUnit)) {
										defUnit = Data.getInstance().getReplacedNodesOriginalToReplacement()
												.get(defUnit);
									}
									if (!toKeep.contains(defUnit)) {
										Log.msg("Found undefined value \"" + useValue + "\" in \nStatement('"
												+ unit.toString() + "')\n->App('"
												+ (Parameters.getInstance().getFrom() == null
														? Parameters.getInstance().getTo().getApp().getFile()
														: Parameters.getInstance().getFrom().getApp().getFile())
												+ "')", Log.DEBUG);
										toSliceExtra.add(unit);
										found = true;
										break;
									}
								}
							}
						}
					}
					if (found) {
						break;
					}
				}
			}
		}

		if (!toSliceExtra.isEmpty()) {
			final String msg = "required extra:\nStatement('" + JicerExtension.UNIT_NEEDLE + "', "
					+ JicerExtension.UNIT_LINENUMBER_NEEDLE + ")\n->App('"
					+ (Parameters.getInstance().getFrom() == null ? Parameters.getInstance().getTo().getApp().getFile()
							: Parameters.getInstance().getFrom().getApp().getFile())
					+ "')";
			final JicerExtension je = new JicerExtension(msg, Statistics.getTimer(Statistics.TIMER_SLICING_EXTRAS),
					Statistics.getCounter(Statistics.COUNTER_SLICING_EXTRAS),
					Statistics.getCounter(Statistics.COUNTER_SLICING_SIZE_EXTRAS), sdg, true, toKeep, toSliceExtra);
			je.slice();
		}
	}

	private void sliceCFG(DependenceGraph sdgSliced, DependenceGraph sdg) {
		Statistics.getTimer(Statistics.TIMER_SLICING_CFG).start();

		final CFGSlicer cfgSlicer = new CFGSlicer(sdgSliced, sdg, Parameters.getInstance().isRunnable(),
				Parameters.getInstance().isSliceOrdinaryLibraryPackages(), Parameters.getInstance().isIncomplete(),
				Parameters.getInstance().getOutputFormat() != Parameters.OUTPUT_FORMAT_NONE
						&& Parameters.getInstance().getOutputFormat() != Parameters.OUTPUT_FORMAT_JIMPLE);

		// Slice CFG
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Slicing CFG (Mode: "
				+ (Parameters.getInstance().getMode() == Parameters.MODE_SHOW_SLICE ? "Show slice"
						: (Parameters.getInstance().getMode() == Parameters.MODE_SLICE_OUT ? "Remove slice from app"
								: "Keep slice only"))
				+ ")", Log.NORMAL);
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete() && (Parameters.getInstance().isSliceOrdinaryLibraryPackages()
					|| !SootHelper.isOrdinaryLibraryClass(sc, false))) {
				if (Parameters.getInstance().getMode() == Parameters.MODE_SHOW_SLICE || Log.logIt(Log.DEBUG)) {
					cfgSlicer.show(sc);
				}
				if (Parameters.getInstance().getMode() == Parameters.MODE_SLICE_OUT) {
					cfgSlicer.sliceout(sc);
				} else if (Parameters.getInstance().getMode() == Parameters.MODE_SLICE) {
					cfgSlicer.slice(sc);
				}
			}
		}

		Statistics.getTimer(Statistics.TIMER_SLICING_CFG).stop();

		// Remove dummy main
		if (Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_APK
				&& this.lifeCycleHandler.getDummyMainClass() != null) {
			Log.msg("Removing dummy main class", Log.NORMAL);
			Scene.v().getApplicationClasses().remove(this.lifeCycleHandler.getDummyMainClass());
		}

		// Add support classes if excluded
		if (!Parameters.getInstance().isIncludeOrdinaryLibraryPackages()
				&& Parameters.getInstance().getOutputFormat() == Parameters.OUTPUT_FORMAT_APK) {
			reAddSupportClasses();
		}
	}

	private void reAddSupportClasses() {
		// Load required support classes
		Log.msg("Loading required support classes", Log.NORMAL);
		final Set<String> requiredSupportClasses = new HashSet<>();
		final Set<String> alreadyApplicationClasses = new HashSet<>();
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			alreadyApplicationClasses.add(sc.getName());
			for (final SootClass suppClass : SootHelper.getAllAccessibleClassesAndInterfaces(sc)) {
				requiredSupportClasses.add(suppClass.getName());
			}
		}
		for (final String className : requiredSupportClasses) {
			if (className.startsWith("android.support.")) {
				if (!alreadyApplicationClasses.contains(className)) {
					try {
						final SootClass sc = Scene.v().loadClassAndSupport(className);
						Log.msg("Adding support class: " + className, Log.DEBUG);
						Scene.v().getApplicationClasses().add(sc);
					} catch (final Exception e) {
						Log.warning(
								"Could not load required support class: " + className + Log.getExceptionAppendix(e));
					}
				}
			}
		}

		// Load required layout classes
		if (Parameters.getInstance().isRunnable()) {
			Log.msg("Find and parse layout files", Log.NORMAL);

			// Find layout files
			final Set<String> layoutFiles = new HashSet<>();
			try (ZipFile zipFile = new ZipFile(Parameters.getInstance().getInputApkFile())) {
				final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
				while (zipEntries.hasMoreElements()) {
					final String fileName = zipEntries.nextElement().getName();
					if (fileName.startsWith("res/layout") && fileName.endsWith(".xml")) {
						layoutFiles.add(fileName);
					}
				}
			} catch (final IOException e) {
				Log.warning("Could not read zipped content of APK file: "
						+ Parameters.getInstance().getInputApkFile().getAbsolutePath() + Log.getExceptionAppendix(e));
			}

			// Parse layout files
			final ApkFile apkFile = ManifestHelper.getInstance()
					.getApkParserFile(Parameters.getInstance().getInputApkFile());
			final Set<String> allClassesInLayoutFiles = new HashSet<>();
			for (final String layoutFile : layoutFiles) {
				try {
					allClassesInLayoutFiles.addAll(findLayoutClasses(apkFile.transBinaryXml(layoutFile)));
				} catch (final IOException e) {
					Log.warning("Could not parse layout file: " + layoutFile + Log.getExceptionAppendix(e));
				}
			}

			Log.msg("Adding classes required due to layouts", Log.NORMAL);
			for (final String classname : allClassesInLayoutFiles) {
				final SootClass sc = Scene.v().loadClassAndSupport(classname);
				if (!Scene.v().getApplicationClasses().contains(sc)) {
					Scene.v().getApplicationClasses().add(sc);
					Log.msg("Adding layout class: " + classname, Log.NORMAL);
				}
			}
		}
	}

	private Set<String> findLayoutClasses(String layoutFileContent) {
		final Pattern pattern = Pattern.compile("(<| )([a-zA-Z])+[.]([a-zA-Z.])+(>|/|\\s)");
		final Matcher matcher = pattern.matcher(layoutFileContent);

		final Set<String> listClassnames = new HashSet<>();
		while (matcher.find()) {
			listClassnames.add(matcher.group().replaceAll("[<>/\\s]", ""));
		}
		return listClassnames;
	}

	private boolean isFromOrTo(Reference fromOrTo, Unit unit, SootMethod sm, SootClass sc, boolean weakComparison,
			boolean considerLineNumber) {
		if (fromOrTo != null) {
			final String unitStr = unit.toString();
			if (Parameters.getInstance().isSimpleInput()) {
				if (considerLineNumber && fromOrTo.getStatement().getLinenumber() != null) {
					if (fromOrTo.getStatement().getLinenumber().intValue() != unit.getJavaSourceStartLineNumber()) {
						return false;
					}
				}
				if (unitStr.contains(fromOrTo.getStatement().getStatementfull())) {
					if (fromOrTo.getMethod() == null || fromOrTo.getMethod().isEmpty()
							|| sm.toString().contains(fromOrTo.getMethod())) {
						if ((fromOrTo.getClassname() == null || fromOrTo.getClassname().isEmpty()
								|| sc.toString().contains(fromOrTo.getClassname()))) {
							return true;
						}
					}
				}
			} else if (weakComparison) {
				final Statement statement = Helper.createStatement(unitStr);
				final EqualsOptions options = EqualsOptions.DEFAULT;
				options.setOption(EqualsOptions.CONSIDER_LINENUMBER, considerLineNumber);
				if (EqualsHelper.equals(statement, fromOrTo.getStatement(), options)) {
					if (fromOrTo.getMethod() == null || fromOrTo.getMethod().isEmpty()
							|| sm.toString().equals(fromOrTo.getMethod())) {
						if ((fromOrTo.getClassname() == null || fromOrTo.getClassname().isEmpty()
								|| sc.toString().equals(fromOrTo.getClassname()))) {
							return true;
						}
					}
				}
			} else {
				if (unitStr.equals(fromOrTo.getStatement().getStatementfull())) {
					if (considerLineNumber && fromOrTo.getStatement().getLinenumber() != null) {
						if (fromOrTo.getStatement().getLinenumber().intValue() != unit.getJavaSourceStartLineNumber()) {
							return false;
						}
					}
					if (fromOrTo.getMethod() == null || fromOrTo.getMethod().isEmpty()
							|| sm.toString().equals(fromOrTo.getMethod())) {
						if ((fromOrTo.getClassname() == null || fromOrTo.getClassname().isEmpty()
								|| sc.toString().equals(fromOrTo.getClassname()))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void writeOutput() {
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Writing output",
				Log.NORMAL);
		if (Log.getLogLevel() >= Log.DEBUG) {
			Log.setSilence(false);
		} else {
			Log.setSilence(Log.SILENCE_LEVEL_MSG);
		}

		boolean retry = true;
		int retryCounter = 0;
		while (retry && retryCounter < 100) {
			if (retryCounter > 0) {
				try {
					// Decoy until fixed (see: https://github.com/soot-oss/soot/pull/1116)
					Thread.sleep(10);
				} catch (final InterruptedException intEx) {
					// Catch to ignore InterruptedException related to Soot's writer (which is thrown about 8 times, if thrown at all)
				}
			}
			try {
				retry = false;
				retryCounter++;
				PackManager.v().writeOutput();
			} catch (final ConcurrentModificationException conEx) {
				retry = true;
				Log.warning(
						"Problem while writing output occured (Related to known Soot-Bug: https://github.com/soot-oss/soot/pull/1116). Retrying..."
								+ Log.getExceptionAppendix(conEx));
			}
		}
		Log.setSilence(false);
	}

	private void signAndMoveToOutput() {
		Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Moving output",
				Log.NORMAL);

		// Move file
		final File outputApkFile = getOutputFile();
		try {
			final File sootOutputApkFile = new File(Parameters.TEMP_OUTPUT_DIR,
					Parameters.getInstance().getInputApkFile().getName());
			Files.move(sootOutputApkFile.toPath(), outputApkFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Log.msg("Output .apk file written: " + outputApkFile.getAbsolutePath(), Log.IMPORTANT);
		} catch (final IOException e) {
			Log.error("Could not write output .apk file." + Log.getExceptionAppendix(e));
		}

		// Signing
		if (Parameters.getInstance().isSign()) {
			Log.msg("Step " + ++this.stepCounter + "/" + Parameters.getInstance().getSteps() + ": Signing output",
					Log.NORMAL);
			Statistics.getTimer(Statistics.TIMER_SIGNING).start();
			if (Parameters.getInstance().isAndroidMode()) {
				final APKSigner signer = new APKSigner(new File(Config.getInstance().zipalignPath),
						new File(Config.getInstance().apksignerPath));
				if (signer.sign(outputApkFile)) {
					Log.msg(outputApkFile.getAbsolutePath() + " signed successfully!", Log.NORMAL);
				} else {
					Log.msg("Failed signing: " + outputApkFile.getAbsolutePath(), Log.NORMAL);
				}
			} else {
				Log.msg("Skip signing output since it is no APK file.", Log.NORMAL);
			}
			Statistics.getTimer(Statistics.TIMER_SIGNING).stop();
		}
	}

	private void convertToClass() {
		Log.msg("Converting to .class output", Log.NORMAL);
		if (Log.getLogLevel() >= Log.DEBUG) {
			Log.setSilence(false);
		} else {
			Log.setSilence(Log.SILENCE_LEVEL_MSG);
		}

		G.reset();

		Options.v().set_output_format(Options.output_format_class);
		Options.v().set_src_prec(Options.src_prec_jimple);

		final File outputClassFile = getOutputFile();
		if (outputClassFile.getParentFile() != null) {
			Options.v().set_output_dir(outputClassFile.getAbsolutePath());
		} else {
			Options.v().set_output_dir(".");
		}

		Options.v().set_soot_classpath(getClasspath());
		Options.v().set_process_dir(Collections.singletonList(Parameters.TEMP_OUTPUT_DIR.getAbsolutePath()));

		soot.Main.main(new String[] { Parameters.getInstance().getInputApkFile().getName().substring(0,
				Parameters.getInstance().getInputApkFile().getName().lastIndexOf(".class")) });

		Log.setSilence(false);
		Log.msg("Output .class file written: " + outputClassFile.getAbsolutePath(), Log.IMPORTANT);
	}

	private void moveJimpleOutput() {
		Log.msg("Moving output", Log.NORMAL);
		try {
			Files.move(Parameters.TEMP_OUTPUT_DIR.toPath(), getOutputDir().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			Log.msg("Output .jimple files written to: " + getOutputDir().getAbsolutePath(), Log.IMPORTANT);
		} catch (final IOException e) {
			Log.error("Could not write output .jimple files." + Log.getExceptionAppendix(e));
		}
	}

	private File getOutputFile() {
		// Determine output .apk/.class file
		if (Parameters.getInstance().getOutputFile() == null) {
			Parameters.getInstance().setOutputFile(new File(Parameters.getInstance().getInputApkFile().getName()));
		} else {
			if ((Parameters.getInstance().isAndroidMode()
					&& Parameters.getInstance().getOutputFile().getName().endsWith(".apk"))
					|| (!Parameters.getInstance().isAndroidMode()
							&& Parameters.getInstance().getOutputFile().getName().endsWith(".class"))) {
				if (Parameters.getInstance().getOutputFile().getParentFile() != null
						&& !Parameters.getInstance().getOutputFile().getParentFile().exists()) {
					Parameters.getInstance().getOutputFile().getParentFile().mkdirs();
				}
			} else if (!Parameters.getInstance().getOutputFile().exists()) {
				Parameters.getInstance().getOutputFile().mkdirs();
			}
			if (Parameters.getInstance().getOutputFile().isDirectory()) {
				Parameters.getInstance().setOutputFile(new File(Parameters.getInstance().getOutputFile(),
						Parameters.getInstance().getInputApkFile().getName()));
			}
		}
		return Parameters.getInstance().getOutputFile();
	}

	private File getOutputDir() {
		// Determine output directory
		final String targetStr;
		if (Parameters.getInstance().getInputApkFile().getName().contains(".")) {
			targetStr = Helper.cutFromStart(Parameters.getInstance().getInputApkFile().getName(), ".",
					Helper.OCCURENCE_LAST);
		} else {
			targetStr = Parameters.getInstance().getInputApkFile().getName();
		}
		if (Parameters.getInstance().getOutputFile() == null) {
			Parameters.getInstance().setOutputFile(new File(targetStr + "_jimple"));
		} else if (Parameters.getInstance().getOutputFile().exists()
				&& !Parameters.getInstance().getOutputFile().isDirectory()) {
			Parameters.getInstance()
					.setOutputFile(new File(Parameters.getInstance().getOutputFile(), targetStr + "_jimple"));
		}
		if (!Parameters.getInstance().getOutputFile().exists()) {
			Parameters.getInstance().getOutputFile().mkdir();
		}
		return Parameters.getInstance().getOutputFile();
	}
}