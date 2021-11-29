package de.foellix.aql.jicer.dg;

import soot.Unit;

public class Edge {
	private Unit from;
	private Unit to;
	private boolean hashCodeReady;
	private int hashCode;
	private int type;

	public Edge(Unit from, Unit to) {
		this(from, to, -1);
	}

	public Edge(Unit from, Unit to, int type) {
		super();
		this.from = from;
		this.to = to;
		this.type = type;
		this.hashCodeReady = false;
		this.hashCode = 0;
	}

	public Unit getFrom() {
		return this.from;
	}

	public Unit getTo() {
		return this.to;
	}

	public void setFrom(Unit from) {
		this.hashCodeReady = false;
		this.from = from;
	}

	public void setTo(Unit to) {
		this.hashCodeReady = false;
		this.to = to;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return this.from.toString() + " -> " + this.to.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		if (!this.hashCodeReady) {
			final String hashString = String.valueOf(this.from.hashCode()) + String.valueOf(this.to.hashCode());
			this.hashCode = hashString.hashCode();
			this.hashCodeReady = true;
		}
		return this.hashCode;
	}
}