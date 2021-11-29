package de.foellix.aql.jicer.sdgslicer;

import java.util.HashSet;
import java.util.Set;

import soot.Unit;

public class GlobalVisit {
	private Set<Unit> contextSensitivity;
	private Set<Unit> visited;

	public GlobalVisit(Set<Unit> contextSensitivity) {
		this.contextSensitivity = contextSensitivity;
		this.visited = new HashSet<>();
	}

	public Set<Unit> getContextSensitivity() {
		return this.contextSensitivity;
	}

	public Set<Unit> getVisited() {
		return this.visited;
	}
}