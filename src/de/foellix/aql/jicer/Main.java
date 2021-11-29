package de.foellix.aql.jicer;

import java.util.Arrays;

import javafx.application.Application;

public class Main {
	public static void main(String[] args) {
		if (Arrays.asList(args).contains("-gui")) {
			Application.launch(GUI.class, args);
		} else {
			final Jicer jicer = new Jicer(args);
			jicer.jice();
		}
	}
}