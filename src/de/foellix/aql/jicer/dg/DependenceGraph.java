package de.foellix.aql.jicer.dg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.statistics.Statistics;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;

public class DependenceGraph implements DirectedGraph<Unit> {
	public static final int TYPE_CONTROL_ENTRY = 0;
	public static final int TYPE_CONTROL = 1;
	public static final int TYPE_DATA = 2;
	public static final int TYPE_PARAMETER_IN = 3;
	public static final int TYPE_PARAMETER_OUT = 4;
	public static final int TYPE_CALL = 5;
	public static final int TYPE_SUMMARY = 6;
	public static final int TYPE_FIELD_DATA = 7;
	public static final int TYPE_STATIC_FIELD_DATA = 8;
	public static final int TYPE_CALLBACK = 9;
	public static final int TYPE_CALLBACK_DEFINITION = 10;
	public static final int TYPE_CALLBACK_ALTERNATIVE = 11;
	public static final int TYPE_EXCEPTION = 12;
	public static final int TYPE_INPUT = 13;
	public static List<Integer> ALL_TYPES = Arrays.asList(new Integer[] { TYPE_CONTROL_ENTRY, TYPE_CONTROL, TYPE_DATA,
			TYPE_PARAMETER_IN, TYPE_PARAMETER_OUT, TYPE_CALL, TYPE_SUMMARY, TYPE_FIELD_DATA, TYPE_STATIC_FIELD_DATA,
			TYPE_CALLBACK, TYPE_CALLBACK_DEFINITION, TYPE_CALLBACK_ALTERNATIVE, TYPE_EXCEPTION, TYPE_INPUT });

	private static Lock lock = new ReentrantLock();

	private SootMethod reference;
	private Set<Unit> allNodes;
	private List<Unit> entryNodes;
	private List<Unit> returnNodes;
	private Unit entryNode;
	private Map<Unit, Set<Unit>> predecessors;
	private Map<Unit, Set<Unit>> successors;
	private Map<Integer, Set<Integer>> typeMap;
	private Unit[] parameterNodes;
	private Map<Type, Set<Unit>> exceptionsThrown;
	private Unit root;

	public DependenceGraph(SootMethod reference, UnitGraph cfg) {
		this(reference, cfg, null);
	}

	public DependenceGraph(SootMethod reference, UnitGraph cfg, Unit entryNode) {
		this.reference = reference;

		this.allNodes = new HashSet<>();
		this.predecessors = new HashMap<>();
		this.successors = new HashMap<>();
		this.typeMap = new HashMap<>();
		this.exceptionsThrown = new HashMap<>();
		this.root = null;

		// Entry node
		if (entryNode == null) {
			this.entryNode = Jimple.v().newNopStmt();
		} else {
			this.entryNode = entryNode;
		}
		this.entryNodes = new ArrayList<>();
		this.entryNodes.add(this.entryNode);
		addNode(this.entryNode);

		// Other nodes
		this.returnNodes = new ArrayList<>();
		this.parameterNodes = new Unit[reference.getParameterCount()];
		if (cfg != null) {
			for (final Unit unit : cfg) {
				addNode(unit);
				for (final ValueBox box : unit.getUseBoxes()) {
					if (box.getValue() instanceof ParameterRef) {
						this.parameterNodes[((ParameterRef) box.getValue()).getIndex()] = unit;
					}
				}
				if (unit instanceof ReturnStmt) {
					this.returnNodes.add(unit);
				}
			}
		}
	}

	public Unit getParameterNode(int number) {
		return this.parameterNodes[number];
	}

	public boolean parameterExists(int number) {
		return this.parameterNodes.length > number;
	}

	public List<Unit> getReturnNodes() {
		return this.returnNodes;
	}

	public Unit getEntryNode() {
		return this.entryNode;
	}

	public Unit getRoot() {
		if (this.root != null) {
			return this.root;
		} else {
			final List<Unit> candidates = new ArrayList<>();
			for (final Unit head : getHeads()) {
				if (getPredsOfAsSet(head).size() == 0) {
					candidates.add(head);
				}
			}
			if (candidates.size() > 0) {
				Log.msg("The graph has more than one root node - selecting dummy main.", Log.DEBUG);
				for (final Unit candidate : candidates) {
					if (nodeToString(candidate).contains("ENTRY: dummyMainMethod")) {
						this.root = candidate;
						break;
					}
				}
				if (this.root == null) {
					Log.msg("No dummy main found - selecting first candidate.", Log.DEBUG);
					this.root = candidates.get(0);
				}
			} else if (candidates.size() == 1) {
				this.root = candidates.get(0);
			}
			return this.root;
		}
	}

	@Override
	public List<Unit> getHeads() {
		return this.entryNodes;
	}

	@Override
	public List<Unit> getTails() {
		return null;
	}

	public Set<Unit> getPredsOfAsSet(Unit unit) {
		return this.predecessors.get(unit);
	}

	@Override
	public List<Unit> getPredsOf(Unit unit) {
		throw new UnsupportedOperationException("Please use \"getPredsOfAsSet\" instead!");
	}

	public Set<Unit> getSuccsOfAsSet(Unit unit) {
		return this.successors.get(unit);
	}

	@Override
	public List<Unit> getSuccsOf(Unit unit) {
		throw new UnsupportedOperationException("Please use \"getSuccsOfAsSet\" instead!");
	}

	public Set<Unit> getDescendants(Unit unit) {
		return getDescendants(unit, new HashSet<>());
	}

	public Set<Unit> getDescendants(Unit unit, Set<Unit> visited) {
		final Set<Unit> descendants = new HashSet<>(getSuccsOfAsSet(unit));
		if (getSuccsOfAsSet(unit) != null) {
			for (final Unit succ : getSuccsOfAsSet(unit)) {
				if (!visited.contains(succ)) {
					visited.add(succ);
					descendants.addAll(getDescendants(succ, visited));
				}
			}
		}
		return descendants;
	}

	@Override
	public int size() {
		return this.allNodes.size();
	}

	@Override
	public Iterator<Unit> iterator() {
		return this.allNodes.iterator();
	}

	public SootMethod getReference() {
		return this.reference;
	}

	public Set<Unit> getAllNodes() {
		return this.allNodes;
	}

	public void addNode(Unit unit) {
		nodeToString(unit);
		this.allNodes.add(unit);
		this.predecessors.put(unit, new HashSet<>());
		this.successors.put(unit, new HashSet<>());
	}

	public Set<Unit> notIn(Set<Unit> in) {
		final Set<Unit> notIn = new HashSet<>(this.allNodes);
		notIn.removeAll(in);
		return notIn;
	}

	public void removeNodes(Set<Unit> unitsToRemove) {
		if (unitsToRemove == null) {
			return;
		}
		for (final Unit unitToRemove : unitsToRemove) {
			removeNode(unitToRemove);
		}
	}

	public void removeNode(Unit node) {
		this.allNodes.remove(node);
		if (this.predecessors.containsKey(node)) {
			for (final Unit pred : this.predecessors.get(node)) {
				if (this.successors.containsKey(pred)) {
					this.successors.get(pred).remove(node);
				}
			}
		}
		if (this.successors.containsKey(node)) {
			for (final Unit succ : this.successors.get(node)) {
				if (this.predecessors.containsKey(succ)) {
					this.predecessors.get(succ).remove(node);
				}
			}
		}
		this.predecessors.remove(node);
		this.successors.remove(node);
	}

	public void addEdge(Edge edge) {
		// Check if node is known
		if (!this.allNodes.contains(edge.getFrom())) {
			addNode(edge.getFrom());
		}
		if (!this.allNodes.contains(edge.getTo())) {
			addNode(edge.getTo());
		}

		// Type
		final Integer edgeHash = new Edge(edge.getFrom(), edge.getTo()).hashCode();
		if (!this.typeMap.containsKey(edgeHash)) {
			this.typeMap.put(edgeHash, new HashSet<>());
		}
		this.typeMap.get(edgeHash).add(edge.getType());
		Statistics.getCounter(Statistics.COUNTER_SDG_TYPED_EDGES[edge.getType()]).increase();

		// Predecessors & Successors
		this.predecessors.get(edge.getTo()).add(edge.getFrom());
		this.successors.get(edge.getFrom()).add(edge.getTo());
	}

	public boolean hasEdge(Unit from, Unit to) {
		if (this.successors.get(from) == null) {
			return false;
		} else {
			return this.successors.get(from).contains(to);
		}
	}

	public Set<Integer> getEdgeTypes(Edge edge) {
		return this.typeMap.get(edge.hashCode());
	}

	public Set<Integer> getEdgeTypes(Unit from, Unit to) {
		return this.typeMap.get(new Edge(from, to).hashCode());
	}

	public String nodeToString(Unit node) {
		return nodeToString(node, this.reference);
	}

	public static String nodeToString(Unit node, SootMethod sm) {
		String str = node.toString();
		if (node instanceof NopStmt) {
			lock.lock();
			if (Data.getInstance().getEntryMethod(node) == null) {
				if (sm != null) {
					Data.getInstance().putEntryNode(node, sm);
				} else {
					Log.error("No suitable method entry found: " + node);
					return node.hashCode() + ":\nUNKNOWN";
				}
			}
			final SootMethod loadedRef = Data.getInstance().getEntryMethod(node);
			lock.unlock();
			str = "ENTRY: " + loadedRef.getName() + (loadedRef.getParameterTypes().isEmpty() ? "" : "\n")
					+ (loadedRef.getParameterTypes()).toString().replace("[", "(").replace("]", ")") + "\n["
					+ loadedRef.getDeclaringClass().getName() + "]";
		} else if (Data.getInstance().getReturnToCallNodes().containsKey(node)) {
			str = str + "\nRETURN: " + Data.getInstance().getReplacedNodesOriginalToReplacement()
					.get(Data.getInstance().getReturnToCallNodes().get(node)).hashCode();
		}
		return node.hashCode() + ":\n" + str;
	}

	public Map<Integer, Set<Integer>> getTypeMap() {
		return this.typeMap;
	}

	public void addExceptionThrown(Type type, Unit thrownBy) {
		if (!this.exceptionsThrown.containsKey(type)) {
			this.exceptionsThrown.put(type, new HashSet<>());
		}
		this.exceptionsThrown.get(type).add(thrownBy);
	}

	public Set<Unit> getExceptionsThrown(Type type) {
		return this.exceptionsThrown.get(type);
	}

	public void mergeWith(DependenceGraph graph) {
		// Merge nodes
		for (final Unit unit : graph) {
			this.allNodes.add(unit);
		}

		// Merge entry nodes
		this.entryNodes.addAll(graph.getHeads());

		// Merge Edges
		for (final Unit unit : graph) {
			if (this.predecessors.containsKey(unit)) {
				this.predecessors.get(unit).addAll(graph.getPredsOfAsSet(unit));
			} else {
				this.predecessors.put(unit, graph.getPredsOfAsSet(unit));
			}
			if (this.successors.containsKey(unit)) {
				this.successors.get(unit).addAll(graph.getSuccsOfAsSet(unit));
			} else {
				this.successors.put(unit, graph.getSuccsOfAsSet(unit));
			}
		}
		for (final Integer edgeHash : graph.getTypeMap().keySet()) {
			this.typeMap.put(edgeHash, graph.getTypeMap().get(edgeHash));
		}
	}

	@Override
	public DependenceGraph clone() {
		final DependenceGraph clone = new DependenceGraph(this.reference, null, this.entryNode);
		clone.allNodes.addAll(this.allNodes);
		clone.entryNodes.addAll(this.entryNodes);
		clone.returnNodes.addAll(this.returnNodes);
		clone.entryNode = this.entryNode;
		for (final Unit key : this.predecessors.keySet()) {
			clone.predecessors.put(key, new HashSet<>(this.predecessors.get(key)));
		}
		for (final Unit key : this.successors.keySet()) {
			clone.successors.put(key, new HashSet<>(this.successors.get(key)));
		}
		for (final Integer key : this.typeMap.keySet()) {
			clone.typeMap.put(key, new HashSet<>(this.typeMap.get(key)));
		}
		clone.parameterNodes = this.parameterNodes.clone();
		for (final Type key : this.exceptionsThrown.keySet()) {
			clone.exceptionsThrown.put(key, new HashSet<>(this.exceptionsThrown.get(key)));
		}
		clone.root = this.root;
		return clone;
	}
}