package com.wudsn.productions.www.atariwiki;

import java.util.ArrayList;
import java.util.*;

public class MarkupElement {

	public enum Type {
		ROOT, SEP, BR, H1, H2, H3, OL, UL, CODE, TOC, LINE, TEXT, LINK, IMAGE
	};

	public static String NEW_LINE = "\n";

	public static class Attachment {
		private MarkupElement parent;
		private String fileName;

		Attachment(MarkupElement parent) {
			this.parent = parent;
		}

		public MarkupElement getParent() {
			return parent;
		}

		public void setfileName(String fileName) {
			this.fileName = fileName;
		}

		public String getfileName() {
			return fileName;
		}
	};

	private MarkupElement parent;
	private int lineNumber;
	private Type type;
	private String content;
	private String url;
	private List<MarkupElement> children;
	private Map<String, Attachment> attachments;

	public MarkupElement() {
		this(null, Type.ROOT);
	}

	protected MarkupElement(MarkupElement parent, Type type) {
		this.parent = parent;
		this.type = type;
		this.content = "";
		this.url = "";
		this.children = new ArrayList<MarkupElement>();
		this.attachments = new TreeMap<String, Attachment>();
	}

	public MarkupElement getParent() {
		return parent;
	}

	public MarkupElement getRoot() {
		MarkupElement result = this;

		while (result.getParent() != null) {
			result = result.getParent();
		}
		return result;
	}

	public int getLineNumber() {
		return lineNumber;
	}
	
	public void setLineNumber(int lineNumber) {
		this.lineNumber=lineNumber;
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

	public MarkupElement addChild(Type type) {
		MarkupElement element = new MarkupElement(this, type);
		children.add(element);
		return element;
	}

	public List<MarkupElement> getChildren() {
		return children;
	}

	public Attachment addAttachment(String fileName) {
		Attachment attachment = new Attachment(this);
		attachment.setfileName(fileName);
		attachments.put(fileName, attachment);
		return attachment;
	}

	public Map<String, Attachment> getAttachments() {
		return attachments;
	}

	public Attachment getAttachment(String fileName) {
		return attachments.get(fileName);
	}

	private void visit(MarkupElementVisitor elementVisitor, int level) {
		elementVisitor.visit(this, level);
		for (MarkupElement childElement : getChildren()) {
			childElement.visit(elementVisitor, level + 1);
		}
	}

	public void visit(MarkupElementVisitor elementVisitor) {
		visit(elementVisitor, 1);
	}
}