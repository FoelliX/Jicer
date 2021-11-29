package de.foellix.aql.jicer.cfgslicer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.soot.SootHelper;
import de.foellix.aql.jicer.statistics.Statistics;
import soot.Body;
import soot.DoubleType;
import soot.FloatType;
import soot.IntegerType;
import soot.LongType;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

public class CFGSlicer {
	private static final String[] DEBUG_DO_NOT_SLICE = {};

	private DependenceGraph sdgSliced;
	private DependenceGraph sdg;
	private boolean runnableOutput;
	private boolean sliceThroughOrdinaryLibraries;
	private boolean incomplete;
	private boolean outputWriteable;
	private Set<SootMethod> calledMethods;

	public CFGSlicer(DependenceGraph sdgSliced, DependenceGraph sdg, boolean runnableOutput,
			boolean sliceThroughOrdinaryLibraries, boolean incomplete, boolean outputWriteable) {
		this.sdgSliced = sdgSliced;
		this.sdg = sdg;
		this.runnableOutput = runnableOutput;
		this.sliceThroughOrdinaryLibraries = sliceThroughOrdinaryLibraries;
		this.incomplete = incomplete;
		this.outputWriteable = outputWriteable;
		this.calledMethods = null;

		// Measure
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (this.sliceThroughOrdinaryLibraries || !SootHelper.isOrdinaryLibraryClass(sc, false)) {
				Statistics.getCounter(Statistics.COUNTER_CLASSES).increase();
				Statistics.getCounter(Statistics.COUNTER_METHODS).increase(sc.getMethodCount());
				for (final SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						final Body b = sm.retrieveActiveBody();
						Statistics.getCounter(Statistics.COUNTER_STATEMENTS).increase(b.getUnits().size());
					}
				}
			}
		}
	}

	public void show(SootClass sc) {
		boolean print = false;
		boolean notMarkedMethod = false;
		final StringBuilder sbm = new StringBuilder();
		final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
		for (final SootMethod sm : snapshotIterator) {
			final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
			if (body == null) {
				continue;
			}
			boolean notMarkedUnit = false;
			final StringBuilder sbu = new StringBuilder();
			for (final Unit su : body.getUnits()) {
				boolean contained = this.sdgSliced.getAllNodes().contains(su);
				if (!contained) {
					contained = this.sdgSliced.getAllNodes()
							.contains(Data.getInstance().getReplacedNodesOriginalToReplacement().get(su));
				}
				if (contained || ignoreUnit(su, true, true)
						|| (this.runnableOutput && ignoreAnonymousClassSuperConstructorCall(sc, sm, su))) {
					sbu.append("\t\t>");
					print = true;
				} else {
					sbu.append("\t\t");
					notMarkedUnit = true;
				}
				sbu.append("\t" + su + "\n");
			}
			if (!notMarkedUnit) {
				sbm.append(">");
			} else {
				notMarkedMethod = true;
			}
			sbm.append("\t\t" + sm.getSignature() + " {\n" + sbu.toString() + "\t\t}\n\n");
		}
		final StringBuilder sbc = new StringBuilder("Showing Slice:\n");
		if (!notMarkedMethod) {
			sbc.append(">");
		}
		sbc.append("\t" + sc.getName() + " {\n" + sbm.toString() + "\n\t}\n");
		if (print) {
			Log.msg(sbc.toString(), Log.NORMAL);
		}
	}

	public void sliceout(SootClass sc) {
		final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
		for (final SootMethod sm : snapshotIterator) {
			if (sm.isConcrete()) {
				final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
				if (body != null) {
					for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
						Unit unit = i.next();
						final Unit removeUnit = unit;
						final Unit replacement = Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
						if (!this.sdgSliced.getAllNodes().contains(unit)
								&& (replacement != null && this.sdgSliced.getAllNodes().contains(replacement))) {
							unit = replacement;
						}
						if (this.sdgSliced.getAllNodes().contains(unit)) {
							final Collection<Unit> succs = this.sdg.getSuccsOfAsSet(unit);
							final Collection<Unit> succsSliced = this.sdgSliced.getSuccsOfAsSet(unit);
							if (succs != null) {
								succs.removeAll(Data.getInstance().getActualParameterNodes());
							}
							if (succsSliced != null) {
								succsSliced.removeAll(Data.getInstance().getActualParameterNodes());
							}

							if ((succs == null && succsSliced == null) || (succs == null && succsSliced.size() == 0)
									|| (succsSliced == null && succs.size() == 0)
									|| (succs != null && succsSliced != null && succs.size() == succsSliced.size())) {
								Log.msg("Slicing Unit: " + sc.getName() + " -> " + sm.getName() + " -> "
										+ removeUnit.toString(), Log.DEBUG);
								body.getUnits().remove(removeUnit);
								Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED).increase();
							}
						}
					}
					cleanUpTraps(body);
					cleanUpReturns(body);
				}
			}
		}
	}

	public void slice(SootClass sc) {
		// Find called methods
		findCalledMethods();

		// Slice Fields
		sliceFields(sc);

		// Slice Methods
		sliceMethods(sc);
	}

	private void findCalledMethods() {
		if (this.calledMethods == null) {
			this.calledMethods = new HashSet<>();
			for (Unit unit : this.sdgSliced.getAllNodes()) {
				if (ignoreUnit(unit, true, true)) {
					continue;
				}
				final Unit replacement = Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
				if (!this.sdgSliced.getAllNodes().contains(unit)
						&& (replacement != null && this.sdgSliced.getAllNodes().contains(replacement))) {
					unit = replacement;
				}
				if (this.sdgSliced.getAllNodes().contains(unit)) {
					if (unit instanceof InvokeStmt) {
						final SootMethod calledMethod = ((InvokeStmt) unit).getInvokeExpr().getMethod();
						this.calledMethods.add(calledMethod);
					}
				}
			}
		}
	}

	private void sliceFields(SootClass sc) {
		// Collect occurring fields
		final Set<SootField> occurringFields = new HashSet<>();
		for (final Unit unit : this.sdgSliced.getAllNodes()) {
			for (final ValueBox box : unit.getUseAndDefBoxes()) {
				if (box.getValue() instanceof FieldRef) {
					occurringFields.add(((FieldRef) box.getValue()).getField());
				}
			}
		}

		// Remove fields
		final Collection<SootField> toRemoveFields = new ArrayList<>();
		final Chain<SootField> fields = sc.getFields();
		if (fields != null && !fields.isEmpty()) {
			for (final SootField sf : fields) {
				if (!occurringFields.contains(sf)) {
					Log.msg("Slicing Field: " + sc.getName() + " -> " + sf, Log.DEBUG_DETAILED);
					toRemoveFields.add(sf);
				}
			}
			for (final SootField sf : toRemoveFields) {
				fields.remove(sf);
			}
		}
	}

	private void sliceMethods(SootClass sc) {
		// Collect methods
		final Set<SootMethod> toRemoveMethods = new HashSet<>();
		final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
		for (final SootMethod sm : snapshotIterator) {
			final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
			if (body == null) {
				continue;
			}
			boolean checkNeeded = false;
			for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
				Unit unit = i.next();
				if (ignoreUnit(unit, true, true)
						|| (this.runnableOutput && ignoreAnonymousClassSuperConstructorCall(sc, sm, unit))) {
					continue;
				}

				// Take care of any other units
				final Unit removeUnit = unit;
				final Unit replacement = Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
				if (!this.sdgSliced.getAllNodes().contains(unit)
						&& (replacement != null && this.sdgSliced.getAllNodes().contains(replacement))) {
					unit = replacement;
				}
				if (!this.sdgSliced.getAllNodes().contains(unit)) {
					checkNeeded = true;
					String unitStr;
					try {
						unitStr = removeUnit.toString();
					} catch (final NullPointerException e) {
						unitStr = removeUnit.getClass().getSimpleName() + "(incomplete)";
					}
					if (DEBUG_DO_NOT_SLICE.length == 0
							|| !Arrays.asList(DEBUG_DO_NOT_SLICE).contains(removeUnit.clone().toString())) {
						Log.msg("Slicing Unit: " + sc.getName() + " -> " + sm.getName() + " -> " + unitStr, Log.DEBUG);
						body.getUnits().remove(removeUnit);
						Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED).increase();
					} else {
						Log.msg("Not slicing Unit: " + sc.getName() + " -> " + sm.getName() + " -> " + unitStr,
								Log.DEBUG);
					}
				}
			}

			cleanUpTraps(body);

			if (checkNeeded) {
				if (isEmpty(body, true)) {
					toRemoveMethods.add(sm);
				}
			}
		}

		// Remove methods
		for (final SootMethod sm : toRemoveMethods) {
			if (!sm.isConstructor() && !sm.isStaticInitializer() && sm.isConcrete()) {
				if (!this.calledMethods.contains(sm) && isEmpty(sm.getActiveBody(), false)) {
					Log.msg("Slicing Method: " + sc.getName() + " -> " + sm.getSubSignature(), Log.DEBUG);
					Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED)
							.increase(sm.getActiveBody().getUnits().size());
					sc.removeMethod(sm);
					Statistics.getCounter(Statistics.COUNTER_METHODS_SLICED).increase();
				} else {
					// Stumping method, since removing it leads to an ConcurrentModificationException
					Log.msg("Stumping Method: " + sc.getName() + " -> " + sm.getSubSignature(), Log.DEBUG);
					int stmtsStumped = sm.getActiveBody().getUnits().size();
					stumpMethod(sm);
					cleanUpReturns(sm.getActiveBody());
					stmtsStumped -= sm.getActiveBody().getUnits().size();
					Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED).increase(stmtsStumped);
					Statistics.getCounter(Statistics.COUNTER_METHODS_STUMPED).increase();
				}
			} else {
				cleanUpReturns(sm.getActiveBody());
				Log.msg("Keeping Constructor: " + sc.getName() + " -> " + sm.getSubSignature(), Log.DEBUG);
			}
		}
	}

	private Set<Unit> getConnectedUnitChain(Unit unit, UnitGraph cfg) {
		final Set<Unit> chain = new HashSet<>();
		chain.add(unit);
		for (final Unit next : cfg.getSuccsOf(unit)) {
			if (cfg.getPredsOf(next).size() == 1 && cfg.getPredsOf(next).iterator().next() == unit) {
				chain.addAll(getConnectedUnitChain(next, cfg));
			}
		}
		return chain;
	}

	private void stumpMethod(SootMethod sm) {
		final Body b = sm.getActiveBody();
		final List<Unit> toRemove = new ArrayList<>();
		for (final Unit unit : b.getUnits()) {
			if (!ignoreUnit(unit, true, false)) {
				toRemove.add(unit);
			}
		}
		for (final Unit unit : toRemove) {
			b.getUnits().remove(unit);
		}
		final List<Trap> trapsToRemove = new ArrayList<>(b.getTraps());
		for (final Trap trap : trapsToRemove) {
			b.getTraps().remove(trap);
		}

	}

	private void cleanUpTraps(Body body) {
		if (body != null && !body.getUnits().isEmpty()) {
			// CleanUp Catch Clauses
			final ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(body);
			if (cfg != null) {
				final Set<Unit> toRemove = new HashSet<>();
				for (final Unit unit : cfg) {
					if (ignoreCatches(unit) && cfg.getExceptionalPredsOf(unit).isEmpty()) {
						final Set<Unit> chain = getConnectedUnitChain(unit, cfg);
						Log.msg("Slicing Catch: " + body.getMethod().getSignature() + " -> {" + chain + "}", Log.DEBUG);
						toRemove.addAll(chain);
					}
				}
				for (final Unit cfgUnit : toRemove) {
					body.getUnits().remove(cfgUnit);
				}
			}

			// CleanUp Traps (which do not have any effect)
			for (final Iterator<Trap> i = body.getTraps().snapshotIterator(); i.hasNext();) {
				final Trap t = i.next();
				if (t.getBeginUnit() == t.getEndUnit() && t.getEndUnit() == t.getHandlerUnit()) {
					body.getTraps().remove(t);
					if (t.getHandlerUnit() instanceof DefinitionStmt) {
						if (((DefinitionStmt) t.getHandlerUnit()).getRightOp() instanceof CaughtExceptionRef) {
							body.getUnits().remove(t.getHandlerUnit());
						}
					}
					Log.msg("Slicing Trap: " + body.getMethod().getSignature() + " -> (" + t.getBeginUnit() + ", "
							+ t.getEndUnit() + ", " + t.getHandlerUnit() + ")", Log.DEBUG);
				}
			}
		}
	}

	private void cleanUpReturns(Body body) {
		// CleanUp methods which basically only contain return statements
		boolean noBranchingPossible = true;
		Unit firstReturn = null;
		for (final Unit u : body.getUnits()) {
			if (u instanceof IfStmt || u instanceof soot.jimple.GotoStmt) {
				noBranchingPossible = false;
				break;
			} else if (firstReturn == null && (u instanceof ReturnStmt || u instanceof ReturnVoidStmt)) {
				firstReturn = u;
				break;
			}
		}
		if (noBranchingPossible) {
			boolean returnFound = false;
			for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
				final Unit u = i.next();
				if (returnFound) {
					body.getUnits().remove(u);
				} else if (u instanceof ReturnStmt) {
					final Type type = ((ReturnStmt) u).getOp().getType();
					Value returnValue;
					if (type instanceof PrimType) {
						if (type instanceof IntegerType) {
							returnValue = IntConstant.v(1);
						} else if (type instanceof DoubleType) {
							returnValue = DoubleConstant.v(1);
						} else if (type instanceof FloatType) {
							returnValue = FloatConstant.v(1);
						} else if (type instanceof LongType) {
							returnValue = LongConstant.v(1);
						} else {
							returnValue = IntConstant.v(1);
						}
					} else {
						returnValue = NullConstant.v();
					}
					body.getUnits().insertBefore(Jimple.v().newReturnStmt(returnValue), u);
					body.getUnits().remove(u);
					Log.msg("Slicing dead return of " + body.getMethod().getSignature() + ": " + u, Log.DEBUG);
					returnFound = true;
				}
			}
		}
	}

	private boolean isEmpty(Body body, boolean ignoreGotosAndCatches) {
		final UnitPatchingChain units = body.getUnits();
		if (units.isEmpty()) {
			return true;
		}
		for (final Unit unit : units) {
			if (!ignoreUnit(unit, false, ignoreGotosAndCatches)) {
				return false;
			}
		}
		return true;
	}

	private boolean ignoreUnit(Unit unit, boolean ignoreSetContentView, boolean ignoreGotosAndCatches) {
		if (this.incomplete) {
			if (this.outputWriteable) {
				if (unit instanceof DefinitionStmt) {
					if (((DefinitionStmt) unit).getRightOp() instanceof ParameterRef) {
						return true;
					}
				}
			}
			return false;
		}

		// Skip this assignment
		if (unit instanceof DefinitionStmt) {
			if (((DefinitionStmt) unit).getRightOp() instanceof ThisRef) {
				return true;
			}
		}
		// Skip parameter assignments
		if (unit instanceof DefinitionStmt) {
			if (((DefinitionStmt) unit).getRightOp() instanceof ParameterRef) {
				return true;
			}
		}
		// Skip return
		if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
			return true;
		}
		// Skip Goto
		if (ignoreGotosAndCatches && ignoreGoto(unit)) {
			return true;
		}
		// Skip Catches
		if (ignoreGotosAndCatches && ignoreCatches(unit)) {
			return true;
		}
		// Skip setContentView method calls
		if (ignoreSetContentView) {
			if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
					&& ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (invoke.getMethod().getName().equals("setContentView")) {
					if (hasActivitySuperClass(invoke.getMethod().getDeclaringClass())) {
						return true;
					}
				}
			}
		}
		// Skip Activity constructor calls
		if (this.runnableOutput) {
			if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
					&& ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (hasActivitySuperClass(invoke.getMethod().getDeclaringClass())) {
					if (invoke.getMethod().getName().equals("<init>")
							|| SootHelper.isCallBackMethod(invoke.getMethod())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean ignoreGoto(Unit unit) {
		if (this.incomplete) {
			return false;
		}

		if (unit instanceof GotoStmt) {
			final Unit target = ((GotoStmt) unit).getTarget();
			final Unit replacedTarget = Data.getInstance().getReplacedNodesOriginalToReplacement().get(target);
			if (target instanceof ReturnStmt || target instanceof ReturnVoidStmt) {
				return true;
			} else if (this.sdgSliced.getAllNodes().contains(target)
					|| this.sdgSliced.getAllNodes().contains(replacedTarget)) {
				return true;
			}
		}
		return false;
	}

	private boolean ignoreCatches(Unit unit) {
		if (this.incomplete) {
			return false;
		}

		if (unit instanceof DefinitionStmt) {
			if (((DefinitionStmt) unit).getRightOp() instanceof CaughtExceptionRef) {
				return true;
			}
		}
		return false;
	}

	private boolean hasActivitySuperClass(SootClass sc) {
		if (sc.getName().equals("android.app.Activity")) {
			return true;
		} else if (sc.getSuperclassUnsafe() == null) {
			return false;
		} else {
			return hasActivitySuperClass(sc.getSuperclass());
		}
	}

	private boolean ignoreAnonymousClassSuperConstructorCall(SootClass sc, SootMethod sm, Unit unit) {
		if (this.incomplete) {
			return false;
		}

		if (SootHelper.isAnonymousClass(sc) && sm.isConstructor()) {
			if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()
					&& ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
				if (invoke.getMethod().getName().equals("<init>")) {
					if (SootHelper.getAllAccessibleClasses(sc).contains(invoke.getMethod().getDeclaringClass())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}