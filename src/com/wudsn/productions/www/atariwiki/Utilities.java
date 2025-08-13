package com.wudsn.productions.www.atariwiki;

import java.net.URLDecoder;
import java.net.URLEncoder;

public abstract class Utilities {

	private Utilities() {
	};

	public static String decodeURL(String url) {
		return URLDecoder.decode(url);
	}

	public static String encodeURL(String path) {
		return URLEncoder.encode(path);
	}

	public static void log(String message) {
		System.out.println(message);
	}
}
