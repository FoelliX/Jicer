package de.foellix.aql.jicer;

import java.io.File;

import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.Helper;

public class Parameters {
	public static final int DEFAULT_K_LIMIT_FOR_ANALYSIS = 100000;
	public static final File TEMP_OUTPUT_DIR = new File("data/temp/sootOutput");

	public static final String OUTPUT_FORMAT_APK = "APK";
	public static final String OUTPUT_FORMAT_JIMPLE = "JIMPLE";
	public static final String OUTPUT_FORMAT_CLASS = "CLASS";
	public static final String OUTPUT_FORMAT_NONE = "NONE";

	public static final String MODE_SLICE = "SLICE";
	public static final String MODE_SLICE_OUT = "SLICEOUT";
	public static final String MODE_SHOW_SLICE = "SHOW";

	private int kLimit;
	private File inputApkFile;
	private int steps = 11; // Without signing
	private File outputFile;
	private String mode;
	private String outputFormat;
	private boolean simpleInput;
	private boolean androidMode;
	private Reference from;
	private Reference to;
	private boolean drawGraphs;
	private boolean includeOrdinaryLibraryPackages;
	private boolean sliceOrdinaryLibraryPackages;
	private boolean sign;
	private boolean recordStatistics;
	private boolean incomplete;
	private boolean runnable;
	private boolean fieldFiltering;
	private boolean contextSensitiveRefinement;
	private boolean strictThreadSensitivity;
	private boolean overapproximateStubDroid;
	private int xss;
	private int xmx;
	private File aqlAnswerFile;

	private static Parameters instance = new Parameters();

	private Parameters() {
		reset();
	}

	public static Parameters getInstance() {
		return instance;
	}

	public void reset() {
		this.kLimit = DEFAULT_K_LIMIT_FOR_ANALYSIS;
		this.mode = MODE_SLICE;
		this.from = null;
		this.to = null;
		this.outputFormat = OUTPUT_FORMAT_APK;
		this.androidMode = true;
		this.simpleInput = false;
		this.drawGraphs = false;
		this.includeOrdinaryLibraryPackages = true;
		this.sliceOrdinaryLibraryPackages = false;
		this.sign = false;
		this.incomplete = false;
		this.runnable = false;
		this.recordStatistics = true;
		this.fieldFiltering = true;
		this.contextSensitiveRefinement = true;
		this.strictThreadSensitivity = false;
		this.overapproximateStubDroid = false;
		this.xss = 2;
		this.xmx = 8000;
	}

	public Parameters copy() {
		final Parameters newParameters = new Parameters();
		newParameters.kLimit = this.kLimit;
		newParameters.mode = this.mode;
		newParameters.from = Helper.copy(this.from);
		newParameters.to = Helper.copy(this.to);
		newParameters.outputFormat = this.outputFormat;
		newParameters.androidMode = this.androidMode;
		newParameters.simpleInput = this.simpleInput;
		newParameters.drawGraphs = this.drawGraphs;
		newParameters.includeOrdinaryLibraryPackages = this.includeOrdinaryLibraryPackages;
		newParameters.sliceOrdinaryLibraryPackages = this.sliceOrdinaryLibraryPackages;
		newParameters.sign = this.sign;
		newParameters.incomplete = this.incomplete;
		newParameters.runnable = this.runnable;
		newParameters.recordStatistics = this.recordStatistics;
		newParameters.fieldFiltering = this.fieldFiltering;
		newParameters.contextSensitiveRefinement = this.contextSensitiveRefinement;
		newParameters.strictThreadSensitivity = this.strictThreadSensitivity;
		newParameters.overapproximateStubDroid = this.overapproximateStubDroid;
		newParameters.xss = this.xss;
		newParameters.xmx = this.xmx;
		return newParameters;
	}

	public int getkLimit() {
		return this.kLimit;
	}

	public File getInputApkFile() {
		return this.inputApkFile;
	}

	public int getSteps() {
		return this.steps;
	}

	public File getOutputFile() {
		return this.outputFile;
	}

	public String getMode() {
		return this.mode;
	}

	public String getOutputFormat() {
		return this.outputFormat;
	}

	public boolean isSimpleInput() {
		return this.simpleInput;
	}

	public boolean isAndroidMode() {
		return this.androidMode;
	}

	public Reference getFrom() {
		return this.from;
	}

	public Reference getTo() {
		return this.to;
	}

	public boolean isDrawGraphs() {
		return this.drawGraphs;
	}

	public boolean isIncludeOrdinaryLibraryPackages() {
		return this.includeOrdinaryLibraryPackages;
	}

	public boolean isSliceOrdinaryLibraryPackages() {
		return this.sliceOrdinaryLibraryPackages;
	}

	public boolean isSign() {
		return this.sign;
	}

	public boolean isRecordStatistics() {
		return this.recordStatistics;
	}

	public boolean isIncomplete() {
		return this.incomplete;
	}

	public boolean isRunnable() {
		return this.runnable;
	}

	public boolean isFieldFiltering() {
		return this.fieldFiltering;
	}

	public boolean isContextSensitiveRefinement() {
		return this.contextSensitiveRefinement;
	}

	public boolean isStrictThreadSensitivity() {
		return this.strictThreadSensitivity;
	}

	public boolean isOverapproximateStubDroid() {
		return this.overapproximateStubDroid;
	}

	public void setkLimit(int kLimit) {
		this.kLimit = kLimit;
	}

	public void setInputApkFile(File inputApkFile) {
		this.inputApkFile = inputApkFile;
	}

	public void setSteps(int steps) {
		this.steps = steps;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setSimpleInput(boolean simpleInput) {
		this.simpleInput = simpleInput;
	}

	public void setAndroidMode(boolean androidMode) {
		this.androidMode = androidMode;
	}

	public void setFrom(Reference from) {
		this.from = from;
	}

	public void setTo(Reference to) {
		this.to = to;
	}

	public void setDrawGraphs(boolean drawGraphs) {
		this.drawGraphs = drawGraphs;
	}

	public void setIncludeOrdinaryLibraryPackages(boolean includeOrdinaryLibraryPackages) {
		this.includeOrdinaryLibraryPackages = includeOrdinaryLibraryPackages;
	}

	public void setSliceOrdinaryLibraryPackages(boolean sliceOrdinaryLibraryPackages) {
		this.sliceOrdinaryLibraryPackages = sliceOrdinaryLibraryPackages;
	}

	public void setSign(boolean sign) {
		this.sign = sign;
	}

	public void setRecordStatistics(boolean recordStatistics) {
		this.recordStatistics = recordStatistics;
	}

	public void setIncomplete(boolean incomplete) {
		this.incomplete = incomplete;
	}

	public void setRunnable(boolean runnable) {
		this.runnable = runnable;
	}

	public void setFieldFiltering(boolean fieldFiltering) {
		this.fieldFiltering = fieldFiltering;
	}

	public void setContextSensitiveRefinement(boolean contextSensitiveRefinement) {
		this.contextSensitiveRefinement = contextSensitiveRefinement;
	}

	public void setStrictThreadSensitivity(boolean strictThreadSensitivity) {
		this.strictThreadSensitivity = strictThreadSensitivity;
	}

	public void setOverapproximateStubDroid(boolean overapproximateStubDroid) {
		this.overapproximateStubDroid = overapproximateStubDroid;
	}

	public void setStackSize(int valueInMB) {
		this.xss = valueInMB;
	}

	public void setMaxMemory(int valueInMB) {
		this.xmx = valueInMB;
	}

	/**
	 * @return JVM stack size in MB
	 */
	public int getStackSize() {
		return this.xss;
	}

	/**
	 * @return JVM max memory in MB
	 */
	public int getMaxMemory() {
		return this.xmx;
	}

	public void setInputEdges(File aqlAnswerFile) {
		this.aqlAnswerFile = aqlAnswerFile;
	}

	public File getInputEdges() {
		return this.aqlAnswerFile;
	}
}