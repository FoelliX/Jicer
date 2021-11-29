package de.foellix.aql.jicer.statistics;

public class Counter {
	private int counter;

	public Counter() {
		this.counter = 0;
	}

	public synchronized Counter increase() {
		this.counter++;
		return this;
	}

	public synchronized Counter increase(int amount) {
		this.counter += amount;
		return this;
	}

	public synchronized Counter decrease() {
		this.counter--;
		return this;
	}

	public synchronized Counter decrease(int amount) {
		this.counter -= amount;
		return this;
	}

	public synchronized int getCounter() {
		return this.counter;
	}
}
