package com.wudsn.productions.www.atariwiki;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public abstract class Utilities {

	private Utilities() {
	};

	public static boolean DEBUG = false;
	
	private static final Logger consoleLogger=new PrintStreamLogger(System.out, System.err);

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
		consoleLogger.logInfo(message, args);
	}

	public static void logWarning(String message, Object... args) {
		consoleLogger.logWarning(message, args);

	}

	public static void logError(String message, Object... args) {
		consoleLogger.logError(message, args);
	}

	public static void logException(Exception exception) {
		consoleLogger.logException(exception);

	}

}
