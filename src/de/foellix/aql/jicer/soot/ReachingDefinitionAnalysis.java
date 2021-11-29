package de.foellix.aql.jicer.soot;

import java.util.HashSet;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.Parameters;
import de.foellix.aql.jicer.soot.stubdroid.StubDroidReader;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class ReachingDefinitionAnalysis extends ForwardFlowAnalysis<Unit, Set<ReachingDefinition>> {
	private static boolean DEBUG = false;

	private boolean failed;
	private Set<ValueOrField> allValues;
	private int k;
	private SootMethod method;

	public ReachingDefinitionAnalysis(UnitGraph graph) {
		super(graph);

		this.failed = false;
		this.allValues = new HashSet<>();
		this.k = 0;
		this.method = graph.getBody().getMethod();

		for (final Unit u : graph) {
			for (final ValueBox box : u.getUseAndDefBoxes()) {
				if (box.getValue() instanceof Local || box.getValue() instanceof Ref) {
					this.allValues.add(new ValueOrField(box.getValue()));
				}
			}
		}

		doAnalysis();
	}

	@Override
	protected void flowThrough(Set<ReachingDefinition> in, Unit unit, Set<ReachingDefinition> out) {
		log("\tFlow function call: " + unit);
		log("\t\t-> IN: " + in);

		out.clear();
		for (final ReachingDefinition rd : in) {
			out.add(rd.copy());
		}

		this.k++;
		if (Parameters.getInstance().getkLimit() > 0 && this.k > Parameters.getInstance().getkLimit()) {
			Log.warning("Reaching Definition analysis execution step limit (" + Parameters.getInstance().getkLimit()
					+ ") reached considering: " + this.method + ". Stopping analysis!");
			Data.getInstance().getFailedRDs().add(this.method.getSignature());
			if (!this.failed) {
				this.failed = true;
			}
			return;
		} else if (this.k
				% (Parameters.getInstance().getkLimit() > 0 ? Parameters.getInstance().getkLimit() / 10 : 1000) == 0) {
			Log.msg("Reaching Definition analysis reached " + this.k
					+ (Parameters.getInstance().getkLimit() > 0 ? "/" + Parameters.getInstance().getkLimit() : "")
					+ " execution steps considering: " + this.method, Log.DEBUG);
		}

		InvokeExpr invokeExpr = null;
		if (unit instanceof Stmt) {
			final Stmt castedUnit = ((Stmt) unit);
			if (castedUnit.containsInvokeExpr() && castedUnit.getInvokeExpr() instanceof InvokeExpr) {
				invokeExpr = castedUnit.getInvokeExpr();
			}
			if (unit instanceof DefinitionStmt) {
				final ValueOrField lhs = new ValueOrField(((DefinitionStmt) castedUnit).getLeftOp());
				final ValueOrField rhs = new ValueOrField(((DefinitionStmt) castedUnit).getRightOp());
				if (lhs.isLocalOrRef()) {
					// Local/ArrayRef1 = ... (Update unit for local/base1)
					log("\t\t\tAction 1: " + lhs);
					updateUnit(lhs, unit, getRDs(lhs, out), out, true);
					if (rhs.isArray()) {
						// ... = ArrayRef
						log("\t\t\tAction 2: " + rhs);
						addAlias(lhs, unit, rhs, getRDs(lhs, out));
					}
					if (rhs.isField()) {
						// ... = FieldRef
						log("\t\t\tAction 3: " + rhs);
						addAlias(lhs, unit, rhs, getRDs(lhs, out));
					}
				}
			}
		}

		// Take care of invokes
		if (invokeExpr != null) {
			// Base
			if (invokeExpr instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr castedInvokeExpr = (InstanceInvokeExpr) invokeExpr;
				if (StubDroidReader.assignsToBase(invokeExpr.getMethod())) {
					final ValueOrField base = new ValueOrField(castedInvokeExpr.getBase());
					if (base.isLocalOrRef()) {
						log("\t\t\tAction 4: " + base);
						updateUnit(base, unit, getRDs(base, out), out, false);
					}
				}
			}
			// Parameters
			for (int i = 0; i < invokeExpr.getArgCount(); i++) {
				if (StubDroidReader.assignsToParameter(invokeExpr.getMethod(), i)) {
					final ValueOrField parameter = new ValueOrField(invokeExpr.getArg(i));
					if (parameter.isLocalOrRef()) {
						log("\t\t\tAction 5: " + parameter);
						updateUnit(parameter, unit, getRDs(parameter, out), out, false);
					}
				}
			}
		}

		log("\t\t-> OUT: " + out);
	}

	private ReachingDefinition getRDs(ValueOrField valueOrField, Set<ReachingDefinition> rds) {
		for (final ReachingDefinition rd : rds) {
			if (valueOrField.equals(rd.getValueOrField())) {
				return rd;
			}
		}
		return null;
	}

	private void updateUnit(ValueOrField lhs, Unit unit, ReachingDefinition rd, Set<ReachingDefinition> rds,
			boolean killAliases) {
		rd.getUnits().clear();
		rd.getUnits().add(unit);
		if (killAliases && !lhs.isArray()) {
			rd.getAliases().clear();
		} else {
			updateAliases(lhs, unit, rd, rds);
		}
	}

	private void updateAliases(ValueOrField lhs, Unit unit, ReachingDefinition rd, Set<ReachingDefinition> rds) {
		if (rd.hasAliases()) {
			for (final ValueOrField alias : rd.getAliases()) {
				if (this.allValues.contains(alias)) {
					updateUnit(alias, unit, getRDs(alias, rds), rds, false);
				}
			}
		}
	}

	private void addAlias(ValueOrField lhs, Unit unit, ValueOrField rhs, ReachingDefinition rd) {
		rd.getAliases().add(rhs);
	}

	@Override
	protected Set<ReachingDefinition> entryInitialFlow() {
		return newInitialFlow();
	}

	@Override
	protected Set<ReachingDefinition> newInitialFlow() {
		final Set<ReachingDefinition> init = new HashSet<>();
		for (final ValueOrField value : this.allValues) {
			init.add(new ReachingDefinition(value));
		}
		return init;
	}

	@Override
	protected void merge(Set<ReachingDefinition> in1, Set<ReachingDefinition> in2, Set<ReachingDefinition> out) {
		log("\t\t\tAction: Merge");

		out.clear();
		for (final ReachingDefinition rdIn1 : in1) {
			final ReachingDefinition rdOut = rdIn1.copy();
			out.add(rdOut);
			for (final ReachingDefinition rdIn2 : in2) {
				if (rdIn1.getValueOrField().equals(rdIn2.getValueOrField())) {
					rdOut.getUnits().addAll(rdIn2.getUnits());
					rdOut.getAliases().addAll(rdIn2.getAliases());
					break;
				}
			}
		}
	}

	@Override
	protected void copy(Set<ReachingDefinition> source, Set<ReachingDefinition> dest) {
		dest.clear();
		for (final ReachingDefinition rd : source) {
			dest.add(rd.copy());
		}
	}

	private void log(String msg) {
		if (DEBUG) {
			Log.msg(msg, Log.DEBUG);
		}
	}

	public boolean isFailed() {
		return this.failed;
	}

	public int getK() {
		return this.k;
	}
}