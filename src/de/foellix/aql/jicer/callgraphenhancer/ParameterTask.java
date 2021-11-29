package de.foellix.aql.jicer.callgraphenhancer;

public class ParameterTask extends Task {
	protected String parameterType;
	protected int parameterNumber;

	public ParameterTask(String callerClassName, String callerMethodName, int callerMethodParameters,
			String parameterType, int parameterNumber, String calleeClassName, String calleeMethodName,
			int calleeMethodParameters) {
		super(callerClassName, callerMethodName, callerMethodParameters, calleeClassName, calleeMethodName,
				calleeMethodParameters);
		this.parameterType = parameterType;
		this.parameterNumber = parameterNumber;
	}
}
