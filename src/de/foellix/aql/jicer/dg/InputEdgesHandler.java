package de.foellix.aql.jicer.dg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Attributes;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.soot.SootHelper;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class InputEdgesHandler {
	private static List<Flow> inputEdges = null;

	public static void evaluateInputEdges(File aqlAnswerFile) {
		if (aqlAnswerFile != null) {
			if (aqlAnswerFile.exists()) {
				final Answer aqlAnswer = AnswerHandler.parseXML(aqlAnswerFile);
				if (aqlAnswer != null) {
					if (aqlAnswer.getFlows() != null && !aqlAnswer.getFlows().getFlow().isEmpty()) {
						inputEdges = aqlAnswer.getFlows().getFlow();
						Log.msg("Loaded " + inputEdges.size() + " input-edges from (AQL-Answer-)file ("
								+ aqlAnswerFile.getAbsolutePath() + ").", Log.NORMAL);
						return;
					}
					Log.msg("Loaded input-edges from (AQL-Answer-)file (" + aqlAnswerFile.getAbsolutePath()
							+ ") successfully but it does not contain any flows.", Log.NORMAL);
					return;
				}
			}
			Log.warning("Loading input-edges from (AQL-Answer-)file (" + aqlAnswerFile.getAbsolutePath() + ") failed!");
			return;
		}
		Log.warning(
				"Loading input-edges from (AQL-Answer-)file failed: No file given! (Usage: -ie \"path/to/AQL-Answer.xml\")");
	}

	public static void addInputEdges(DependenceGraph sdg) {
		if (inputEdges != null) {
			for (final Flow edge : inputEdges) {
				final Unit from = findUnitForReference(Helper.getFrom(edge));
				if (from != null) {
					final Unit to = findUnitForReference(Helper.getTo(edge));
					if (to != null) {
						final Edge inputEdge = new Edge(from, to, DependenceGraph.TYPE_INPUT);
						sdg.addEdge(inputEdge);
						sdg.getEdgeTypes(inputEdge).addAll(getTypes(edge.getAttributes()));
						Log.msg("Adding input edge from \"" + from + "\" to \"" + to + "\". (given input: "
								+ Helper.toString(edge) + ")", Log.DEBUG);
						continue;
					}
				}
				Log.warning("Could not identify input edge!\n(" + Helper.toString(edge) + ")");
			}
		}
	}

	private static List<Integer> getTypes(Attributes attributes) {
		final List<Integer> temp = new ArrayList<>();
		if (attributes != null && !attributes.getAttribute().isEmpty()) {
			for (final Attribute attribute : attributes.getAttribute()) {
				if (attribute.getName().equalsIgnoreCase("type")) {
					try {
						final Integer type = Integer.valueOf(attribute.getValue());
						temp.add(type);
					} catch (final NumberFormatException e) {
						continue;
					}
				}
			}
		}
		return temp;
	}

	private static Unit findUnitForReference(Reference reference) {
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete() && EqualsHelper.equalsClassString(sc.getName(), reference.getClassname())) {
				for (final SootMethod sm : sc.getMethods()) {
					if (EqualsHelper.equalsMethodString(sm.getSignature(), reference.getMethod())) {
						final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
						if (body == null) {
							continue;
						}

						// Find unit (precise)
						for (final Unit unit : body.getUnits()) {
							if (EqualsHelper.equalsStatementString(unit.toString(),
									reference.getStatement().getStatementfull())) {
								if (Data.getInstance().getReplacedNodesOriginalToReplacement().containsKey(unit)) {
									return Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
								} else {
									return unit;
								}
							}
						}

						// Find unit (weak)
						for (final Unit unit : body.getUnits()) {
							if (EqualsHelper.equalsStatementString(Helper.cut(unit.toString(), "<", ">"),
									reference.getStatement().getStatementgeneric())) {
								if (Data.getInstance().getReplacedNodesOriginalToReplacement().containsKey(unit)) {
									return Data.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
								} else {
									return unit;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
}
