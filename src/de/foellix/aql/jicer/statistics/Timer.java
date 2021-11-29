package de.foellix.aql.jicer.statistics;

import de.foellix.aql.Log;

public class Timer {
	private String title;
	private long start;
	private long time;
	private boolean running;

	public Timer(String title) {
		this.title = title;
		this.time = 0;
		this.running = false;
	}

	public synchronized void start() {
		if (!this.running) {
			this.start = System.currentTimeMillis();
			this.running = true;
		} else {
			Log.msg("Timer (" + this.title + ") already running!", Log.DEBUG);
		}
	}

	public synchronized void stop() {
		if (this.running) {
			this.time += (System.currentTimeMillis() - this.start);
			this.running = false;
		} else {
			Log.msg("Timer (" + this.title + ") was not started, yet!", Log.DEBUG);
		}
	}

	public synchronized void setTime(long timeInMS) {
		this.time = timeInMS;
	}

	public synchronized long getTime() {
		return this.time;
	}

	public synchronized String getTimeAsString() {
		long rest = this.time;
		final long ms = rest % 1000;
		rest = rest / 1000;
		final long s = rest % 60;
		rest = rest / 60;
		final long m = rest % 60;
		rest = rest / 60;
		final long h = rest;
		return (h > 0 ? h + ":" : "") + (m > 0 ? digits(m, 2) + ":" + digits(s, 2) : s) + "." + digits(ms, 3);
	}

	private String digits(long input, int num) {
		String output = String.valueOf(input).toString();
		while (output.length() < num) {
			output = "0" + output;
		}
		return output;
	}
}