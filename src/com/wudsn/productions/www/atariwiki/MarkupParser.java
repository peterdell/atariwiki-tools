package com.wudsn.productions.www.atariwiki;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import com.wudsn.productions.www.atariwiki.Markup.Format;

// Currently only parses JSPWiki format.

public class MarkupParser {

	private Markup.Format format;

	private MarkupElement rootElement;
	private Stack<MarkupElement> stack;
	private MarkupElement currentElement;

	public MarkupParser(Markup.Format format) {
		this.format = format;
	}

	private void init() {
		rootElement = new MarkupElement();
		stack = new Stack<MarkupElement>();
		currentElement = null;

		pushElement(rootElement);
	}

	private void pushElement(MarkupElement element) {
		stack.push(element);
		currentElement = element;
	}

	private void popElement() {
		stack.pop();
		currentElement = stack.peek();
	}

	private MarkupElement addChildElement(MarkupElement.Type type, String content) {
		MarkupElement element = currentElement.addChild(type);
		element.setContent(content);
		return element;
	}

	private void addLink(String link) {
		// ![drawing](drawing.jpg)

		String description = "";
		String url = "";

		if (link.startsWith("{Image")) {
			int index = link.indexOf("src='") + 5;
			url = link.substring(index);
			index = url.indexOf("'");
			url = url.substring(0, index);
			addChildElement(MarkupElement.Type.IMAGE, description).setURL(url);

			return;
		}

		int index = link.indexOf("|");
		if (index < 0) {
			description = "";
			url = link;
		} else {
			description = link.substring(0, index);
			url = link.substring(index + 1);
		}
		
		if (!url.contains("://")) {
			url=AtariWikiConverter.cleanFileName(url);
		}
		addChildElement(MarkupElement.Type.LINK, description).setURL(url);
	}

	private void addChildElementWithFormat(String content) {

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < content.length(); i++) {
			String prefix = content.substring(i);
			if (prefix.startsWith("[")) {
				int endIndex = prefix.indexOf("]");
				if (endIndex > 1) {
					String link = prefix.substring(1, endIndex).trim();
					addChildElement(MarkupElement.Type.TEXT, builder.toString());
					builder.setLength(0);
					addLink(link);
					i += endIndex;
				}
			} else {
				builder.append(content.charAt(i));
			}

		}
		if (builder.length() > 0) {
			addChildElement(MarkupElement.Type.TEXT, builder.toString());
		}
	}

	private void addChildElementWithLines(MarkupElement.Type type, String content) {

		addChildElement(type, content);

		// Remove single trailing \\
		content = content.trim();
		if (content.endsWith("\\\\")) {
			content = content.substring(0, content.length() - 2);
		}

		String[] lines = content.split("\\\\");
		for (String line : lines) {
			if (!line.isBlank()) {
				addChildElementWithFormat(line);
			}
		}

		addNewLine();
	}

	private void addNewLine() {
		addChildElement(MarkupElement.Type.BR, "");
	}

	private void consumeLine(String line) {
		String codeBlockStart = "";
		String codeBlockEnd = "";
		String separator = "";
		if (format == Format.JSP) {
			codeBlockStart = "{{{";
			codeBlockEnd = "}}}";
			separator = "---";
		} else {
			return;
		}

		String condensedLine = line.replace(" ", "");
		if (currentElement.getType() == MarkupElement.Type.CODE) {

			if (line.startsWith(codeBlockEnd)) {
				popElement();
				return;
			} else {
				if (currentElement.getContent().isBlank()) {
					currentElement.setContent(line);
				} else {
					currentElement.setContent(currentElement.getContent() + MarkupElement.NEW_LINE + line);
				}
				return;
			}
		}

		// log(line);

		if (condensedLine.startsWith(separator)) {
			addChildElement(MarkupElement.Type.SEP, "---");
			return;
		}

		if (line.startsWith("!!!")) {
			addChildElementWithLines(MarkupElement.Type.H1, line.substring(3).trim());
			return;
		}
		if (line.startsWith("!!")) {
			addChildElementWithLines(MarkupElement.Type.H2, line.substring(2).trim());
			return;
		}
		if (line.startsWith("!")) {
			addChildElementWithLines(MarkupElement.Type.H3, line.substring(1).trim());
			return;
		}

		if (line.startsWith("* ")) {
			addChildElementWithLines(MarkupElement.Type.UL, line.substring(2).trim());
			return;
		}
		if (line.startsWith("# ")) {
			addChildElementWithLines(MarkupElement.Type.OL, line.substring(2).trim());
			return;
		}

		if (condensedLine.startsWith("[{TableOfContents}]")) {
			addChildElement(MarkupElement.Type.TOC, "Table of Contents");
			return;
		}

		if (line.startsWith(codeBlockStart)) {
			MarkupElement childElemment = addChildElement(MarkupElement.Type.CODE, "");
			pushElement(childElemment);
			return;
		}

		addChildElementWithLines(MarkupElement.Type.LINE, line);

	}

	public MarkupElement parse(Reader reader) throws IOException {

		init();

		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			while (line != null) {
				consumeLine(line);
				line = bufferedReader.readLine();
			}

		} catch (java.io.IOException ex) {
			throw ex;

		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					// Ignore
				}
			}

		}
		return rootElement;
	}
}