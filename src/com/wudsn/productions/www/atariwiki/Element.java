package com.wudsn.productions.www.atariwiki;

import java.util.ArrayList;
import java.util.List;

public class Element {

	public enum Type {
		ROOT, SEP, BR, H1, H2, H3, OL, UL, CODE, TOC, LINE, TEXT, LINK, IMAGE
	};

	private Element parent;
	private Type type;
	private String content;
	private String url;
	private List<Element> children;

	public Element(Element parent, Type type, String content) {
		this.parent = parent;
		this.type = type;
		this.content = content;
		this.url = "";
		this.children = new ArrayList<Element>();
		// System.out.println("Type=" + type + " Content=" + content);
	}

	public Element getParent() {
		return parent;
	}

	public Type getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public List<Element> getChildren() {
		return children;
	}
}