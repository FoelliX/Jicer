package de.foellix.aql.jicer.dg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import soot.SootField;
import soot.Unit;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphConstants;
import soot.util.dot.DotGraphEdge;

public class GraphDrawer {
	private static final Integer TYPE_INVISIBLE = -42;
	private DependenceGraph graph;
	private Collection<Unit> slicedUnits;
	private Collection<Unit> highlightIn;
	private Collection<Unit> highlightOut1;
	private Collection<Unit> highlightOut2;

	private DotGraph dot;

	public GraphDrawer(String name, DependenceGraph graph) {
		this.graph = graph;
		this.slicedUnits = null;

		this.dot = new DotGraph(name);
		this.dot.setNodeShape(DotGraphConstants.NODE_SHAPE_BOX);
	}

	public GraphDrawer(String name, DependenceGraph graph, DependenceGraph graphSliced) {
		this(name, graph);
		this.slicedUnits = graphSliced.getAllNodes();
	}

	public GraphDrawer(String name, DependenceGraph graph, Collection<Unit> slicedUnits) {
		this(name, graph);
		this.slicedUnits = slicedUnits;
	}

	public void drawGraph(String output) {
		drawGraph(output, 0);
	}

	public void drawGraph(String output, int level) {
		// Legend
		drawLegendEdge(DependenceGraph.TYPE_CONTROL_ENTRY, "entry");
		drawLegendEdge(DependenceGraph.TYPE_CONTROL, "control");
		drawLegendEdge(DependenceGraph.TYPE_EXCEPTION, "exception");
		drawLegendEdge(DependenceGraph.TYPE_DATA, "data");
		drawLegendEdge(DependenceGraph.TYPE_FIELD_DATA, "field");
		drawLegendEdge(DependenceGraph.TYPE_STATIC_FIELD_DATA, "staticField");
		final Set<Integer> types = new HashSet<>();
		types.add(DependenceGraph.TYPE_DATA);
		types.add(DependenceGraph.TYPE_CONTROL);
		drawLegendEdge(types, "control & data");
		drawLegendEdge(DependenceGraph.TYPE_CALL, "call");
		drawLegendEdge(DependenceGraph.TYPE_PARAMETER_IN, "parameterIN");
		drawLegendEdge(DependenceGraph.TYPE_PARAMETER_OUT, "parameterOUT");
		drawLegendEdge(DependenceGraph.TYPE_SUMMARY, "summary");
		drawLegendEdge(DependenceGraph.TYPE_CALLBACK, "callback");
		drawLegendEdge(DependenceGraph.TYPE_CALLBACK_DEFINITION, "callbackDEF");
		drawLegendEdge(DependenceGraph.TYPE_CALLBACK_ALTERNATIVE, "callbackALT");
		drawLegendEdge(DependenceGraph.TYPE_INPUT, "input");
		drawLegendSliceNode();
		drawLegendExtraNode();
		drawLegendFieldFilteringNode();
		drawLegendContextSensitiveRefinementNode();

		// Generate all edges (nodes are generated implicitly)
		final Set<Integer> edgesAdded = new HashSet<>();
		for (final Unit from : this.graph) {
			for (final Unit to : this.graph.getPredsOfAsSet(from)) {
				final Integer edgeString = new Edge(to, from).hashCode();
				if (!edgesAdded.contains(edgeString)) {
					drawEdge(to, from);
					edgesAdded.add(edgeString);
				}
			}
			for (final Unit to : this.graph.getSuccsOfAsSet(from)) {
				final Integer edgeString = new Edge(from, to).hashCode();
				if (!edgesAdded.contains(edgeString)) {
					drawEdge(from, to);
					edgesAdded.add(edgeString);
				}
			}
		}

		// Mark nodes to be sliced
		markSlicingNodes();

		// Render image
		final File dotFile = new File(output + ".dot");
		final File svgFile = new File(output + ".svg");
		try {
			Graphviz.useEngine(new GraphvizV8Engine());

			if (dotFile.getParentFile() != null && !dotFile.getParentFile().exists()) {
				dotFile.getParentFile().mkdirs();
			}

			final FileOutputStream os = new FileOutputStream(dotFile);
			this.dot.render(os, level);
			os.close();

			final FileInputStream is = new FileInputStream(dotFile);
			final MutableGraph g = new Parser().read(is);

			Graphviz.fromGraph(g).render(Format.SVG).toFile(svgFile);
			is.close();

			dotFile.delete();
		} catch (final Exception e) {
			Log.warning("Could not output debugging graph: " + svgFile.getAbsolutePath() + "."
					+ Log.getExceptionAppendix(e));
		}
	}

	private void drawLegendEdge(int type, String label) {
		final Set<Integer> types = new HashSet<>();
		types.add(type);
		drawLegendEdge(types, label);
	}

	private void drawLegendEdge(Set<Integer> types, String label) {
		drawEdge(label + ">", "<" + label, types);
		this.dot.getNode(label + ">").setAttribute("style", "dashed");
		this.dot.getNode("<" + label).setAttribute("style", "dashed");

		types.clear();
		types.add(TYPE_INVISIBLE);
		drawEdge("<" + label, this.graph.nodeToString(this.graph.getRoot()), types);
	}

	private void drawLegendSliceNode() {
		final String label = "In Slice";
		this.dot.getNode(label).setAttribute("fontcolor", "white");
		this.dot.getNode(label).setAttribute("fillcolor", "black");
		this.dot.getNode(label).setStyle(DotGraphConstants.NODE_STYLE_FILLED);

		final Set<Integer> types = new HashSet<>();
		types.add(TYPE_INVISIBLE);
		drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
	}

	private void drawLegendExtraNode() {
		final String label = "In Extra-Slice";
		this.dot.getNode(label).setAttribute("fillcolor", "gray");
		this.dot.getNode(label).setStyle(DotGraphConstants.NODE_STYLE_FILLED);

		final Set<Integer> types = new HashSet<>();
		types.add(TYPE_INVISIBLE);
		drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
	}

	private void drawLegendFieldFilteringNode() {
		final String label = "Field-Filtered";
		this.dot.getNode(label).setAttribute("color", "brown");
		this.dot.getNode(label).setAttribute("penwidth", "3");

		final Set<Integer> types = new HashSet<>();
		types.add(TYPE_INVISIBLE);
		drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
	}

	private void drawLegendContextSensitiveRefinementNode() {
		final String label = "Context-Sensitive-Refinement";
		this.dot.getNode(label).setAttribute("color", "brown");
		this.dot.getNode(label).setAttribute("style", "dashed");
		this.dot.getNode(label).setAttribute("penwidth", "3");

		final Set<Integer> types = new HashSet<>();
		types.add(TYPE_INVISIBLE);
		drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
	}

	private void drawEdge(Unit from, Unit to) {
		drawEdge(this.graph.nodeToString(from), this.graph.nodeToString(to), this.graph.getEdgeTypes(from, to),
				Data.getInstance().getFieldEdgeLabel(new Edge(from, to)));
	}

	private void drawEdge(String from, String to, Set<Integer> types) {
		drawEdge(from, to, types, null);
	}

	private void drawEdge(String from, String to, Set<Integer> types, SootField label) {
		final DotGraphEdge edge = this.dot.drawEdge(from, to);
		if (label != null) {
			edge.setAttribute("fontcolor", "blue");
			edge.setLabel("[" + label.getName() + "]");
		}
		if (this.graph instanceof DependenceGraph) {
			if (types != null && types.contains(TYPE_INVISIBLE)) {
				edge.setAttribute("color", "white");
				edge.setAttribute("penwidth", "0");
				edge.setAttribute("arrowsize", "0");
			} else {
				if (types != null) {
					if (types.contains(DependenceGraph.TYPE_PARAMETER_IN)) {
						edge.setAttribute("color", "purple");
						edge.setAttribute("style", "dashed");
					} else if (types.contains(DependenceGraph.TYPE_PARAMETER_OUT)) {
						edge.setAttribute("color", "purple");
						edge.setAttribute("style", "dotted");
					} else if (types.contains(DependenceGraph.TYPE_CONTROL)
							&& types.contains(DependenceGraph.TYPE_DATA)) {
						edge.setAttribute("color", "darkgreen");
						edge.setAttribute("fillcolor", "blue");
					} else if (types.contains(DependenceGraph.TYPE_CONTROL_ENTRY)) {
						edge.setAttribute("color", "darkgreen");
						edge.setAttribute("fillcolor", "white");
					} else if (types.contains(DependenceGraph.TYPE_CONTROL)) {
						edge.setAttribute("color", "darkgreen");
					} else if (types.contains(DependenceGraph.TYPE_EXCEPTION)) {
						edge.setAttribute("color", "darkgreen");
						edge.setAttribute("style", "dashed");
					} else if (types.contains(DependenceGraph.TYPE_DATA)
							&& types.contains(DependenceGraph.TYPE_FIELD_DATA)) {
						edge.setAttribute("color", "blue");
						edge.setAttribute("style", "dashed");
					} else if (types.contains(DependenceGraph.TYPE_DATA)
							&& types.contains(DependenceGraph.TYPE_STATIC_FIELD_DATA)) {
						edge.setAttribute("color", "blue");
						edge.setAttribute("style", "dotted");
					} else if (types.contains(DependenceGraph.TYPE_DATA)) {
						edge.setAttribute("color", "blue");
						edge.setAttribute("fillcolor", "white");
					} else if (types.contains(DependenceGraph.TYPE_FIELD_DATA)) {
						edge.setAttribute("color", "blue");
						edge.setAttribute("style", "dashed");
						edge.setAttribute("fillcolor", "white");
					} else if (types.contains(DependenceGraph.TYPE_STATIC_FIELD_DATA)) {
						edge.setAttribute("color", "blue");
						edge.setAttribute("style", "dotted");
						edge.setAttribute("fillcolor", "white");
					} else if (types.contains(DependenceGraph.TYPE_CALL)) {
						edge.setAttribute("color", "purple");
					} else if (types.contains(DependenceGraph.TYPE_SUMMARY)) {
						edge.setAttribute("color", "brown");
						edge.setAttribute("style", "dashed");
					} else if (types.contains(DependenceGraph.TYPE_CALLBACK)) {
						edge.setAttribute("color", "gray");
						edge.setAttribute("style", "dashed");
					} else if (types.contains(DependenceGraph.TYPE_CALLBACK_DEFINITION)) {
						edge.setAttribute("color", "gray");
					} else if (types.contains(DependenceGraph.TYPE_CALLBACK_ALTERNATIVE)) {
						edge.setAttribute("color", "gray");
						edge.setAttribute("style", "dotted");
					} else if (types.contains(DependenceGraph.TYPE_INPUT)) {
						edge.setAttribute("color", "orange");
					} else {
						edge.setAttribute("color", "red");
					}
				} else {
					edge.setAttribute("color", "red");
				}
				edge.setAttribute("penwidth", "2");
				edge.setAttribute("arrowsize", "2");
			}
		}
	}

	private void markSlicingNodes() {
		for (final Unit nodeUnit : this.graph.getAllNodes()) {
			if (this.highlightOut1 != null && this.highlightOut1.contains(nodeUnit)) {
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("color", "brown");
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "3");
			} else if (this.highlightOut2 != null && this.highlightOut2.contains(nodeUnit)) {
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("color", "brown");
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("style", "dashed");
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "3");
			} else {
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "1");
			}
		}
		if (this.slicedUnits != null) {
			for (final Unit nodeUnit : this.slicedUnits) {
				if (this.highlightIn != null && this.highlightIn.contains(nodeUnit)) {
					this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fontcolor", "white");
					this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fillcolor", "black");
				} else {
					this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fillcolor", "gray");
				}
				this.dot.getNode(this.graph.nodeToString(nodeUnit)).setStyle(DotGraphConstants.NODE_STYLE_FILLED);
			}
		}
		if (!this.slicedUnits.containsAll(this.highlightIn)) {
			Log.warning("Incorrect highlighting specified for the graph to be drawn!");
		}
	}

	public void setHighlightIn(Collection<Unit> highlightedUnits) {
		this.highlightIn = highlightedUnits;
	}

	public void setHighlightOut1(Collection<Unit> highlightedUnits) {
		this.highlightOut1 = highlightedUnits;
	}

	public void setHighlightOut2(Collection<Unit> highlightedUnits) {
		this.highlightOut2 = highlightedUnits;
	}
}