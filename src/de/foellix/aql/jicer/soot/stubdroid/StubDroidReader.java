package de.foellix.aql.jicer.soot.stubdroid;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Parameters;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.xml.SummaryReader;
import soot.jimple.infoflow.methodSummary.xml.SummaryXMLException;

public class StubDroidReader {
	private static final File STUB_DROID_DIR = new File("data", "StubDroidSummaries");
	private static final File STUB_DROID_MANUAL_DIR = new File(STUB_DROID_DIR, "manual");

	private static Map<String, ClassMethodSummaries> summariesMap = new HashMap<>();
	private static Map<String, Set<String>> interfaceMap = new HashMap<>();

	/*
	 * Assigns to base
	 */
	public static boolean assignsToBase(SootMethod method) {
		return assignsToBase(method, new HashSet<>());
	}

	private static boolean assignsToBase(SootMethod method, Set<SootMethod> visited) {
		if (visited.contains(method)) {
			return false;
		} else {
			visited.add(method);
		}

		// Check if methods assigns to any field of this or calls super class
		if (method.isConstructor()) {
			return true;
		}
		Body b;
		try {
			b = method.retrieveActiveBody();
		} catch (final RuntimeException e) {
			return assignsToBaseStubDroid(method);
		}
		for (final Iterator<Unit> i = b.getUnits().snapshotIterator(); i.hasNext();) {
			final Unit unit = i.next();
			if (unit.toString().startsWith("specialinvoke $") && unit.toString()
					.endsWith(".<java.lang.RuntimeException: void <init>(java.lang.String)>(\"Stub!\")")) {
				return assignsToBaseStubDroid(method);
			} else if (unit instanceof Stmt) {
				if (unit instanceof DefinitionStmt) {
					final DefinitionStmt castedUnit = ((DefinitionStmt) unit);
					if (castedUnit.getLeftOp() instanceof FieldRef) {
						if (method.getDeclaringClass().getFields().contains(castedUnit.getFieldRef().getField())) {
							// This field
							return true;
						}
						SootClass parent = method.getDeclaringClass();
						while (parent.getSuperclassUnsafe() != null) {
							// Super field
							parent = parent.getSuperclass();
							if (parent.getFields().contains(castedUnit.getFieldRef().getField())) {
								return true;
							}
						}
					}
				}

				if (((Stmt) unit).containsInvokeExpr()) {
					// Super/This call
					final InvokeExpr call = ((Stmt) unit).getInvokeExpr();
					final SootMethod calledMethod = call.getMethod();
					final SootClass calledClass = calledMethod.getDeclaringClass();
					SootClass parent = method.getDeclaringClass();
					do {
						if (calledClass == parent) {
							if (assignsToBaseStubDroid(calledMethod)) {
								return true;
							} else {
								if (assignsToBase(calledMethod, visited)) {
									return true;
								}
							}
						}
						parent = parent.getSuperclassUnsafe();
					} while (parent != null);

					// Field as argument
					for (final Value parameter : call.getArgs()) {
						parent = method.getDeclaringClass();
						do {
							if (parent.getType() == parameter.getType()) {
								if (assignsToBaseStubDroid(calledMethod)) {
									return true;
								} else if (assignsToBase(calledMethod, visited)) {
									return true;
								}
							}
							parent = parent.getSuperclassUnsafe();
						} while (parent != null);
					}
				}
			}
		}
		return false;
	}

	private static boolean assignsToBaseStubDroid(SootMethod sm) {
		return assignsToBaseStubDroid(sm.getDeclaringClass(), sm);
	}

	private static boolean assignsToBaseStubDroid(SootClass sc, SootMethod sm) {
		return assignsToBaseStubDroid(sc.getName(), sm);
	}

	private static boolean assignsToBaseStubDroid(String className, SootMethod sm) {
		final ClassMethodSummaries summaries = readStubDroidSummaries(className);

		boolean assignsToBase = false;
		if (summaries != null) {
			// Check if assigns to base
			final Set<MethodFlow> summary = summaries.getMethodSummaries().getFlowsForMethod(sm.getSubSignature());
			if (summary != null) {
				for (final MethodFlow flow : summary) {
					if (flow.source().getType() == SourceSinkType.Parameter
							&& flow.sink().getType() == SourceSinkType.Field) {
						assignsToBase = true;
						break;
					}
				}
			}
		}
		if (!assignsToBase && interfaceMap.get(className) != null) {
			for (final String interfaceName : interfaceMap.get(className)) {
				if (assignsToBaseStubDroid(interfaceName, sm)) {
					assignsToBase = true;
					break;
				}
			}
		}
		// Under- or Over-Approximate?
		if (!assignsToBase && Parameters.getInstance().isOverapproximateStubDroid() && summaries == null
				&& interfaceMap.get(className) == null) {
			assignsToBase = true;
		}
		return assignsToBase;
	}

	/*
	 * Assigns to parameter
	 */
	public static boolean assignsToParameter(SootMethod sm, int parameterNumber) {
		return assignsToParameterStubDroid(sm.getDeclaringClass(), sm, parameterNumber);
	}

	private static boolean assignsToParameterStubDroid(SootClass sc, SootMethod sm, int parameterNumber) {
		return assignsToParameterStubDroid(sc.getName(), sm, parameterNumber);
	}

	private static boolean assignsToParameterStubDroid(String className, SootMethod sm, int parameterNumber) {
		// StubDroid only (others are handled implicitly)
		final ClassMethodSummaries summaries = readStubDroidSummaries(className);

		boolean assignsToParameter = false;
		if (summaries != null) {
			// Check if assigns to parameter
			final Set<MethodFlow> summary = summaries.getMethodSummaries().getFlowsForMethod(sm.getSubSignature());
			if (summary != null) {
				for (final MethodFlow flow : summary) {
					if ((flow.source().getType() == SourceSinkType.Parameter
							|| flow.source().getType() == SourceSinkType.Field)
							&& flow.sink().getType() == SourceSinkType.Parameter
							&& flow.sink().getParameterIndex() == parameterNumber) {
						assignsToParameter = true;
						break;
					}
				}
			}
		}
		if (!assignsToParameter && interfaceMap.get(className) != null) {
			for (final String interfaceName : interfaceMap.get(className)) {
				if (assignsToParameterStubDroid(interfaceName, sm, parameterNumber)) {
					assignsToParameter = true;
					break;
				}
			}
		}
		// Under- or Over-Approximate?
		if (!assignsToParameter && Parameters.getInstance().isOverapproximateStubDroid() && summaries == null
				&& interfaceMap.get(className) == null) {
			assignsToParameter = true;
		}
		return assignsToParameter;
	}

	/*
	 * StudDroid environmental classes
	 */
	private static ClassMethodSummaries readStubDroidSummaries(String className) {
		// Read summaries file or load result
		if (summariesMap.containsKey(className)) {
			return summariesMap.get(className);
		} else {
			ClassMethodSummaries summaries = readStubDroidSummaries(className, STUB_DROID_DIR);
			if (summaries == null) {
				summaries = readStubDroidSummaries(className, STUB_DROID_MANUAL_DIR);
			}
			return summaries;
		}
	}

	private static ClassMethodSummaries readStubDroidSummaries(String className, File directory) {
		final File summariesFile = new File(directory, className + ".xml");
		try {
			if (summariesFile.exists()) {
				final SummaryReader sr = new SummaryReader();
				final ClassMethodSummaries summaries = new ClassMethodSummaries(className);
				sr.read(summariesFile, summaries);
				summariesMap.put(className, summaries);
				interfaceMap.put(className, summaries.getInterfaces());
				return summaries;
			} else {
				Log.note("No summaries file for: " + className);
			}
		} catch (XMLStreamException | SummaryXMLException | IOException e) {
			Log.warning("Could not read summaries file: " + summariesFile.getAbsolutePath());
		}
		return null;
	}
}