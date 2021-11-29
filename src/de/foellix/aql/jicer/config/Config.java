package de.foellix.aql.jicer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.GUI;

public class Config {
	public static final String[] DEFAULT_EXCLUDE_POSSIBILITIES = new String[] { "android.support.", "android.arch.",
			"com.android.", "com.google.", "org.apache.", "org.mozilla.", "org.springframework." };

	private static final String PLATFORMS_PATH = "platformsPath";
	private static final String ZIPALIGN_PATH = "zipalignPath";
	private static final String APKSIGNER_PATH = "apksignerPath";
	private static final String DEFAULT_EXCLUDES = "defaultExcludes";

	public String platformsPath;
	public String zipalignPath;
	public String apksignerPath;
	public String defaultExcludes;

	private static Config instance = new Config();

	private Config() {
		load();
	}

	public static Config getInstance() {
		return instance;
	}

	void load() {
		final Properties prop = new Properties();
		try {
			final FileInputStream in = new FileInputStream("config.properties");
			prop.load(in);
			in.close();

			this.platformsPath = prop.getProperty(PLATFORMS_PATH);
			this.zipalignPath = prop.getProperty(ZIPALIGN_PATH);
			this.apksignerPath = prop.getProperty(APKSIGNER_PATH);
			this.defaultExcludes = prop.getProperty(DEFAULT_EXCLUDES);
		} catch (final Exception e0) {
			if (!GUI.started) {
				final Scanner sc = new Scanner(System.in);
				this.platformsPath = prop.getProperty(PLATFORMS_PATH);
				this.zipalignPath = prop.getProperty(ZIPALIGN_PATH);
				this.apksignerPath = prop.getProperty(APKSIGNER_PATH);
				this.defaultExcludes = prop.getProperty(DEFAULT_EXCLUDES);

				// Paths
				while (this.platformsPath == null || this.platformsPath.isEmpty()) {
					Log.msg("Please enter the path to your Android SDK platforms directory (e.g. \"/path/to/android/sdk/platforms\"): ",
							Log.NORMAL);
					this.platformsPath = sc.next();
				}
				while (this.zipalignPath == null || this.zipalignPath.isEmpty()) {
					Log.msg("Please enter the path to zipalign (e.g. \"/path/to/android/sdk/build-tools/29.0.2/zipalign.exe\" or \"/usr/bin/zipalign\" or \"zipalign\"): ",
							Log.NORMAL);
					this.zipalignPath = sc.next();
				}
				while (this.apksignerPath == null || this.apksignerPath.isEmpty()) {
					Log.msg("Please enter the path to apksigner (e.g. \"/path/to/android/sdk/build-tools/29.0.2/apksigner.bat\" or \"/usr/bin/apksigner\" or \"apksigner\"): ",
							Log.NORMAL);
					this.apksignerPath = sc.next();
				}

				// Default excludes
				if (this.defaultExcludes == null) {
					this.defaultExcludes = "";
					final List<String> defaultExcludes = Arrays.asList(DEFAULT_EXCLUDE_POSSIBILITIES);
					for (final String defaultExclude : defaultExcludes) {
						String answer = null;
						Log.msg("Do you want to exclude the the package(s) " + defaultExclude
								+ "* from slicing? (y/n - default: y)", Log.NORMAL);
						while (true) {
							answer = sc.next();
							if (answer.equalsIgnoreCase("y")) {
								if (this.defaultExcludes != null && !this.defaultExcludes.isEmpty()) {
									this.defaultExcludes += ", ";
								}
								this.defaultExcludes += defaultExclude;
								break;
							} else if (answer.equalsIgnoreCase("n")) {
								break;
							} else {
								Log.msg("Please type \"y\" for yes or \"n\" for no!", Log.NORMAL);
							}
						}
					}
					Log.msg("More excludes can be added in \"config.properties\"!", Log.NORMAL);
				}

				sc.close();

				File temp = new File(this.platformsPath);
				this.platformsPath = temp.getAbsolutePath();
				temp = new File(this.zipalignPath);
				this.zipalignPath = temp.getAbsolutePath();
				temp = new File(this.apksignerPath);
				this.apksignerPath = temp.getAbsolutePath();

				save();
			}
		}
	}

	public void save() {
		final Properties prop = new Properties();
		prop.setProperty(PLATFORMS_PATH, this.platformsPath);
		prop.setProperty(ZIPALIGN_PATH, this.zipalignPath);
		prop.setProperty(APKSIGNER_PATH, this.apksignerPath);
		prop.setProperty(DEFAULT_EXCLUDES, this.defaultExcludes);
		try {
			final FileOutputStream out = new FileOutputStream("config.properties");
			prop.store(out, "Jicer - Configuration file");
			out.close();
		} catch (final Exception e) {
			Log.error("Error while storing properties" + Log.getExceptionAppendix(e));
		}
	}
}
