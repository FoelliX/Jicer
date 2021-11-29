package de.foellix.aql.jicer.dg;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.Parameters;
import de.foellix.aql.jicer.soot.FlowInsensitiveReachingDefinitionAnalysis;
import de.foellix.aql.jicer.soot.ReachingDefinition;
import de.foellix.aql.jicer.soot.ReachingDefinitionAnalysis;
import de.foellix.aql.jicer.soot.ValueOrField;
import de.foellix.aql.jicer.soot.stubdroid.StubDroidReader;
import soot.Body;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class PDGHelper {
	private static Map<Unit, Set<ReachingDefinition>> reachingDefinitionMap = new ConcurrentHashMap<>();

	public static DependenceGraph getPDG(Body b) {
		// Compute Control Flow
		final UnitGraph cfg = new ExceptionalUnitGraph(b);
		final DependenceGraph pdg = new DependenceGraph(b.getMethod(), cfg);
		final MHGPostDominatorsFinder<Unit> domFinder = new MHGPostDominatorsFinder<>(cfg);
		for (final Unit x : cfg) {
			for (final Unit y : cfg.getSuccsOf(x)) {
				if (!domFinder.isDominatedBy(x, y)) {
					addControlDependence(x, y, domFinder, pdg, new HashSet<>());
				}
			}
		}

		// Include entry node
		for (final Unit x : cfg) {
			if (pdg.getPredsOfAsSet(x).isEmpty()) {
				pdg.addEdge(new Edge(pdg.getEntryNode(), x, DependenceGraph.TYPE_CONTROL_ENTRY));
			}
		}

		// Compute Data Flow
		Log.msg("\t- Starting: Reaching Definition analysis of \"" + b.getMethod() + "\"", Log.DEBUG);
		ForwardFlowAnalysis<Unit, Set<ReachingDefinition>> rd = new ReachingDefinitionAnalysis(cfg);
		if (((ReachingDefinitionAnalysis) rd).isFailed() && !Parameters.getInstance().isIncomplete()
				&& Parameters.getInstance().getMode() != Parameters.MODE_SLICE_OUT) {
			Log.msg("\t- Continuing with: Flow-insensitive Reaching Definition analysis of \"" + b.getMethod() + "\"",
					Log.DEBUG);
			rd = new FlowInsensitiveReachingDefinitionAnalysis(cfg);
			Log.msg("\t- Finished: Flow-insensitive Reaching Definition analysis of \"" + b.getMethod() + "\" ("
					+ (rd != null) + ")", Log.DEBUG);
		} else if (((ReachingDefinitionAnalysis) rd).isFailed()) {
			Log.msg("\t- Stopping: Reaching Definition analysis of \"" + b.getMethod() + "\" (" + (rd != null) + ")",
					Log.DEBUG);
		} else {
			Log.msg("\t- Finished: Reaching Definition analysis of \"" + b.getMethod() + "\" after "
					+ ((ReachingDefinitionAnalysis) rd).getK() + " steps (" + (rd != null) + ")", Log.DEBUG);
		}

		for (final Unit defUnit : cfg) {
			Data.getInstance().getDefValuesMap().put(defUnit, rd.getFlowBefore(defUnit));
			final Set<ValueOrField> defValues = getDefValues(defUnit, rd.getFlowBefore(defUnit));

			for (final ValueOrField defValue : defValues) {
				if (defValue.isLocalOrRef()) {
					for (final Unit useUnit : cfg) {
						if (defUnit == useUnit) {
							// Self-edges are omitted due to efficiency
							// boolean useContains = false;
							// for (final ValueBox use : cfgUnit.getUseBoxes()) {
							// if (use.getValue() == v.getValue()) {
							// useContains = true;
							// break;
							// }
							// }
							// if (!useContains) {
							continue;
							// }
						}

						boolean valid = false;
						for (final ValueOrField useValue : getUseValues(useUnit)) {
							valid = useValue.equals(defValue);
							if (valid) {
								break;
							}
						}

						if (valid && contains(rd.getFlowBefore(useUnit), defValue, defUnit)) {
							pdg.addEdge(new Edge(defUnit, useUnit, DependenceGraph.TYPE_DATA));
						}
					}
				}
			}
		}

		// Add exceptions thrown
		for (final Unit unit : b.getUnits()) {
			if (unit instanceof ThrowStmt) {
				final ThrowStmt castedUnit = (ThrowStmt) unit;
				final Type type = castedUnit.getOp().getType();
				pdg.addExceptionThrown(type, unit);
			}
		}

		for (final Unit unit : b.getUnits()) {
			reachingDefinitionMap.put(unit, rd.getFlowBefore(unit));
		}

		return pdg;
	}

	private static void addControlDependence(Unit x, Unit z, MHGPostDominatorsFinder<Unit> domFinder,
			DependenceGraph pdg, Set<Unit> visited) {
		visited.add(z);
		if (x != z) {
			pdg.addEdge(new Edge(x, z, DependenceGraph.TYPE_CONTROL));
			for (final Unit newZ : domFinder.getGraph().getPredsOf(z)) {
				if (newZ != z && !visited.contains(newZ)) {
					addControlDependence(x, newZ, domFinder, pdg, visited);
				}
			}
		}
	}

	private static Set<ValueOrField> getDefValues(Unit defUnit, Set<ReachingDefinition> rds) {
		final Set<ValueOrField> defValues = new HashSet<>();

		// DefBoxes
		for (final ValueBox box : defUnit.getDefBoxes()) {
			defValues.add(new ValueOrField(box.getValue()));
		}
		// Other
		if (defUnit instanceof Stmt) {
			if (defUnit instanceof DefinitionStmt) {
				defValues.add(new ValueOrField(((DefinitionStmt) defUnit).getLeftOp()));
			}
			if (!defValues.isEmpty()) {
				final Set<ValueOrField> toAdd = new HashSet<>();
				for (final ValueOrField defValue : defValues) {
					for (final ReachingDefinition aliasRD : rds) {
						if (aliasRD.hasAliases() && aliasRD.getValueOrField().equals(defValue)) {
							toAdd.addAll(aliasRD.getAliases());
						}
					}
				}
				defValues.addAll(toAdd);
			}
			if (((Stmt) defUnit).containsInvokeExpr()) {
				// Base
				if (((Stmt) defUnit).getInvokeExpr() instanceof InstanceInvokeExpr) {
					if (StubDroidReader.assignsToBase(((Stmt) defUnit).getInvokeExpr().getMethod())) {
						defValues.add(
								new ValueOrField(((InstanceInvokeExpr) ((Stmt) defUnit).getInvokeExpr()).getBase()));
					}
				}
				// Parameters
				for (int i = 0; i < ((Stmt) defUnit).getInvokeExpr().getArgCount(); i++) {
					if (StubDroidReader.assignsToParameter(((Stmt) defUnit).getInvokeExpr().getMethod(), i)) {
						defValues.add(new ValueOrField(((Stmt) defUnit).getInvokeExpr().getArg(i)));
					}
				}
			}
		}

		return defValues;
	}

	public static Set<ValueOrField> getUseValues(Unit useUnit) {
		final Set<ValueOrField> useValues = new HashSet<>();

		// UseBoxes
		for (final ValueBox useBox : useUnit.getUseBoxes()) {
			useValues.add(new ValueOrField(useBox.getValue()));
		}
		// ArrayRef on LHS
		if (useUnit instanceof DefinitionStmt) {
			final Value lhs = ((DefinitionStmt) useUnit).getLeftOp();
			if (lhs instanceof ArrayRef) {
				useValues.add(new ValueOrField(lhs));
			}
		}
		// Base
		if (useUnit instanceof InvokeStmt) {
			final InvokeStmt otherCfgUnitsInvokeStmt = (InvokeStmt) useUnit;
			if (otherCfgUnitsInvokeStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr otherCfgUnitsInvokeExpr = (InstanceInvokeExpr) otherCfgUnitsInvokeStmt
						.getInvokeExpr();
				useValues.add(new ValueOrField(otherCfgUnitsInvokeExpr.getBase()));
			}
		}

		return useValues;
	}

	private static boolean contains(Set<ReachingDefinition> rds, ValueOrField value, Unit defUnit) {
		for (final ReachingDefinition rd : rds) {
			if (rd.getValueOrField().equals(value) && rd.getUnits().contains(defUnit)) {
				return true;
			}
		}
		return false;
	}

	public static Set<ReachingDefinition> getReachingDefinition(Unit unit) {
		return reachingDefinitionMap.get(unit);
	}

	public static void reset() {
		reachingDefinitionMap.clear();
	}
}
