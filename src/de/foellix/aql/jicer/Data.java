package de.foellix.aql.jicer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.Edge;
import de.foellix.aql.jicer.soot.ReachingDefinition;
import de.foellix.aql.jicer.soot.SootHelper;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;

public class Data implements Iterable<DependenceGraph> {
	public static final File CALLBACK_FILE = new File("data", "AndroidCallbacks.txt");
	public static final EdgeLabel ANY_FIELD_LABEL = new EdgeLabel();

	private Map<SootMethod, DependenceGraph> pdgMap;
	private Map<Unit, Unit> replacedNodesOriginalToReplacement;
	private Map<Unit, Unit> replacedNodesReplacementToOriginal;
	private Set<Unit> actualParameterNodes;
	private Map<Integer, EdgeLabel> edgeLabels;
	private Map<Unit, Set<ReachingDefinition>> defValuesMap;
	private Map<Unit, SootMethod> entryNodeMap;
	private Set<String> callBackClasses;
	private Map<Unit, Unit> mapCallToReturnNodes;
	private Map<Unit, Unit> mapReturnToCallNodes;
	private Set<String> failedRDs;

	private static Data instance = new Data();

	private Data() {
		this.pdgMap = new ConcurrentHashMap<>();
		this.replacedNodesOriginalToReplacement = new ConcurrentHashMap<>();
		this.replacedNodesReplacementToOriginal = new ConcurrentHashMap<>();
		this.actualParameterNodes = new ConcurrentHashSet<>();
		this.edgeLabels = new ConcurrentHashMap<>();
		this.defValuesMap = new ConcurrentHashMap<>();
		this.entryNodeMap = new ConcurrentHashMap<>();
		this.callBackClasses = null;
		this.mapCallToReturnNodes = new HashMap<>();
		this.mapReturnToCallNodes = new HashMap<>();
		this.failedRDs = new ConcurrentHashSet<>();
	}

	public static Data getInstance() {
		return instance;
	}

	public DependenceGraph getPDG(SootMethod method) {
		return this.pdgMap.get(method);
	}

	public Map<Unit, Unit> getReplacedNodesOriginalToReplacement() {
		return this.replacedNodesOriginalToReplacement;
	}

	public Map<Unit, Unit> getReplacedNodesReplacementToOriginal() {
		return this.replacedNodesReplacementToOriginal;
	}

	public Set<Unit> getActualParameterNodes() {
		return this.actualParameterNodes;
	}

	public void putEntryNode(Unit node, SootMethod sm) {
		this.entryNodeMap.put(node, sm);
	}

	public SootMethod getEntryMethod(Unit node) {
		return this.entryNodeMap.get(node);
	}

	public Set<Unit> getEntryNodes(SootMethod sm) {
		final Set<SootClass> allClasses = SootHelper.getAllAccessibleClasses(sm.getDeclaringClass());
		final Set<Unit> entryNodes = new HashSet<>();
		for (final Unit entryNode : this.entryNodeMap.keySet()) {
			final SootMethod candidate = this.entryNodeMap.get(entryNode);
			if (sm.getSignature().equals(candidate.getSignature())) {
				// Exact match
				entryNodes.clear();
				entryNodes.add(entryNode);
				return entryNodes;
			} else if (sm.getName().equals(candidate.getName())) {
				// Candidate match
				SootClass sc = candidate.getDeclaringClass();
				while (sc != null) {
					if (allClasses.contains(sc)) {
						entryNodes.add(entryNode);
						break;
					}
					boolean added = false;
					for (final SootClass si : sc.getInterfaces()) {
						if (si == sm.getDeclaringClass()) {
							entryNodes.add(entryNode);
							added = true;
							break;
						}
					}
					if (added) {
						break;
					}
					sc = sc.getSuperclassUnsafe();
				}
			}
		}
		return entryNodes;
	}

	public void addPdg(SootMethod method, DependenceGraph pdg) {
		this.pdgMap.put(method, pdg);
	}

	public void addActualParameterNode(Unit unit) {
		this.actualParameterNodes.add(unit);
	}

	public Map<Unit, Set<ReachingDefinition>> getDefValuesMap() {
		return this.defValuesMap;
	}

	public void reset() {
		this.pdgMap.clear();
		this.replacedNodesOriginalToReplacement.clear();
		this.replacedNodesReplacementToOriginal.clear();
		this.actualParameterNodes.clear();
		this.edgeLabels.clear();
		this.defValuesMap.clear();
		this.entryNodeMap.clear();
		this.mapCallToReturnNodes.clear();
		this.mapReturnToCallNodes.clear();
		this.failedRDs.clear();
	}

	@Override
	public Iterator<DependenceGraph> iterator() {
		final List<DependenceGraph> list = new ArrayList<>(this.pdgMap.values());
		return list.iterator();
	}

	public void addAnyFieldEdgeLabel(Edge edge) {
		Log.msg("Adding universal field edge: " + edge, Log.DEBUG);
		this.edgeLabels.put(edge.hashCode(), ANY_FIELD_LABEL);
	}

	public void addFieldEdgeLabel(Edge edge, SootField field) {
		Log.msg("Adding labeled (" + field.getName() + ") field edge: " + edge, Log.DEBUG);
		this.edgeLabels.put(edge.hashCode(), new EdgeLabel(field));
	}

	public boolean isAnonymousClassEdge(Edge edge) {
		if (this.edgeLabels.containsKey(edge.hashCode())) {
			return this.edgeLabels.get(edge.hashCode()).isAny();
		} else {
			return false;
		}
	}

	public SootField getFieldEdgeLabel(Edge edge) {
		if (this.edgeLabels.containsKey(edge.hashCode())) {
			return this.edgeLabels.get(edge.hashCode()).getField();
		} else {
			return null;
		}
	}

	public Set<String> getCallBackClasses() {
		if (this.callBackClasses == null) {
			this.callBackClasses = new HashSet<>();
			if (CALLBACK_FILE.exists()) {
				try {
					for (final String line : Files.readAllLines(CALLBACK_FILE.toPath())) {
						if (!line.startsWith("#")) {
							this.callBackClasses.add(line);
						}
					}
				} catch (final IOException e) {
					Log.error("Could not parse callbacks from \"" + CALLBACK_FILE.getAbsolutePath() + "\"."
							+ Log.getExceptionAppendix(e));
				}
			} else {
				Log.error("Could not find callbacks file: " + CALLBACK_FILE.getAbsolutePath());
			}
		}
		return this.callBackClasses;
	}

	public Map<Unit, Unit> getCallToReturnNodes() {
		return this.mapCallToReturnNodes;
	}

	public Map<Unit, Unit> getReturnToCallNodes() {
		return this.mapReturnToCallNodes;
	}

	private static class EdgeLabel {
		private SootField field;
		private boolean any;

		EdgeLabel() {
			this.field = null;
			this.any = true;
		}

		EdgeLabel(SootField field) {
			this.field = field;
			this.any = false;
		}

		public SootField getField() {
			return this.field;
		}

		public boolean isAny() {
			return this.any;
		}
	}

	public Set<String> getFailedRDs() {
		return this.failedRDs;
	}
}