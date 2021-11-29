package de.foellix.aql.jicer.sdgslicer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.soot.SootHelper;
import de.foellix.aql.jicer.statistics.Counter;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;

public abstract class SDGSlicer {
	protected Queue<Unit> worklist;
	protected DependenceGraph dg;
	protected Unit target;
	protected Set<SootField> fieldsToConsider;
	protected Set<Unit> csVisited;
	public GlobalVisit globalVisit;
	protected Counter counter;

	private Set<Unit> visited1;
	private Set<Unit> visited2;
	private Set<Unit> visited3;
	private Set<Unit> contextSensitivelyAllowed;
	private Set<Integer> ignoredTypes;

	public SDGSlicer(DependenceGraph dg, Counter counter, Unit target, Set<SootField> fieldsToConsider) {
		this.worklist = new LinkedList<>();
		this.dg = dg;
		this.target = target;
		this.fieldsToConsider = null;
		this.csVisited = new HashSet<>();
		this.counter = counter;

		this.visited1 = new HashSet<>();
		this.visited2 = new HashSet<>();
		this.visited3 = new HashSet<>();
		this.contextSensitivelyAllowed = new HashSet<>();
		this.ignoredTypes = new HashSet<>();

		// Collect fields
		if (fieldsToConsider != null && !fieldsToConsider.isEmpty()) {
			this.fieldsToConsider = fieldsToConsider;
			if (this.fieldsToConsider.size() > 10 && !Log.logIt(Log.VERBOSE)) {
				Log.msg("Fields which will be considered while slicing:\n" + this.fieldsToConsider.size()
						+ " fields considered!\n(shortened due to log-level. All fields will be logged on \"verbose\" log-level.)",
						Log.NORMAL);
			} else {
				Log.msg("Fields which will be considered while slicing:\n" + this.fieldsToConsider, Log.NORMAL);
			}
		}

		// Initialize context-sensitivity
		init(target);

		// Register
		this.globalVisit = new GlobalVisit(this.contextSensitivelyAllowed);
	}

	public Set<Unit> slice() {
		// Slice
		final Set<Unit> slice = sliceDirected(this.target);
		this.globalVisit.getVisited().addAll(slice);
		this.globalVisit.getContextSensitivity().addAll(this.contextSensitivelyAllowed);

		return slice;
	}

	private Set<Unit> sliceDirected(Unit target) {
		int before = -1;
		do {
			if (before != -1) {
				Log.msg("Restarting due to changed context-sensitivity.", Log.DEBUG_DETAILED);
				this.visited1.clear();
				this.visited2.clear();
			}
			before = this.contextSensitivelyAllowed.size();
			// Phase 1
			Set<Integer> ignoreTypes = new HashSet<>(this.ignoredTypes);
			ignoreTypes.add(DependenceGraph.TYPE_PARAMETER_OUT);
			this.worklist.add(target);
			Log.msg("Phase I", Log.DEBUG_DETAILED);
			visit(this.visited1, ignoreTypes);
			Log.msg("done", Log.DEBUG_DETAILED);

			if (before == this.contextSensitivelyAllowed.size()) {
				// Phase 2
				ignoreTypes = new HashSet<>(this.ignoredTypes);
				ignoreTypes.add(DependenceGraph.TYPE_PARAMETER_IN);
				// Call edges cannot simply be ignored here since they must be traversed
				// if a function is reached via e.g. field edges before.
				// Instead context-sensitivity is tracked.
				this.worklist.addAll(this.visited1);
				Log.msg("Phase II", Log.DEBUG_DETAILED);
				visit(this.visited2, ignoreTypes);
				Log.msg("done", Log.DEBUG_DETAILED);
			}
		} while (before < this.contextSensitivelyAllowed.size());

		// Keep loops
		for (final Unit unit : this.dg) {
			if (unit instanceof GotoStmt) {
				final GotoStmt castedUnit = (GotoStmt) unit;
				if (this.visited1.contains(castedUnit.getTarget()) || this.visited2.contains(castedUnit.getTarget())) {
					this.visited3.add(unit);
				}
			}
		}

		// Slice
		final Set<Unit> unitsInSlice = new HashSet<>();
		unitsInSlice.addAll(this.visited1);
		unitsInSlice.addAll(this.visited2);
		unitsInSlice.addAll(this.visited3);
		return unitsInSlice;
	}

	protected abstract void init(Unit start);

	public void visit(Set<Unit> visited, Set<Integer> ignoreEdgeTypes) {
		while (!this.worklist.isEmpty()) {
			visit(this.worklist.poll(), visited, ignoreEdgeTypes, true);
		}
	}

	protected abstract void visit(Unit start, Set<Unit> visited, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways);

	protected boolean ignoreEdge(Set<Integer> edgeTypes, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways) {
		if (ignoreEdgeTypes.isEmpty()) {
			return false;
		} else {
			if (ignoreAlways) {
				for (final Integer type : edgeTypes) {
					if (ignoreEdgeTypes.contains(type)) {
						return true;
					}
				}
				return false;
			} else {
				for (final Integer type : edgeTypes) {
					if (!ignoreEdgeTypes.contains(type)) {
						return false;
					}
				}
				return true;
			}
		}
	}

	public Set<Integer> getIgnoreTypes() {
		return this.ignoredTypes;
	}

	protected Set<Unit> getContextSensitivelyAllowed() {
		return this.contextSensitivelyAllowed;
	}

	protected Set<Unit> findMethodCalls(Unit unit) {
		final Set<Unit> methodCalls = new HashSet<>();
		if (!this.csVisited.contains(unit)) {
			this.csVisited.add(unit);

			if (unit instanceof Stmt) {
				final Stmt castedUnit = (Stmt) unit;
				if (castedUnit.containsInvokeExpr()) {
					methodCalls.add(unit);
				}
			}
			Unit entryNode = getEntryNode(unit);
			if (entryNode == null) {
				entryNode = unit;
			}
			if (this.dg.getPredsOfAsSet(entryNode) != null) {
				methodCalls.addAll(this.dg.getPredsOfAsSet(entryNode));
			}

			final Set<Unit> methodsToAdd = new HashSet<>();
			for (final Unit call : methodCalls) {
				methodsToAdd.addAll(findMethodCalls(call));
			}
			methodCalls.addAll(methodsToAdd);
			for (final Unit methodCall : new HashSet<>(methodCalls)) {
				if (methodCall instanceof Stmt && !((Stmt) methodCall).containsInvokeExpr()) {
					methodCalls.remove(methodCall);
				}
			}
		}
		return methodCalls;
	}

	private Unit getEntryNode(Unit unit) {
		final SootMethod sm = SootHelper.getMethod(unit);
		if (sm != null) {
			final Unit firstUnitOfSM = sm.retrieveActiveBody().getUnits().getFirst();
			final Set<Unit> candidates = this.dg.getPredsOfAsSet(firstUnitOfSM);
			if (candidates != null) {
				for (final Unit candidate : candidates) {
					if (candidate instanceof NopStmt) {
						return candidate;
					}
				}
			}
		}
		return null;
	}

	protected void count() {
		this.counter.increase();
	}

	public Unit getTarget() {
		return this.target;
	}

	public GlobalVisit getGlobalVisit() {
		return this.globalVisit;
	}
}