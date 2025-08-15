package com.wudsn.productions.www.atariwiki;

public interface Logger {

	public void logInfo(String message, Object... args);

	public void logWarning(String message, Object... args);

	public void logError(String message, Object... args);

	public void logException(Exception exception);

	public void newLine();
}
