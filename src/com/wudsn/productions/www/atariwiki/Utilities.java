package com.wudsn.productions.www.atariwiki;

import java.net.URLDecoder;
import java.net.URLEncoder;

public abstract class Utilities {

	private Utilities() {
	};

	public static boolean DEBUG = true;

	public static String decodeURL(String url) {
		return URLDecoder.decode(url);
	}

	public static String encodeURL(String path) {
		return URLEncoder.encode(path);
	}

	public static void log(String message) {
		System.out.println(message);
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

	public static void logError(String message, Object... args) {
		message = "ERROR: " + message.formatted(args);
		log(message);
	}
}
