package de.foellix.aql.jicer.callgraphenhancer;

public class Task {
	protected String callerClassName;
	protected String callerMethodName;
	protected int callerMethodParameters;

	protected String calleeClassName;
	protected String calleeMethodName;
	protected int calleeMethodParameters;

	public Task(String callerClassName, String callerMethodName, int callerMethodParameters, String calleeClassName,
			String calleeMethodName, int calleeMethodParameters) {
		this.callerClassName = callerClassName;
		this.callerMethodName = callerMethodName;
		this.callerMethodParameters = callerMethodParameters;

		this.calleeClassName = calleeClassName;
		this.calleeMethodName = calleeMethodName;
		this.calleeMethodParameters = calleeMethodParameters;
	}
}