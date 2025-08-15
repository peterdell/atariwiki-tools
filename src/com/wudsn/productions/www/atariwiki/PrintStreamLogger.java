package com.wudsn.productions.www.atariwiki;

import java.io.PrintStream;

public class PrintStreamLogger implements Logger {

	private PrintStream out;
	private PrintStream err;

	public PrintStreamLogger(PrintStream out, PrintStream err) {
		this.out = out;
		this.err = err;
	}

	private void log(String message) {
		out.println(message);
		out.flush();
	}

	@Override
	public void logInfo(String message, Object... args) {
		message = "INFO   : " + message.formatted(args);
		log(message);
	}

	@Override
	public void logWarning(String message, Object... args) {
		message = "WARNING: " + message.formatted(args);
		err.println(message);
		err.flush();
	}

	@Override

	public void logError(String message, Object... args) {
		message = "ERROR  : " + message.formatted(args);
		err.println(message);
		err.flush();
	}

	@Override
	public void logException(Exception exception) {
		logError("%s", exception.getMessage());
		Throwable cause = exception.getCause();
		while (cause != null) {
			logError("CAUSE  : %s", cause.getMessage());

			cause = cause.getCause();
		}
		// System.exit(1);
	}

	@Override
	public void newLine() {
		log("");		
	}

}
