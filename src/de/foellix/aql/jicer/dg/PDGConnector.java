package de.foellix.aql.jicer.dg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.Parameters;
import de.foellix.aql.jicer.callgraphenhancer.CallGraphEnhancer;
import de.foellix.aql.jicer.soot.ReachingDefinition;
import de.foellix.aql.jicer.soot.SootHelper;
import soot.Local;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;

public class PDGConnector {
	private DependenceGraph currentDG;
	private CallGraph cg;
	private CallGraphEnhancer cge;
	private Unit currentUnit;
	private Unit currentReplacement;
	private int counter;

	private List<Edge> edgesToAdd;
	private List<Unit> nodesToRemove;
	private Map<String, AssignStmt> parameterMap;
	private Map<Unit, AssignStmt> returnMap;
	private Map<Unit, Unit> replacementMap;

	public PDGConnector() {
		this.counter = 0;
		this.cg = null;
		this.cge = null;
	}

	public DependenceGraph buildSDG() {
		// Add parameter nodes
		Log.msg("Adding parameter nodes", Log.NORMAL);
		this.parameterMap = new HashMap<>();
		this.returnMap = new HashMap<>();
		this.replacementMap = new HashMap<>();
		this.edgesToAdd = new ArrayList<>();
		this.nodesToRemove = new ArrayList<>();
		for (final DependenceGraph pdg : Data.getInstance()) {
			this.edgesToAdd.clear();
			this.nodesToRemove.clear();

			this.currentDG = pdg;
			for (final Unit unit : pdg) {
				this.currentUnit = unit;
				findActualParameters(unit);
			}
			for (final Edge edge : this.edgesToAdd) {
				if (this.replacementMap.containsKey(edge.getFrom())) {
					edge.setFrom(this.replacementMap.get(edge.getFrom()));
				}
				if (this.replacementMap.containsKey(edge.getTo())) {
					edge.setTo(this.replacementMap.get(edge.getTo()));
				}
				pdg.addEdge(edge);
			}
			for (final Unit node : this.nodesToRemove) {
				pdg.removeNode(node);
			}
		}

		// Merge graphs
		Log.msg("Merging PDGs", Log.NORMAL);
		final Iterator<DependenceGraph> iterator = Data.getInstance().iterator();
		final DependenceGraph sdg = iterator.next();
		while (iterator.hasNext()) {
			sdg.mergeWith(iterator.next());
		}

		// Add merged edges
		Log.msg("Adding connecting edges", Log.NORMAL);
		this.currentDG = sdg;
		for (final Unit unit : sdg) {
			this.currentUnit = unit;
			if (unit instanceof Stmt) {
				final Stmt castedUnit = (Stmt) unit;
				if (castedUnit.containsInvokeExpr()) {
					buildGraphConnection(unit, castedUnit.getInvokeExpr());
				}
			}
		}

		// Add field edges
		Log.msg("Adding (static-)field edges", Log.NORMAL);
		addFieldEdges(sdg);

		return sdg;
	}

	private void addFieldEdges(DependenceGraph sdg) {
		final Map<SootField, Set<Unit>> fieldUsages = new HashMap<>();
		final Map<SootField, Set<Unit>> fieldDefinitions = new HashMap<>();
		for (final Unit unit : sdg.getAllNodes()) {
			// Usage of field
			if (unit instanceof DefinitionStmt) {
				final DefinitionStmt casted = (DefinitionStmt) unit;
				if (casted.getRightOp() instanceof FieldRef) {
					final SootField field = ((FieldRef) casted.getRightOp()).getField();
					if (!fieldUsages.containsKey(field)) {
						fieldUsages.put(field, new HashSet<>());
					}
					fieldUsages.get(field).add(unit);
				}
			}

			// Definition of field
			if (PDGHelper.getReachingDefinition(unit) != null) {
				for (final ReachingDefinition rd : PDGHelper.getReachingDefinition(unit)) {
					if (rd.getValueOrField().isField()) {
						final SootField field = rd.getValueOrField().getField();
						if (!fieldDefinitions.containsKey(field)) {
							fieldDefinitions.put(field, new HashSet<>());
						}
						fieldDefinitions.get(field).addAll(rd.getUnits());
					}
				}
			}
		}

		// Remove uses that have a direct definition predecessor
		if (!Parameters.getInstance().isStrictThreadSensitivity()) {
			for (final SootField field : fieldUsages.keySet()) {
				final Set<Unit> usesToRemove = new HashSet<>();
				for (final Unit use : fieldUsages.get(field)) {
					for (final Unit pred : sdg.getPredsOfAsSet(use)) {
						if (fieldDefinitions.get(field).contains(pred)) {
							Log.msg("Not considering field use (" + use
									+ ") because of a direct definition predecessor: " + pred, Log.DEBUG_DETAILED);
							usesToRemove.add(use);
						}
					}
				}
				fieldUsages.get(field).removeAll(usesToRemove);
			}
		}

		// Connect Usages with their Definitions
		for (final SootField field : fieldUsages.keySet()) {
			for (final Unit use : fieldUsages.get(field)) {
				if (fieldDefinitions.containsKey(field)) {
					// Collect all edges
					final Set<Edge> edges = new HashSet<>();
					for (final Unit def : fieldDefinitions.get(field)) {
						if (def != null) {
							if (!sdg.hasEdge(def, use)) {
								final Edge edge = new Edge(def, use);
								edges.add(edge);
							}
							// else {
							// edges.clear();
							// break;
							// }
						}
					}
					// Add all edges
					if (!edges.isEmpty()) {
						for (final Edge edge : edges) {
							addFieldEdge(edge, field, sdg);
						}
					}
				}
			}
		}
	}

	private void addFieldEdge(Edge edge, SootField field, DependenceGraph sdg) {
		if (field.isStatic()) {
			edge.setType(DependenceGraph.TYPE_STATIC_FIELD_DATA);
		} else {
			edge.setType(DependenceGraph.TYPE_FIELD_DATA);
		}
		sdg.addEdge(edge);
		if (SootHelper.isAnonymousClass(field.getDeclaringClass()) || SootHelper.isTemporaryField(field)) {
			Data.getInstance().addAnyFieldEdgeLabel(edge);
		} else {
			Data.getInstance().addFieldEdgeLabel(edge, field);
		}
	}

	private void buildGraphConnection(Unit unit, InvokeExpr expr) {
		try {
			// Find interface methods
			if (this.cg == null) {
				this.cg = Scene.v().getCallGraph();
				this.cge = new CallGraphEnhancer();
			}
			Unit cgUnit = Data.getInstance().getReplacedNodesReplacementToOriginal().get(unit);
			if (cgUnit == null) {
				cgUnit = unit;
			}
			this.cge.enhance(this.cg, cgUnit);
			final Set<SootMethod> methodTargets = new HashSet<>();
			methodTargets.add(expr.getMethod());
			for (final Iterator<soot.jimple.toolkits.callgraph.Edge> i = this.cg.edgesOutOf(cgUnit); i.hasNext();) {
				final SootMethod target = i.next().tgt();
				methodTargets.add(target);
			}
			if (methodTargets.size() > 1) {
				final StringBuilder sb = new StringBuilder("Over-Approximating method call (from statement \"" + unit
						+ "\" - " + unit.getJavaSourceStartLineNumber() + "): " + expr.getMethod());
				for (final SootMethod target : methodTargets) {
					sb.append("\n\t- " + target);
				}
				Log.warning(sb.toString());
			}

			// Add static constructors if needed
			final Set<SootMethod> toAdd = new HashSet<>();
			for (final SootMethod methodTarget : methodTargets) {
				if (methodTarget.isConstructor()) {
					final SootMethod staticConstructor = methodTarget.getDeclaringClass()
							.getMethodByNameUnsafe(KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME);
					if (staticConstructor != null) {
						if (expr.getArgCount() == staticConstructor.getParameterCount()) {
							toAdd.add(staticConstructor);
						}
					}
				}
			}
			methodTargets.addAll(toAdd);

			// Connect method calls with targets
			for (final SootMethod methodTarget : methodTargets) {
				if (Data.getInstance().getPDG(methodTarget) != null) {
					// Call
					this.currentDG.addEdge(new Edge(unit, Data.getInstance().getPDG(methodTarget).getEntryNode(),
							DependenceGraph.TYPE_CALL));

					// Parameters
					for (int i = 0; i < expr.getArgCount(); i++) {
						if (Data.getInstance().getPDG(methodTarget).parameterExists(i)) {
							final Unit from = this.parameterMap.get(toArgsString(unit, i));
							this.currentDG
									.addEdge(new Edge(from, Data.getInstance().getPDG(methodTarget).getParameterNode(i),
											DependenceGraph.TYPE_PARAMETER_IN));
						}
					}

					for (final Unit returnNode : Data.getInstance().getPDG(methodTarget).getReturnNodes()) {
						final Unit returnParameterNode = this.returnMap.get(unit);
						if (returnParameterNode != null) {
							this.currentDG.addEdge(
									new Edge(returnNode, returnParameterNode, DependenceGraph.TYPE_PARAMETER_OUT));
						}
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private String toArgsString(Unit unit, int number) {
		return unit.toString() + "#" + number;
	}

	private void findActualParameters(Unit unit) {
		if (unit instanceof Stmt) {
			final Stmt castedUnit = (Stmt) unit;
			if (castedUnit.containsInvokeExpr()) {
				if (unit instanceof AssignStmt) {
					findActualParameters(castedUnit, ((AssignStmt) unit).getLeftOp());
				} else {
					findActualParameters(castedUnit, null);
				}
			}
		}
	}

	private void findActualParameters(Stmt unit, Value returnValue) {
		Unit returnNode = null;
		if (returnValue != null) {
			returnNode = buildActualReturnNode(returnValue);
			Data.getInstance().addActualParameterNode(returnNode);
		} else {
			this.currentReplacement = this.currentUnit;
		}
		final List<Unit> parameterNodes = new ArrayList<>();
		for (int i = 0; i < unit.getInvokeExpr().getArgCount(); i++) {
			final Value value = unit.getInvokeExpr().getArgs().get(i);
			if (value instanceof Local || value instanceof Constant) {
				final Unit parameterNode = buildActualParameterNodes(value, i);
				parameterNodes.add(parameterNode);
				Data.getInstance().addActualParameterNode(parameterNode);
			}
		}

		if (returnNode != null) {
			if (!parameterNodes.isEmpty()) {
				for (final Unit parameterNode : parameterNodes) {
					this.edgesToAdd.add(new Edge(parameterNode, returnNode, DependenceGraph.TYPE_SUMMARY));
				}
			}
			Log.msg("Return: " + returnNode + " <-> Call: " + unit, Log.DEBUG_DETAILED);
			Data.getInstance().getReturnToCallNodes().put(returnNode, unit);
			Data.getInstance().getCallToReturnNodes().put(unit, returnNode);
		}
	}

	private Unit buildActualParameterNodes(Value value, int number) {
		final Local newLocal = Jimple.v().newLocal("$actual_parameter_" + this.counter++, value.getType());
		final AssignStmt parameterNode = Jimple.v().newAssignStmt(newLocal, value);

		this.edgesToAdd.add(new Edge(this.currentReplacement, parameterNode, DependenceGraph.TYPE_CONTROL));
		this.edgesToAdd.add(new Edge(this.currentReplacement, parameterNode, DependenceGraph.TYPE_DATA));
		this.parameterMap.put(toArgsString(this.currentReplacement, number), parameterNode);

		return parameterNode;
	}

	private Unit buildActualReturnNode(Value value) {
		final Local newLocal = Jimple.v().newLocal("$actual_return_parameter_" + this.counter++, value.getType());
		final AssignStmt returnParameterNode = Jimple.v().newAssignStmt(value, newLocal);

		final AssignStmt replacement = Jimple.v().newAssignStmt(newLocal, ((AssignStmt) this.currentUnit).getRightOp());
		addAll(returnParameterNode.getBoxesPointingToThis(), replacement);
		this.returnMap.put(replacement, returnParameterNode);
		this.currentReplacement = replacement;
		addAll(this.currentUnit.getBoxesPointingToThis(), this.currentReplacement);
		Data.getInstance().getReplacedNodesOriginalToReplacement().put(this.currentUnit, this.currentReplacement);
		Data.getInstance().getReplacedNodesReplacementToOriginal().put(this.currentReplacement, this.currentUnit);
		this.replacementMap.put(this.currentUnit, this.currentReplacement);
		this.edgesToAdd.add(new Edge(replacement, returnParameterNode, DependenceGraph.TYPE_CONTROL));
		this.edgesToAdd.add(new Edge(replacement, returnParameterNode, DependenceGraph.TYPE_DATA));
		for (final Unit from : this.currentDG.getPredsOfAsSet(this.currentUnit)) {
			for (final int type : this.currentDG.getEdgeTypes(from, this.currentUnit)) {
				this.edgesToAdd.add(new Edge(from, replacement, type));
			}
		}
		for (final Unit to : this.currentDG.getSuccsOfAsSet(this.currentUnit)) {
			for (final int type : this.currentDG.getEdgeTypes(this.currentUnit, to)) {
				if (type != DependenceGraph.TYPE_DATA) {
					this.edgesToAdd.add(new Edge(replacement, to, type));
				}
			}
		}
		this.nodesToRemove.add(this.currentUnit);

		for (final Unit to : this.currentDG.getSuccsOfAsSet(this.currentUnit)) {
			this.edgesToAdd.add(new Edge(returnParameterNode, to, DependenceGraph.TYPE_DATA));
		}

		return returnParameterNode;
	}

	private void addAll(List<UnitBox> add, Unit to) {
		for (final UnitBox ub : add) {
			to.addBoxPointingToThis(ub);
		}
	}
}