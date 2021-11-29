package de.foellix.aql.jicer.sdgslicer;

import java.util.HashSet;
import java.util.Set;

import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.Edge;
import soot.SootField;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.FieldRef;

public class FieldCollector {
	protected DependenceGraph dg;
	protected Unit target;
	protected Set<SootField> fieldsToConsider;

	public FieldCollector(DependenceGraph dg, Unit target) {
		this.dg = dg;
		this.target = target;
		this.fieldsToConsider = new HashSet<>();
	}

	public Set<SootField> collectFields() {
		return collectFields(this.target);
	}

	private Set<SootField> collectFields(Unit unit) {
		// (Remember: !initial -> fieldsToConsider is subset of fieldsWhileCollecting regarding the current target)
		addConsideredFields(unit);
		collectFieldsForward(unit, new HashSet<>());
		collectFieldsBackward(unit, new HashSet<>());
		return this.fieldsToConsider;
	}

	private void collectFieldsForward(Unit unit, Set<Unit> visited) {
		visited.add(unit);

		if (this.dg.getPredsOfAsSet(unit) != null) {
			for (final Unit succ : this.dg.getSuccsOfAsSet(unit)) {
				Edge temp = new Edge(unit, succ);
				if (!visited.contains(succ) && (this.dg.getEdgeTypes(temp).size() > 1
						|| !this.dg.getEdgeTypes(temp).contains(DependenceGraph.TYPE_CONTROL))) {
					addConsideredFields(succ);
					collectFieldsForward(succ, visited);
				}
				temp = null;
			}
		}
	}

	private void collectFieldsBackward(Unit unit, Set<Unit> visited) {
		visited.add(unit);

		if (this.dg.getPredsOfAsSet(unit) != null) {
			for (final Unit pred : this.dg.getPredsOfAsSet(unit)) {
				Edge temp = new Edge(pred, unit);
				if (!visited.contains(pred) && (this.dg.getEdgeTypes(temp).size() > 1
						|| !this.dg.getEdgeTypes(temp).contains(DependenceGraph.TYPE_CONTROL))) {
					addConsideredFields(pred);
					collectFieldsBackward(pred, visited);
				}
				temp = null;
			}
		}
	}

	private void addConsideredFields(Unit unit) {
		for (final ValueBox box : unit.getUseAndDefBoxes()) {
			if (box.getValue() instanceof FieldRef) {
				// FieldRef
				final SootField field = ((FieldRef) box.getValue()).getField();
				this.fieldsToConsider.add(field);

				// Remember: Alias not required? They would destroy running example!
			}
		}
	}
}