package de.foellix.aql.jicer.sdgslicer;

import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.Edge;
import de.foellix.aql.jicer.statistics.Counter;
import soot.SootField;
import soot.Unit;
import soot.jimple.NopStmt;

public class SDGForwardSlicer extends SDGSlicer {
	public SDGForwardSlicer(DependenceGraph dg, Counter counter, Unit target) {
		this(dg, counter, target, null);
	}

	public SDGForwardSlicer(DependenceGraph dg, Counter counter, Unit target, Set<SootField> fieldsToConsider) {
		super(dg, counter, target, fieldsToConsider);
	}

	/**
	 * @param ignoreAlways
	 *            true: ignore if the ignoreEdgeType is contained, false: only ignore if the ignoreEdgeType is the only assigned type
	 */
	@Override
	protected void visit(Unit start, Set<Unit> visited, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways) {
		super.count();

		if (this.globalVisit != null && this.globalVisit.getVisited().contains(start)) {
			return;
		}

		final Unit originalCall = Data.getInstance().getReplacedNodesReplacementToOriginal().get(start);
		if (Data.getInstance().getCallToReturnNodes().containsKey(originalCall)) {
			// Add context-sensitivity due to return node
			super.getContextSensitivelyAllowed().add(Data.getInstance().getCallToReturnNodes().get(originalCall));
		}

		visited.add(start);
		if (this.dg.getSuccsOfAsSet(start) != null) {
			for (final Unit succ : this.dg.getSuccsOfAsSet(start)) {
				Edge temp = new Edge(start, succ);
				final Set<Integer> tempTypes = this.dg.getEdgeTypes(temp);
				if (tempTypes.contains(DependenceGraph.TYPE_SUMMARY)) {
					// Add context-sensitivity due to summary edge
					super.getContextSensitivelyAllowed().add(succ);
				}
				// Check ignored edge type
				if (!visited.contains(succ) && !ignoreEdge(tempTypes, ignoreEdgeTypes, ignoreAlways)) {
					// Check if field-label is not assigned OR field-label is considered OR anonymous class edge
					if (this.fieldsToConsider == null || Data.getInstance().getFieldEdgeLabel(temp) == null
							|| this.fieldsToConsider.contains(Data.getInstance().getFieldEdgeLabel(temp))
							|| Data.getInstance().isAnonymousClassEdge(temp)) {
						// Context-Sensitivity check
						if (!Data.getInstance().getReturnToCallNodes().containsKey(succ)
								|| getContextSensitivelyAllowed().contains(succ)) {
							if (tempTypes.contains(DependenceGraph.TYPE_FIELD_DATA)
									|| tempTypes.contains(DependenceGraph.TYPE_STATIC_FIELD_DATA)) {
								// Re-init context-sensitivity due to (static-)field edge
								init(succ);
							}
							try {
								if (!this.worklist.contains(succ)) {
									this.worklist.add(succ);
								}
							} catch (final StackOverflowError e) {
								Log.error("A StackOverflowError was thrown while slicing through: " + succ
										+ ". To avoid please increase the JavaVitualMachine's heap size (launch parameter \"-Xss\" - e.g.: \"-Xss2m\")");
								return;
							}
						}
					}
				}
				temp = null;
			}
		}
	}

	@Override
	protected void init(Unit start) {
		if (!(start instanceof NopStmt)) {
			final Set<Unit> replacedCalls = super.findMethodCalls(start);
			for (final Unit replacedCall : replacedCalls) {
				final Unit originalCall;
				if (Data.getInstance().getReplacedNodesReplacementToOriginal().containsKey(replacedCall)) {
					originalCall = Data.getInstance().getReplacedNodesReplacementToOriginal().get(replacedCall);
				} else {
					originalCall = replacedCall;
				}
				if (Data.getInstance().getCallToReturnNodes().containsKey(originalCall)) {
					super.getContextSensitivelyAllowed()
							.add(Data.getInstance().getCallToReturnNodes().get(originalCall));
				}
			}
		}
	}
}