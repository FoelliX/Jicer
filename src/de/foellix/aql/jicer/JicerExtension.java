package de.foellix.aql.jicer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.sdgslicer.SDGBackwardSlicer;
import de.foellix.aql.jicer.sdgslicer.SDGForwardSlicer;
import de.foellix.aql.jicer.sdgslicer.SDGSlicer;
import de.foellix.aql.jicer.sdgslicer.SDGSlicerComparator;
import de.foellix.aql.jicer.statistics.Counter;
import de.foellix.aql.jicer.statistics.Timer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;

public class JicerExtension {
	public static final String UNIT_NEEDLE = "%UNIT%";
	public static final String UNIT_LINENUMBER_NEEDLE = "%UNIT_LINENUMBER%";

	private String msg;
	private Timer timer;
	private Counter counterSteps;
	private Counter counterSize;
	private DependenceGraph sdg;
	private boolean isBackward;
	private Set<Unit> toKeep;
	private Set<Unit> toSliceExtra;
	private Set<Integer> ignoreTypes;

	protected JicerExtension(String msg, Timer timer, Counter counterSteps, Counter counterSize, DependenceGraph sdg,
			boolean isBackward, Set<Unit> toKeep, Set<Unit> toSliceExtra) {
		this.msg = msg;
		this.timer = timer;
		this.counterSteps = counterSteps;
		this.counterSize = counterSize;
		this.sdg = sdg;
		this.isBackward = isBackward;
		this.toKeep = toKeep;
		this.toSliceExtra = toSliceExtra;

		this.ignoreTypes = null;
	}

	protected void setIgnoreTypes(Set<Integer> ignoreTypes) {
		this.ignoreTypes = ignoreTypes;
	}

	/**
	 * Start slicing extras
	 *
	 * @param stopAtMethodSwitch
	 *            slice across methods
	 * @param imprecise
	 *            Take context-sensitivity into account when loading which statements have been visited before (true: only take fields into account; false: take fields and context-sensitivity into account)
	 */
	protected void slice() {
		// Prepare
		final List<SDGSlicer> extraSlicers = new LinkedList<>();
		int i = 0;
		for (final Unit unit : this.toSliceExtra) {
			Log.msg("(" + ++i + "/" + this.toSliceExtra.size() + ") Preparing "
					+ this.msg.replace(UNIT_NEEDLE, unit.toString()).replace(UNIT_LINENUMBER_NEEDLE,
							String.valueOf(unit.getJavaSourceStartLineNumber()).toString()),
					Log.DEBUG);
			this.timer.start();
			SDGSlicer slicer;
			if (this.isBackward) {
				slicer = new SDGBackwardSlicer(this.sdg, this.counterSteps, unit);
			} else {
				slicer = new SDGForwardSlicer(this.sdg, this.counterSteps, unit);
			}
			if (this.ignoreTypes != null) {
				slicer.getIgnoreTypes().addAll(this.ignoreTypes);
			}
			extraSlicers.add(slicer);
			this.timer.stop();
			Log.msg("done", Log.DEBUG);
		}

		// Sort
		extraSlicers.sort(SDGSlicerComparator.getInstance());

		// Slice
		i = 0;
		for (final SDGSlicer slicer : extraSlicers) {
			boolean subset = false;
			for (final SDGSlicer slicerBefore : extraSlicers) {
				if (slicerBefore == slicer) {
					break;
				}
				if (slicerBefore.getGlobalVisit().getContextSensitivity()
						.containsAll(slicer.getGlobalVisit().getContextSensitivity())) {
					slicer.getGlobalVisit().getVisited().addAll(slicerBefore.getGlobalVisit().getVisited());
					if (slicerBefore.getGlobalVisit().getVisited().contains(slicer.getTarget())) {
						subset = true;
						break;
					}
				}
			}
			if (!subset) {
				Log.msg("(" + ++i + "/" + this.toSliceExtra.size() + ") Slicing "
						+ this.msg.replace(UNIT_NEEDLE, slicer.getTarget().toString()).replace(UNIT_LINENUMBER_NEEDLE,
								String.valueOf(slicer.getTarget().getJavaSourceStartLineNumber())),
						Log.NORMAL);
				final Set<Unit> extraSlice = slicer.slice();
				this.counterSize.increase(extraSlice.size());
				this.toKeep.addAll(extraSlice);
				Log.msg("done", Log.DEBUG);
			} else {
				Log.msg("(" + ++i + "/" + this.toSliceExtra.size() + ") Skipping (was contained in a previous slice) "
						+ this.msg.replace(UNIT_NEEDLE, slicer.getTarget().toString()).replace(UNIT_LINENUMBER_NEEDLE,
								String.valueOf(slicer.getTarget().getJavaSourceStartLineNumber())),
						Log.DEBUG);
			}
		}
	}

	protected static Set<Unit> contextSensitiveRefinement(DependenceGraph sdgSliced, Unit targetTo, Set<Unit> toSlice,
			Set<Unit> fromSlice) {
		// Find junctions
		Set<Unit> toRemove = new HashSet<>();
		for (final Unit unit : sdgSliced) {
			if (unit instanceof NopStmt) {
				final SootMethod method = Data.getInstance().getEntryMethod(unit);
				if (method != null) {
					final Set<Unit> candidates = new HashSet<>();
					for (final Unit pred : sdgSliced.getPredsOfAsSet(unit)) {
						if (pred instanceof Stmt) {
							final Stmt castedPred = (Stmt) pred;
							if (castedPred.containsInvokeExpr()) {
								if (method == castedPred.getInvokeExpr().getMethod()) {
									candidates.add(pred);
								}
							}
						}
					}
					if (candidates.size() >= 2) {
						final StringBuilder sb = new StringBuilder("Found context sensitive refinement callee:\n"
								+ sdgSliced.nodeToString(unit) + "\nwith the callers:\n" + candidates
								+ "\nof which the following do not appear in the associated forward slice:\n");
						if (candidates.removeAll(fromSlice) && !candidates.isEmpty()) {
							sb.append(candidates.toString());
							Log.msg(sb.toString(), Log.DEBUG_DETAILED);
							toRemove.addAll(candidates);
						}
					}
				}
			}
		}

		// Refine
		sdgSliced.removeNodes(toRemove);
		toRemove = findDeadUnits(sdgSliced, targetTo);
		sdgSliced.removeNodes(toRemove);
		return toRemove;
	}

	private static Set<Unit> findDeadUnits(DependenceGraph sdgSliced, Unit targetTo) {
		final Set<Unit> toRemove = new HashSet<>(sdgSliced.getAllNodes());
		final Set<Unit> visited = new HashSet<>();
		findDeadUnits(sdgSliced, targetTo, visited);
		toRemove.removeAll(visited);
		return toRemove;
	}

	private static void findDeadUnits(DependenceGraph sdgSliced, Unit unit, Set<Unit> visited) {
		if (!visited.contains(unit)) {
			visited.add(unit);
			for (final Unit pred : sdgSliced.getPredsOfAsSet(unit)) {
				findDeadUnits(sdgSliced, pred, visited);
			}
		}
	}
}