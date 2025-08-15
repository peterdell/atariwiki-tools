package com.wudsn.productions.www.atariwiki;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public abstract class Utilities {

	private Utilities() {
	};

	public static boolean DEBUG = false;

	public static String decodeURL(String url) {
		return URLDecoder.decode(url, StandardCharsets.UTF_8);
	}

	public static String encodeURL(String path) {
		return URLEncoder.encode(path, StandardCharsets.UTF_8);
	}

	public static void log(String message) {
		System.out.println(message);
		System.out.flush();
	}

	public static void logDebug(String message, Object... args) {
		if (DEBUG) {
			message = "DEBUG: " + message.formatted(args);
			log(message);
		}
	}

	public static void logInfo(String message, Object... args) {
		message = "INFO: " + message.formatted(args);
		log(message);
	}

	public static void logWarning(String message, Object... args) {
		message = "WARNING: " + message.formatted(args);
		System.err.println(message);
		System.err.flush();
	}

	public static void logError(String message, Object... args) {
		message = "ERROR: " + message.formatted(args);
		System.err.println(message);
		System.err.flush();
	}

	public static void logException(Exception exception) {
		logError("%s", exception.getMessage());
		Throwable cause = exception.getCause();
		while (cause != null) {
			logError("CAUSE: %s", cause.getMessage());

			cause = cause.getCause();
		}
		// System.exit(1);
	}

}
