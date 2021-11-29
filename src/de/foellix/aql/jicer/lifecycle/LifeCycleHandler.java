package de.foellix.aql.jicer.lifecycle;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.config.Config;
import soot.Printer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SootIntegrationMode;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;

public class LifeCycleHandler {
	private final File apkFile;
	private Collection<SootMethod> dummyMains;
	private SootClass dummyMainClass;

	public LifeCycleHandler(File apkFile) {
		this.apkFile = apkFile;
		this.dummyMains = null;
		this.dummyMainClass = null;
	}

	public Collection<SootMethod> getDummyMainMethods() {
		if (this.dummyMains != null) {
			return this.dummyMains;
		}

		// Construct call graph
		final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		config.getAnalysisFileConfig().setTargetAPKFile(this.apkFile.getAbsolutePath());
		config.getAnalysisFileConfig().setAndroidPlatformDir(Config.getInstance().platformsPath);
		config.setSootIntegrationMode(SootIntegrationMode.UseExistingInstance);
		config.setCodeEliminationMode(CodeEliminationMode.NoCodeElimination);
		config.setTaintAnalysisEnabled(false);
		final SetupApplication sa = new SetupApplication(config);
		Options.v().setPhaseOption("cg", "enabled:true");
		sa.constructCallgraph();

		// Get dummy main
		List<SootMethod> dummyMains;
		try {
			final SootMethod dummyMain = sa.getDummyMainMethod();
			this.dummyMainClass = dummyMain.getDeclaringClass();

			// Get dummy mains
			dummyMains = this.dummyMainClass.getMethods();

			// Output dummy main
			if (Log.logIt(Log.DEBUG)) {
				printDummyMainClass(this.dummyMainClass);
			}
		} catch (final NullPointerException e) {
			Log.error("Dummy main could not be created (by FlowDroid)!" + Log.getExceptionAppendix(e));
			dummyMains = new ArrayList<>();
		}

		this.dummyMains = dummyMains;
		return this.dummyMains;
	}

	private void printDummyMainClass(SootClass dummyMainClass) {
		String dummyMainAsString;
		try (StringWriter stringOut = new StringWriter()) {
			final PrintWriter writerOut = new PrintWriter(stringOut);
			Printer.v().printTo(dummyMainClass, writerOut);
			writerOut.flush();
			writerOut.close();
			stringOut.flush();
			dummyMainAsString = stringOut.toString();
		} catch (final IOException e) {
			dummyMainAsString = null;
			Log.error("Could not output dummy main as string!");
		}
		if (dummyMainAsString != null) {
			Log.msg("Dummy main (created by FlowDroid):\n" + dummyMainAsString, Log.DEBUG_DETAILED);
		}
	}

	public SootClass getDummyMainClass() {
		return this.dummyMainClass;
	}
}