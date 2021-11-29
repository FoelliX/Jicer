package de.foellix.aql.jicer.soot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class FlowInsensitiveReachingDefinitionAnalysis extends ForwardFlowAnalysis<Unit, Set<ReachingDefinition>> {
	private Set<ReachingDefinition> rds;

	public FlowInsensitiveReachingDefinitionAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		final Map<ValueOrField, Set<Unit>> map = new HashMap<>();
		for (final Unit u : graph) {
			if (u instanceof AssignStmt) {
				final AssignStmt stmt = (AssignStmt) u;
				final ValueOrField key = new ValueOrField(stmt.getLeftOp());
				if (!map.containsKey(key)) {
					map.put(key, new HashSet<>());
				}
				map.get(key).add(u);
			}
		}

		this.rds = new HashSet<>();
		for (final ValueOrField key : map.keySet()) {
			final ReachingDefinition rd = new ReachingDefinition(key);
			rd.getUnits().addAll(map.get(key));
			this.rds.add(rd);
		}
	}

	@Override
	protected void flowThrough(Set<ReachingDefinition> in, Unit d, Set<ReachingDefinition> out) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Set<ReachingDefinition> newInitialFlow() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void merge(Set<ReachingDefinition> in1, Set<ReachingDefinition> in2, Set<ReachingDefinition> out) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void copy(Set<ReachingDefinition> source, Set<ReachingDefinition> dest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<ReachingDefinition> getFlowAfter(Unit s) {
		return this.rds;
	}

	@Override
	public Set<ReachingDefinition> getFlowBefore(Unit s) {
		return this.rds;
	}
}
