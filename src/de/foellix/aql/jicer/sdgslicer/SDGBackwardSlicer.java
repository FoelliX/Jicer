package de.foellix.aql.jicer.sdgslicer;

import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.Edge;
import de.foellix.aql.jicer.statistics.Counter;
import soot.SootField;
import soot.Unit;

public class SDGBackwardSlicer extends SDGSlicer {
	public SDGBackwardSlicer(DependenceGraph dg, Counter counter, Unit target) {
		this(dg, counter, target, null);
	}

	public SDGBackwardSlicer(DependenceGraph dg, Counter counter, Unit target, Set<SootField> fieldsToConsider) {
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

		if (Data.getInstance().getReturnToCallNodes().containsKey(start)) {
			// Add context-sensitivity due to return node
			final Unit originalCall = Data.getInstance().getReturnToCallNodes().get(start);
			super.getContextSensitivelyAllowed()
					.add(Data.getInstance().getReplacedNodesOriginalToReplacement().get(originalCall));
		}

		visited.add(start);
		if (this.dg.getPredsOfAsSet(start) != null) {
			for (final Unit pred : this.dg.getPredsOfAsSet(start)) {
				Edge temp = new Edge(pred, start);
				final Set<Integer> tempTypes = this.dg.getEdgeTypes(temp);
				// Check ignored edge type
				if (!visited.contains(pred) && !ignoreEdge(tempTypes, ignoreEdgeTypes, ignoreAlways)) {
					// Check if field-label is not assigned OR field-label is considered OR anonymous class edge
					if (this.fieldsToConsider == null || Data.getInstance().getFieldEdgeLabel(temp) == null
							|| this.fieldsToConsider.contains(Data.getInstance().getFieldEdgeLabel(temp))
							|| Data.getInstance().isAnonymousClassEdge(temp)) {
						// Context-Sensitivity check
						if ((!Data.getInstance().getCallToReturnNodes().containsKey(pred)
								&& !Data.getInstance().getCallToReturnNodes().containsKey(
										Data.getInstance().getReplacedNodesReplacementToOriginal().get(pred)))
								|| getContextSensitivelyAllowed().contains(pred)) {
							if (tempTypes.contains(DependenceGraph.TYPE_FIELD_DATA)
									|| tempTypes.contains(DependenceGraph.TYPE_STATIC_FIELD_DATA)) {
								// Re-init context-sensitivity due to (static-)field edge
								init(pred);
							}
							try {
								if (!this.worklist.contains(pred)) {
									this.worklist.add(pred);
								}
							} catch (final StackOverflowError e) {
								Log.error("A StackOverflowError was thrown while slicing through: " + pred
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
		final Set<Unit> replacedCalls = super.findMethodCalls(start);
		super.getContextSensitivelyAllowed().addAll(replacedCalls);
	}
}