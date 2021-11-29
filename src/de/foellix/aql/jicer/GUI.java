package de.foellix.aql.jicer;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.SootHelper;
import de.foellix.aql.jicer.config.Config;
import de.foellix.aql.ui.gui.FileChooserUIElement;
import de.foellix.aql.ui.gui.FontAwesome;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class GUI extends Application {
	public static boolean started = false;

	private static final int SPACING_TINY = 5;
	private static final int SPACING_SMALL = 10;
	private static final int SPACING_LARGE = 25;

	private Stage stage;
	private File lastApk;
	private soot.Scene ss;
	private RadioButton modeSlice, modeSliceOut, modeShow, ana, in, run, apk, none, jimple;
	private CheckBox sign, fff, csr, sts, os, stats, dg, si, eol, sol;
	private FileChooserUIElement sdkChooser, zipalignChooser, signerChooser, apkChooser, fromChooser, toChooser,
			inputEdgesChooser, outputChooser, outputRunCmd;
	private TextField defaultExcludes, kLimit, stackSize, maxMemory;
	private TitledPane properties;

	@Override
	public void start(Stage stage) throws Exception {
		started = true;
		this.stage = stage;

		// Parse parameters
		final List<String> argList = this.getParameters().getRaw();
		final String[] args = new String[argList.size()];
		for (int i = 0; i < argList.size(); i++) {
			args[i] = argList.get(i);
		}
		ParameterParser.parseArguments(CLIHelper.replaceNeedlesWithQuotes(args), false);

		// Init GUI
		stage.setTitle("Jicer");
		stage.getIcons()
				.add(new Image(new File("data/gui/images/icon_16.png").toURI().toString(), 16, 16, false, true));
		stage.getIcons()
				.add(new Image(new File("data/gui/images/icon_32.png").toURI().toString(), 32, 32, false, true));
		stage.getIcons()
				.add(new Image(new File("data/gui/images/icon_64.png").toURI().toString(), 64, 64, false, true));

		final VBox centerElement = new VBox(SPACING_SMALL);
		centerElement.setPadding(new Insets(SPACING_SMALL));

		// config.properties
		this.sdkChooser = new FileChooserUIElement(stage, "Select Android SDK Platforms");
		this.sdkChooser.setFolder();
		this.zipalignChooser = new FileChooserUIElement(stage, "Select zipalign executeable");
		this.zipalignChooser.getFilters().clear();
		this.zipalignChooser.getFilters().add(FileChooserUIElement.FILTER_ALL);
		this.zipalignChooser.setInitFilter(FileChooserUIElement.FILTER_ALL);
		this.signerChooser = new FileChooserUIElement(stage, "Select jarsigner executeable");
		this.signerChooser.getFilters().clear();
		this.signerChooser.getFilters().add(FileChooserUIElement.FILTER_ALL);
		this.signerChooser.setInitFilter(FileChooserUIElement.FILTER_ALL);
		this.defaultExcludes = new TextField();

		BorderPane tempBox = new BorderPane();
		VBox labelBox = new VBox(SPACING_TINY);
		Label temp = new Label("Android SDK Platforms:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		temp = new Label("ZipAlign Executable:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		temp = new Label("ApkSigner Executable:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		temp = new Label("Default Excludes:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		tempBox.setLeft(labelBox);
		VBox chooserBox = new VBox(SPACING_TINY);
		chooserBox.getChildren().addAll(this.sdkChooser, this.zipalignChooser, this.signerChooser,
				this.defaultExcludes);
		tempBox.setCenter(chooserBox);
		final BorderPane saveBtnBox = new BorderPane();
		final Button saveBtn = new Button(FontAwesome.ICON_SAVE + "  Save");
		FontAwesome.applyFontAwesome(saveBtn, "-fx-font-size: 13;");
		saveBtn.setPadding(new Insets(SPACING_SMALL));
		saveBtn.setOnAction(eh -> {
			setConfigProperties();
			Config.getInstance().save();
		});
		saveBtnBox.setPadding(new Insets(SPACING_TINY, 0, 0, 0));
		saveBtnBox.setRight(saveBtn);
		tempBox.setBottom(saveBtnBox);
		centerElement.getChildren().add(tempBox);
		this.properties = new TitledPane("config.properties", tempBox);

		// App: From & To
		this.apkChooser = new FileChooserUIElement(stage, "Select APK");
		this.apkChooser.getFilters().clear();
		this.apkChooser.getFilters().add(FileChooserUIElement.FILTER_APK);
		this.apkChooser.getFilters().add(FileChooserUIElement.FILTER_ALL);
		this.apkChooser.setInitFilter(FileChooserUIElement.FILTER_APK);
		this.fromChooser = new FileChooserUIElement(stage, "Select From criterion");
		this.fromChooser.getButton().setText(FontAwesome.ICON_ARROW_DOWN);
		this.fromChooser.getButton().setOnAction(eh -> fromToPicker(this.apkChooser, this.fromChooser.getTextField()));
		this.toChooser = new FileChooserUIElement(stage, "Select To criterion");
		this.toChooser.getButton().setText(FontAwesome.ICON_ARROW_UP);
		this.toChooser.getButton().setOnAction(eh -> fromToPicker(this.apkChooser, this.toChooser.getTextField()));

		tempBox = new BorderPane();
		labelBox = new VBox(SPACING_TINY);
		temp = new Label("Apk:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		temp = new Label("From:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		temp = new Label("To:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		labelBox.getChildren().add(temp);
		tempBox.setLeft(labelBox);
		chooserBox = new VBox(SPACING_TINY);
		chooserBox.getChildren().addAll(this.apkChooser, this.fromChooser, this.toChooser);
		tempBox.setCenter(chooserBox);
		centerElement.getChildren().addAll(tempBox, new Separator());

		// Mode
		final ToggleGroup mode = new ToggleGroup();
		this.modeSlice = new RadioButton("Slice");
		this.modeSlice.setToggleGroup(mode);
		this.modeSliceOut = new RadioButton("SliceOut");
		this.modeSliceOut.setToggleGroup(mode);
		this.modeShow = new RadioButton("Show");
		this.modeShow.setToggleGroup(mode);

		// Features & Options
		final GridPane featuresBox = new GridPane();
		featuresBox.setHgap(SPACING_LARGE);
		featuresBox.setVgap(SPACING_TINY);
		final ColumnConstraints cc = new ColumnConstraints();
		cc.setHgrow(Priority.ALWAYS);
		featuresBox.getColumnConstraints().addAll(cc, cc, cc, cc, cc, cc);

		final ToggleGroup granularity = new ToggleGroup();
		this.ana = new RadioButton("Analyzable Slicing");
		this.ana.setTooltip(new Tooltip(
				"Default granularity - not the largest nor the smalles slice. Analyzable but not executeable."));
		this.in = new RadioButton("Incomplete/Debuggable Slicing");
		this.in.setTooltip(new Tooltip("Compute smallest slice. Not analyzable or runnable."));
		this.run = new RadioButton("Runnable Slicing");
		this.run.setTooltip(new Tooltip("Compute largest slice. Analyzable and runnable."));
		granularity.getToggles().addAll(this.ana, this.in, this.run);

		final ToggleGroup format = new ToggleGroup();
		this.apk = new RadioButton("APK");
		this.jimple = new RadioButton("Jimple");
		this.none = new RadioButton("None");
		format.getToggles().addAll(this.apk, this.jimple, this.none);

		// Backup and reset not implemented since not needed here
		// backup = new CheckBox("Backup AQL-System data");
		// reset = new CheckBox("Reset AQL-System data");
		this.si = new CheckBox("Simple Input");
		this.si.setTooltip(new Tooltip(
				"Never required if criteria are selected via this GUI (see documentation for more information)."));
		this.dg = new CheckBox("Draw ADG");
		this.dg.setTooltip(new Tooltip(
				"The ADG will be output as SVG file (Not recommended to use for large apps - takes very long and may cause outOfMemoryExceptions)."));
		this.eol = new CheckBox("excludeOrdinaryLibraries");
		this.eol.setTooltip(new Tooltip(
				"Do not extract libraries such as the \"android.support.*\" library (customizable - see documentation and config.properties)."));
		this.sol = new CheckBox("sliceOrdinaryLibraries");
		this.sol.setTooltip(new Tooltip(
				"Slice through libraries such as the \"android.support.*\" library (customizable - see documentation and config.properties)."));
		this.sign = new CheckBox("Sign APK");
		this.sign.setTooltip(new Tooltip("Sign the output APK"));
		this.fff = new CheckBox("Forward Field Filtering (FFF)");
		this.fff.setTooltip(new Tooltip(
				"Refine forward slice by Forward Field Filtering (FFF - see documentation for more information)."));
		this.csr = new CheckBox("Context-Sensitive Refinement (CSR)");
		this.csr.setTooltip(new Tooltip(
				"Refine backward slice by Context-Sensitive Refinement (CSR - see documentation for more information)."));
		this.sts = new CheckBox("Strict Thread-Sensitivity (STS)");
		this.sts.setTooltip(
				new Tooltip("When not active local data is preferred (see documentation for more information)."));
		this.os = new CheckBox("Overapproximate StubDroid Summaries (OS)");
		this.os.setTooltip(new Tooltip(
				"When not active no summary means no reassignment (see documentation for more information)."));
		this.stats = new CheckBox("Statistics");
		this.stats.setTooltip(new Tooltip("Statistics are recorded and written to statistics.csv."));

		this.jimple.setOnAction(eh -> {
			if (this.jimple.isSelected()) {
				this.sign.setDisable(true);
				this.sign.setSelected(false);
			}
		});
		this.none.setOnAction(eh -> {
			if (this.none.isSelected()) {
				this.sign.setDisable(true);
				this.sign.setSelected(false);
			}
		});
		this.apk.setOnAction(eh -> {
			if (this.apk.isSelected() && this.run.isSelected()) {
				this.sign.setDisable(false);
			}
		});
		this.ana.setOnAction(eh -> {
			if (this.ana.isSelected()) {
				this.sign.setDisable(true);
				this.sign.setSelected(false);
			}
		});
		this.in.setOnAction(eh -> {
			if (this.in.isSelected()) {
				this.sign.setDisable(true);
				this.sign.setSelected(false);
			}
		});
		this.run.setOnAction(eh -> {
			if (this.run.isSelected() && this.apk.isSelected()) {
				this.sign.setDisable(false);
			} else {
				this.sign.setDisable(true);
				this.sign.setSelected(false);
			}
		});
		if (!this.run.isSelected()) {
			this.sign.setDisable(true);
			this.sign.setSelected(false);
		}

		// Analysis & JVM
		final HBox kLimitBox = new HBox(SPACING_TINY);
		this.kLimit = new TextField();
		this.kLimit.setMaxWidth(60);
		this.kLimit.setPadding(new Insets(0));
		this.kLimit.setTooltip(
				new Tooltip("Reaching Definition Analysis Option (see documentation for more information)."));
		kLimitBox.getChildren().addAll(this.kLimit, new Label("K-Limit"));

		final HBox stackSizeBox = new HBox(SPACING_TINY);
		this.stackSize = new TextField();
		this.stackSize.setMaxWidth(60);
		this.stackSize.setPadding(new Insets(0));
		this.stackSize.setTooltip(new Tooltip(
				"JVM Option (will be ignored when clicken \"Run\"; see documentation for more information)."));
		stackSizeBox.getChildren().addAll(this.stackSize, new Label("JVM Stack Size (MB)"));

		final HBox maxMemoryBox = new HBox(SPACING_TINY);
		this.maxMemory = new TextField();
		this.maxMemory.setMaxWidth(60);
		this.maxMemory.setPadding(new Insets(0));
		this.maxMemory.setTooltip(new Tooltip(
				"JVM Option (will be ignored when clicken \"Run\"; see documentation for more information)."));
		maxMemoryBox.getChildren().addAll(this.maxMemory, new Label("JVM Max. Memory (GB)"));

		int col = 0;
		featuresBox.add(new Label("Slicing Mode:"), col, 0);
		featuresBox.add(this.modeSlice, col, 1);
		featuresBox.add(this.modeSliceOut, col, 2);
		featuresBox.add(this.modeShow, col, 3);

		col++;
		featuresBox.add(new Label("Output Format"), col, 0);
		featuresBox.add(this.apk, col, 1);
		featuresBox.add(this.jimple, col, 2);
		featuresBox.add(this.none, col, 3);

		col++;
		featuresBox.add(new Label("Slicing Granularity"), col, 0);
		featuresBox.add(this.ana, col, 1);
		featuresBox.add(this.in, col, 2);
		featuresBox.add(this.run, col, 3);
		featuresBox.add(this.sign, col, 4);

		col++;
		featuresBox.add(new Label("Slicing Features"), col, 0);
		featuresBox.add(this.fff, col, 1);
		featuresBox.add(this.csr, col, 2);
		featuresBox.add(this.sts, col, 3);
		featuresBox.add(this.os, col, 4);

		col++;
		featuresBox.add(new Label("Misc"), col, 0);
		featuresBox.add(this.stats, col, 1);
		featuresBox.add(this.dg, col, 2);
		featuresBox.add(this.si, col, 3);

		col++;
		featuresBox.add(new Label("Ordinary Libraries"), col, 0);
		featuresBox.add(this.eol, col, 1);
		featuresBox.add(this.sol, col, 2);

		col++;
		featuresBox.add(new Label("Extra"), col, 0);
		featuresBox.add(kLimitBox, col, 1);
		featuresBox.add(maxMemoryBox, col, 2);
		featuresBox.add(stackSizeBox, col, 3);

		centerElement.getChildren().add(featuresBox);

		// Input edges
		this.inputEdgesChooser = new FileChooserUIElement(stage, "Select AQL-Answer to be used as input edges");
		this.inputEdgesChooser.getTextField().setPromptText("Optional");
		this.inputEdgesChooser.getFilters().clear();
		this.inputEdgesChooser.getFilters().add(FileChooserUIElement.FILTER_XML);
		this.inputEdgesChooser.getFilters().add(FileChooserUIElement.FILTER_ALL);
		this.inputEdgesChooser.setInitFilter(FileChooserUIElement.FILTER_XML);

		tempBox = new BorderPane();
		temp = new Label("Input Edges:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		tempBox.setLeft(temp);
		tempBox.setCenter(this.inputEdgesChooser);
		centerElement.getChildren().addAll(tempBox);

		// Output file
		this.outputChooser = new FileChooserUIElement(stage, "Select output APK file.");
		this.outputChooser.setSave();
		this.outputChooser.getTextField().setPromptText("Optional (Default: same APK filename in current directory)");
		this.outputChooser.getFilters().clear();
		this.outputChooser.getFilters().add(FileChooserUIElement.FILTER_APK);
		this.outputChooser.getFilters().add(FileChooserUIElement.FILTER_ALL);
		this.outputChooser.setInitFilter(FileChooserUIElement.FILTER_APK);

		tempBox = new BorderPane();
		temp = new Label("Output APK:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		tempBox.setLeft(temp);
		tempBox.setCenter(this.outputChooser);
		centerElement.getChildren().addAll(tempBox, new Separator());

		// Output
		tempBox = new BorderPane();
		this.outputRunCmd = new FileChooserUIElement(stage,
				"Run this command to execute Jicer as currently configured!");
		this.outputRunCmd.getTextField().setEditable(false);
		this.outputRunCmd.getTextField().setPromptText("Click to generate");
		this.outputRunCmd.getTextField().setOnMousePressed(eh -> getRunCmd());
		this.outputRunCmd.getButton().setText(FontAwesome.ICON_COPY);
		this.outputRunCmd.getButton().setOnAction(eh -> {
			final ClipboardContent content = new ClipboardContent();
			content.putString(getRunCmd());
			Clipboard.getSystemClipboard().setContent(content);
		});
		tempBox.setCenter(this.outputRunCmd);
		temp = new Label("Run-Command:");
		temp.setPadding(new Insets(4, 5, 4, 0));
		tempBox.setLeft(temp);
		centerElement.getChildren().add(tempBox);

		// Buttons
		tempBox = new BorderPane();
		tempBox.setPadding(new Insets(SPACING_SMALL));
		final HBox buttonBox = new HBox(SPACING_SMALL);
		final Button runBtn = new Button(FontAwesome.ICON_PLAY + "  Run");
		FontAwesome.applyFontAwesome(FontAwesome.getInstance().setGreen(runBtn), "-fx-font-size: 13;");
		runBtn.setPadding(new Insets(SPACING_SMALL));
		runBtn.setOnAction(eh -> run());
		final Button cancelBtn = new Button("Cancel");
		FontAwesome.applyFontAwesome(cancelBtn, "-fx-font-size: 13;");
		cancelBtn.setPadding(new Insets(SPACING_SMALL));
		cancelBtn.setOnAction(eh -> {
			stage.hide();
		});
		buttonBox.getChildren().addAll(cancelBtn, runBtn);
		tempBox.setRight(buttonBox);
		final Button resetBtn = new Button(FontAwesome.ICON_REFRESH + " Reload Defaults");
		FontAwesome.applyFontAwesome(resetBtn, "-fx-font-size: 13;");
		resetBtn.setPadding(new Insets(SPACING_SMALL));
		resetBtn.setOnAction(eh -> loadDefaultOptions());
		tempBox.setLeft(resetBtn);

		// Load defaults
		loadDefaultOptions();
		getRunCmd();

		// Complete and Show
		final BorderPane pane = new BorderPane(centerElement);
		pane.setBottom(tempBox);
		pane.setTop(this.properties);
		final Scene scene = new Scene(pane);
		scene.getStylesheets().add(new File("data/gui/style.css").toURI().toString());
		stage.setMinWidth(1214);
		stage.setMinHeight(644);
		stage.setScene(scene);
		stage.show();

		// Auto-refresh
		new Thread(() -> {
			try {
				while (stage.isShowing()) {
					Thread.sleep(500);
					getRunCmd();
					Platform.runLater(() -> {
						if (this.apkChooser.getTextField().getText().isBlank()) {
							stage.setTitle("Jicer");
						} else {
							stage.setTitle("Jicer [" + this.apkChooser.getTextField().getText() + "]");
						}
					});
				}
			} catch (final InterruptedException e) {
				return;
			}
		}).start();
	}

	private void setConfigProperties() {
		Config.getInstance().platformsPath = this.sdkChooser.getTextField().getText();
		Config.getInstance().zipalignPath = this.zipalignChooser.getTextField().getText();
		Config.getInstance().apksignerPath = this.signerChooser.getTextField().getText();
		Config.getInstance().defaultExcludes = this.defaultExcludes.getText();
	}

	private String getRunCmd() {
		return getRunCmd(true);
	}

	private String getRunCmd(boolean escape) {
		final StringBuilder sb = new StringBuilder("java -Xss" + this.stackSize.getText() + "m -Xmx"
				+ this.maxMemory.getText() + "G -jar Jicer-" + Properties.info().VERSION + ".jar");
		if (this.modeShow.isSelected()) {
			sb.append(" -m " + de.foellix.aql.jicer.Parameters.MODE_SHOW_SLICE);
		} else if (this.modeSliceOut.isSelected()) {
			sb.append(" -m " + de.foellix.aql.jicer.Parameters.MODE_SLICE_OUT);
		} else {
			sb.append(" -m " + de.foellix.aql.jicer.Parameters.MODE_SLICE);
		}
		if (this.jimple.isSelected()) {
			sb.append(" -f " + de.foellix.aql.jicer.Parameters.OUTPUT_FORMAT_JIMPLE);
		} else if (this.none.isSelected()) {
			sb.append(" -f " + de.foellix.aql.jicer.Parameters.OUTPUT_FORMAT_NONE);
		}
		if (this.in.isSelected()) {
			sb.append(" -in");
		} else if (this.run.isSelected()) {
			sb.append(" -run");
			if (this.sign.isSelected()) {
				sb.append(" -sign");
			}
		}
		if (!this.fff.isSelected()) {
			sb.append(" -nff");
		}
		if (!this.csr.isSelected()) {
			sb.append(" -ncsr");
		}
		if (this.sts.isSelected()) {
			sb.append(" -sts");
		}
		if (this.os.isSelected()) {
			sb.append(" -os");
		}
		if (!this.stats.isSelected()) {
			sb.append(" -ns");
		}
		if (this.dg.isSelected()) {
			sb.append(" -dg");
		}
		if (this.si.isSelected()) {
			sb.append(" -si");
		}
		if (this.eol.isSelected()) {
			sb.append(" -eol");
		}
		if (this.sol.isSelected()) {
			sb.append(" -sol");
		}
		if (Integer.valueOf(this.kLimit.getText())
				.intValue() != de.foellix.aql.jicer.Parameters.DEFAULT_K_LIMIT_FOR_ANALYSIS) {
			sb.append(" -k " + this.kLimit.getText());
		}
		if (this.inputEdgesChooser.getTextField().getText() != null
				&& !this.inputEdgesChooser.getTextField().getText().isBlank()) {
			sb.append(" -ie \"" + new File(this.inputEdgesChooser.getTextField().getText()).getAbsolutePath() + "\"");
		}
		if (this.outputChooser.getTextField().getText() != null
				&& !this.outputChooser.getTextField().getText().isBlank()) {
			sb.append(" -o \"" + new File(this.outputChooser.getTextField().getText()).getAbsolutePath() + "\"");
		}
		if (!this.fromChooser.getTextField().getText().isBlank()) {
			String from = this.fromChooser.getTextField().getText();
			if (escape) {
				from = CLIHelper.escapeChars(from);
			}
			sb.append(" -from \"" + CLIHelper.replaceQuotesWithNeedles(from) + "\"");
		}
		if (!this.toChooser.getTextField().getText().isBlank()) {
			String to = this.toChooser.getTextField().getText();
			if (escape) {
				to = CLIHelper.escapeChars(to);
			}
			sb.append(" -to \"" + CLIHelper.replaceQuotesWithNeedles(to) + "\"");
		}

		final String runCmd = sb.toString();
		Platform.runLater(() -> this.outputRunCmd.getTextField().setText(runCmd));
		return runCmd;
	}

	private void run() {
		setConfigProperties();
		String argsStr = getRunCmd(false);
		argsStr = argsStr.substring(argsStr.indexOf(".jar") + 5);
		this.stage.hide();

		final String[] args = Helper.getRunCommandAsArray(argsStr);
		new Jicer(args).jice();
	}

	private void loadDefaultOptions() {
		// config.properties
		if (new File("config.properties").exists()) {
			this.properties.setExpanded(false);
		} else {
			this.properties.setExpanded(true);
		}
		if (Config.getInstance().platformsPath != null && !Config.getInstance().platformsPath.isBlank()) {
			this.sdkChooser.getTextField().setText(Config.getInstance().platformsPath);
		} else {
			this.sdkChooser.getTextField().clear();
		}
		if (Config.getInstance().zipalignPath != null && !Config.getInstance().zipalignPath.isBlank()) {
			this.zipalignChooser.getTextField().setText(Config.getInstance().zipalignPath);
		} else {
			this.zipalignChooser.getTextField().clear();
		}
		if (Config.getInstance().apksignerPath != null && !Config.getInstance().apksignerPath.isBlank()) {
			this.signerChooser.getTextField().setText(Config.getInstance().apksignerPath);
		} else {
			this.signerChooser.getTextField().clear();
		}
		if (Config.getInstance().defaultExcludes != null && !Config.getInstance().defaultExcludes.isBlank()) {
			this.defaultExcludes.setText(Config.getInstance().defaultExcludes);
		} else {
			final List<String> defaultExcludes = Arrays.asList(Config.DEFAULT_EXCLUDE_POSSIBILITIES);
			final StringBuilder sb = new StringBuilder();
			for (final String defaultExclude : defaultExcludes) {
				if (!sb.isEmpty()) {
					sb.append(", ");
				}
				sb.append(defaultExclude);
			}
			this.defaultExcludes.setText(sb.toString());
		}

		// Parameters
		if (de.foellix.aql.jicer.Parameters.getInstance().getInputApkFile() != null) {
			this.apkChooser.getTextField()
					.setText(de.foellix.aql.jicer.Parameters.getInstance().getInputApkFile().getAbsolutePath());
		} else {
			this.apkChooser.getTextField().clear();
		}
		final Reference from = de.foellix.aql.jicer.Parameters.getInstance().getFrom();
		if (from != null) {
			this.fromChooser.getTextField().setText(Helper.toString(from));
		} else {
			this.fromChooser.getTextField().clear();
		}
		final Reference to = de.foellix.aql.jicer.Parameters.getInstance().getTo();
		if (to != null) {
			this.toChooser.getTextField().setText(Helper.toString(to));
		} else {
			this.toChooser.getTextField().clear();
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().getMode() == de.foellix.aql.jicer.Parameters.MODE_SLICE
				|| de.foellix.aql.jicer.Parameters.getInstance().getMode() == null) {
			this.modeSlice.setSelected(true);
		} else {
			this.modeSlice.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().getMode() == de.foellix.aql.jicer.Parameters.MODE_SLICE_OUT) {
			this.modeSliceOut.setSelected(true);
		} else {
			this.modeSliceOut.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance()
				.getMode() == de.foellix.aql.jicer.Parameters.MODE_SHOW_SLICE) {
			this.modeShow.setSelected(true);
		} else {
			this.modeShow.setSelected(false);
		}
		if (!de.foellix.aql.jicer.Parameters.getInstance().isIncomplete()
				&& !de.foellix.aql.jicer.Parameters.getInstance().isRunnable()) {
			this.ana.setSelected(true);
		} else {
			this.ana.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isIncomplete()) {
			this.in.setSelected(true);
		} else {
			this.in.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isRunnable()) {
			this.run.setSelected(true);
		} else {
			this.run.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance()
				.getOutputFormat() == de.foellix.aql.jicer.Parameters.OUTPUT_FORMAT_APK) {
			this.apk.setSelected(true);
		} else {
			this.apk.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance()
				.getOutputFormat() == de.foellix.aql.jicer.Parameters.OUTPUT_FORMAT_JIMPLE) {
			this.jimple.setSelected(true);
		} else {
			this.jimple.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance()
				.getOutputFormat() == de.foellix.aql.jicer.Parameters.OUTPUT_FORMAT_NONE) {
			this.none.setSelected(true);
		} else {
			this.none.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isSimpleInput()) {
			this.si.setSelected(true);
		} else {
			this.si.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isDrawGraphs()) {
			this.dg.setSelected(true);
		} else {
			this.dg.setSelected(false);
		}
		if (!de.foellix.aql.jicer.Parameters.getInstance().isIncludeOrdinaryLibraryPackages()) {
			this.eol.setSelected(true);
		} else {
			this.eol.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isSliceOrdinaryLibraryPackages()) {
			this.sol.setSelected(true);
		} else {
			this.sol.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isSign()) {
			this.sign.setSelected(true);
		} else {
			this.sign.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isFieldFiltering()) {
			this.fff.setSelected(true);
		} else {
			this.fff.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isContextSensitiveRefinement()) {
			this.csr.setSelected(true);
		} else {
			this.csr.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isStrictThreadSensitivity()) {
			this.sts.setSelected(true);
		} else {
			this.sts.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isOverapproximateStubDroid()) {
			this.os.setSelected(true);
		} else {
			this.os.setSelected(false);
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().isRecordStatistics()) {
			this.stats.setSelected(true);
		} else {
			this.stats.setSelected(false);
		}
		if (this.run.isSelected() && this.apk.isSelected()) {
			this.sign.setDisable(false);
		} else if (!this.run.isSelected() || !this.apk.isSelected()) {
			this.sign.setDisable(true);
		}
		this.kLimit.setText(String.valueOf(de.foellix.aql.jicer.Parameters.getInstance().getkLimit()));
		this.maxMemory.setText(String.valueOf(de.foellix.aql.jicer.Parameters.getInstance().getMaxMemory() / 1000));
		this.stackSize.setText(String.valueOf(de.foellix.aql.jicer.Parameters.getInstance().getStackSize()));
		if (de.foellix.aql.jicer.Parameters.getInstance().getInputEdges() != null) {
			this.inputEdgesChooser.getTextField()
					.setText(de.foellix.aql.jicer.Parameters.getInstance().getInputEdges().getAbsolutePath());
		} else {
			this.inputEdgesChooser.getTextField().clear();
		}
		if (de.foellix.aql.jicer.Parameters.getInstance().getOutputFile() != null) {
			this.outputChooser.getTextField()
					.setText(de.foellix.aql.jicer.Parameters.getInstance().getOutputFile().getAbsolutePath());
		} else {
			this.outputChooser.getTextField().clear();
		}
		this.outputRunCmd.getTextField().clear();
	}

	private void fromToPicker(FileChooserUIElement apkChooser, TextField textField) {
		final File apk = new File(apkChooser.getTextField().getText());
		if (apk.exists()) {
			final Dialog<String> alert = new Dialog<>();
			final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons()
					.add(new Image(new File("data/gui/images/icon_16.png").toURI().toString(), 16, 16, false, true));
			alertStage.getIcons()
					.add(new Image(new File("data/gui/images/icon_32.png").toURI().toString(), 32, 32, false, true));
			alertStage.getIcons()
					.add(new Image(new File("data/gui/images/icon_64.png").toURI().toString(), 64, 64, false, true));
			alertStage.setResizable(true);
			alertStage.setMinWidth(800);
			alertStage.setWidth(800);
			alertStage.setMinHeight(600);
			alertStage.setHeight(600);
			alertStage.getScene().getStylesheets().add(new File("data/gui/style.css").toURI().toString());
			alert.setGraphic(new ImageView(new Image(new File("data/gui/images/icon_64.png").toURI().toString())));
			alert.setTitle("Select Criterion");
			alert.setHeaderText("Loading...");
			alert.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
			final VBox box = new VBox(SPACING_SMALL);

			final Map<TreeItem<SootObject>, ObservableList<TreeItem<SootObject>>> unitMap = new HashMap<>();
			final Map<TreeItem<SootObject>, TreeItem<SootObject>> parentMap = new HashMap<>();
			final TreeItem<SootObject> app = new TreeItem<>(new SootObject(apk.getAbsolutePath()));
			app.setExpanded(true);
			final TreeView<SootObject> tree = new TreeView<>(app);
			tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<SootObject>>() {
				@Override
				public void changed(ObservableValue<? extends TreeItem<SootObject>> observable,
						TreeItem<SootObject> oldValue, TreeItem<SootObject> newValue) {
					if (newValue != null && newValue.getValue().isStatement()) {
						alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
					} else {
						alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
					}
				}
			});

			new Thread(() -> {
				if (GUI.this.lastApk == null || !GUI.this.lastApk.getAbsolutePath().equals(apk.getAbsolutePath())) {
					GUI.this.ss = SootHelper.getScene(apk, new File(Config.getInstance().platformsPath));
					GUI.this.lastApk = apk;
				}
				for (final SootClass sc : GUI.this.ss.getApplicationClasses()) {
					// Classes
					if (sc.isConcrete()) {
						final TreeItem<SootObject> scItem = new TreeItem<>(new SootObject(sc));
						app.getChildren().add(scItem);
						for (final SootMethod sm : sc.getMethods()) {
							// Methods
							if (sm.isConcrete()) {
								final Body b = sm.retrieveActiveBody();
								if (b != null) {
									final TreeItem<SootObject> smItem = new TreeItem<>(new SootObject(sm));
									scItem.getChildren().add(smItem);
									final ObservableList<TreeItem<SootObject>> unitItems = FXCollections
											.observableArrayList();
									for (final Unit unit : b.getUnits()) {
										// Statements
										final TreeItem<SootObject> unitItem = new TreeItem<>(new SootObject(unit));
										unitItems.add(unitItem);
									}
									smItem.getChildren().addAll(unitItems);
									unitMap.put(smItem, unitItems);
									parentMap.put(smItem, scItem);
								}
							}
						}
					}
				}
				Platform.runLater(() -> alert.setHeaderText("Please select a statement from the list below."));
			}).start();

			final TextField filter = new TextField();
			filter.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					for (final TreeItem<SootObject> parent : unitMap.keySet()) {
						parent.getChildren().clear();
						parent.getChildren().addAll(new FilteredList<>(unitMap.get(parent), item -> {
							return item.getValue().toString().toLowerCase().contains(filter.getText().toLowerCase());
						}));

						final TreeItem<SootObject> methodParent = parentMap.get(parent);
						if (parent.getChildren().isEmpty() && methodParent.getChildren().contains(parent)) {
							methodParent.getChildren().remove(parent);
						} else if (!parent.getChildren().isEmpty() && !methodParent.getChildren().contains(parent)) {
							methodParent.getChildren().add(parent);
						}

						if (methodParent.getChildren().isEmpty() && app.getChildren().contains(methodParent)) {
							app.getChildren().remove(methodParent);
						} else if (!methodParent.getChildren().isEmpty() && !app.getChildren().contains(methodParent)) {
							app.getChildren().add(methodParent);
						}
					}
				}
			});

			final BorderPane filterBox = new BorderPane(filter);
			final Label searchLabel = new Label("Search:");
			searchLabel.setPadding(new Insets(5, 5, 5, 0));
			filterBox.setLeft(searchLabel);
			box.getChildren().addAll(filterBox, tree);
			alert.getDialogPane().setContent(box);
			alert.setResultConverter(dialogButton -> {
				if (dialogButton == ButtonType.OK && tree.getSelectionModel().getSelectedItem() != null) {
					try {
						return "Statement('" + tree.getSelectionModel().getSelectedItem().getValue().toString()
								+ "')->Method('"
								+ tree.getSelectionModel().getSelectedItem().getParent().getValue().toString()
								+ "')->Class('"
								+ tree.getSelectionModel().getSelectedItem().getParent().getParent().getValue()
										.toString()
								+ "')->App('" + tree.getSelectionModel().getSelectedItem().getParent().getParent()
										.getParent().getValue().toString()
								+ "')";
					} catch (final NullPointerException e) {
						return null;
					}
				}
				return null;
			});

			final Optional<String> result = alert.showAndWait();
			if (result.isPresent()) {
				textField.setText(result.get());
			} else {
				Log.msg("Please select a statment.", Log.NORMAL);
			}
		} else {
			Log.msg("Please select an existing APK first.", Log.NORMAL);
		}
	}

	private class SootObject {
		private Object object;

		public SootObject(Object object) {
			this.object = object;
		}

		public boolean isStatement() {
			return this.object instanceof Unit;
		}

		@Override
		public String toString() {
			return this.object.toString();
		}
	}
}