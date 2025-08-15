package com.wudsn.productions.www.atariwiki;

public class Markup {
	public static String FILE_NAME_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!.+-_$";
	public static String INDEX_MD = "index.md";
	public static String ATTACHMENTS = "attachments";
	public static String ATTACHMENTS_PREFIX = ATTACHMENTS + "/";
	public static int ATTACHMENTS_PREFIX_LENGTH = ATTACHMENTS_PREFIX.length();
	
	public enum Format {
		JSP, MD
	};
}
