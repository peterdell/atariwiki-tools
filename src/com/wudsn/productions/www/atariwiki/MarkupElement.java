package com.wudsn.productions.www.atariwiki;

import java.util.ArrayList;
import java.util.List;

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
	private Type type;
	private String content;
	private String url;
	private List<MarkupElement> children;
	private List<Attachment> attachments;

	public MarkupElement() {
		this(null, Type.ROOT);
	}

	protected MarkupElement(MarkupElement parent, Type type) {
		this.parent = parent;
		this.type = type;
		this.content = "";
		this.url = "";
		this.children = new ArrayList<MarkupElement>();
		this.attachments = new ArrayList<Attachment>();
	}

	public MarkupElement getParent() {
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
		attachments.add(attachment);
		return attachment;
	}

	public List<Attachment> getAttachments() {
		return attachments;
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