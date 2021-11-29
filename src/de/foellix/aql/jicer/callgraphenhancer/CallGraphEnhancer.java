package de.foellix.aql.jicer.callgraphenhancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.soot.SootHelper;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver.SootClassNotFoundException;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CallGraphEnhancer {
	private static final String EXCLUDED_PREFIX = "android.support.v4.";
	private final List<Task> tasks;

	public CallGraphEnhancer() {
		this.tasks = new ArrayList<>();

		// (Base-)Tasks
		this.tasks.add(new Task("java.lang.Thread", "start", 0, "java.lang.Runnable", "run", 0));

		// ParameterTasks
		this.tasks.add(new ParameterTask("java.util.concurrent.ThreadPoolExecutor", "submit", 1, "java.lang.Runnable",
				1, "java.lang.Runnable", "run", 0));
		this.tasks.add(new ParameterTask("java.util.concurrent.Executor", "execute", 1, "java.lang.Runnable", 1,
				"java.lang.Runnable", "run", 0));

		// StaticTasks
		this.tasks.add(new Task("android.os.Handler", "dispatchMessage", 1, "android.os.Handler", "handleMessage", 1));
	}

	public void enhance(CallGraph cg, Unit unit) {
		if (unit instanceof Stmt) {
			final Stmt stmt = (Stmt) unit;
			final InstanceInvokeExpr invoke = getInvoke(stmt);
			if (invoke != null) {
				for (final Task task : this.tasks) {
					if (invoke.getBase().getType().toString().equals(task.callerClassName)) {
						if (invoke.getMethod().getName().equals(task.callerMethodName)) {
							if (invoke.getMethod().getParameterCount() == task.callerMethodParameters) {
								try {
									if (task instanceof ParameterTask) {
										enhanceParameter(cg, stmt, invoke, (ParameterTask) task);
									} else {
										enhanceBase(cg, stmt, invoke, task);
									}
								} catch (final SootClassNotFoundException e) {
									continue;
								}
							}
						}
					}
				}
			}
		}
	}

	private void enhanceParameter(CallGraph cg, Stmt stmt, InstanceInvokeExpr invoke, ParameterTask task) {
		if (invoke.getArgCount() >= task.parameterNumber) {
			final SootClass actualClass = Scene.v()
					.loadClassAndSupport(invoke.getArg(task.parameterNumber - 1).getType().toString());
			final SootClass parameterTypeClass = Scene.v().loadClassAndSupport(task.parameterType);
			if (SootHelper.getAllAccessibleClassesAndInterfaces(actualClass).contains(parameterTypeClass)) {
				enhanceBase(cg, stmt, invoke, task);
			}
		}
	}

	private void enhanceBase(CallGraph cg, Stmt stmt, InstanceInvokeExpr invoke, Task task) {
		final Set<SootMethod> candidates = SootHelper.findMethods(task.calleeMethodName, task.calleeMethodParameters);
		for (final SootMethod candidate : candidates) {
			final SootClass class2 = candidate.getDeclaringClass();
			if (!class2.getName().startsWith(EXCLUDED_PREFIX)) {
				final SootClass object2 = Scene.v().loadClassAndSupport(task.calleeClassName);
				if (SootHelper.getAllAccessibleClassesAndInterfaces(class2).contains(object2)) {
					addEdge(cg, stmt, candidate);
				}
			}
		}
	}

	private void addEdge(CallGraph cg, Stmt stmt, SootMethod target) {
		Log.msg("Over-approximating callgraph edge: " + stmt + " -> " + target, Log.DEBUG);
		cg.addEdge(new Edge(SootHelper.getMethod(stmt), stmt, target));
	}

	private InstanceInvokeExpr getInvoke(Stmt stmt) {
		if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			return (InstanceInvokeExpr) stmt.getInvokeExpr();
		}
		return null;
	}
}