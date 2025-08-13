package com.wudsn.productions.www.atariwiki;

import java.io.PrintWriter;

public class MarkupWriter {

	private PrintWriter printWriter;

	public MarkupWriter(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	private void write(String content) {
		printWriter.write(content);
	}

	private void writeln(String content) {
		write(content + "\n");
	}

	public void writeElement(MarkupElement element) {
		switch (element.getType()) {

		case ROOT: {
			return;
		}
		case SEP: {
			writeln("---");
			return;
		}
		case BR: {
			writeln("  ");
			return;
		}

		case H1: {
			write("# ");
			return;
		}
		case H2: {
			write("## ");
			return;
		}
		case H3: {
			write("### ");
			return;
		}

		case OL: {
			write("1. ");
			return;
		}
		case UL: {
			write("- ");
			return;
		}

		case CODE: {
			writeln("```");
			writeln(element.getContent());
			writeln("```");
			return;
		}

		case TOC: {
			return;
		}

		case LINE: {
			return;
		}

		case TEXT: {
			write(element.getContent());
			return;
		}

		case LINK: {
			String description = element.getContent();
			String url = element.getURL();

			if (description.isBlank()) {
				description = url;
			}
			if (!url.contains("://")) {
				// Make Wiki internal links relative file links
				int index = url.lastIndexOf('.');
				String extension = "";
				if (index >= 0) {
					extension = url.substring(index).toLowerCase();
				}
				// Assume known extensions are attachments
				if (!extension.isEmpty()
						&& ".atr,.bin,.car,.zip,.txt,.src,.asm,.rar,.pdf,.cas".indexOf(extension) >= 0) {
					url = "attachments/" + Utilities.encodeURL(url);
				} else {
					if (!extension.isEmpty()) {
						Utilities.log("INFO: Unknown extension " + extension);
					}
					url = url + "/index.md";
				}
			}
			write("[" + description + "](" + url + ")");
			return;
		}

		case IMAGE: {
			String description = element.getContent();
			String url = element.getURL();
			write("!");
			write("[" + description + "]");
			if (!url.contains("://")) {
				// Make Wiki internal links relative file links
				url = "attachments/" + Utilities.encodeURL(url);
			}
			write("(" + url + ")");
			return;
		}
		}
	}

	public void writeElementWithChildren(MarkupElement element) {
		element.visit(new MarkupElementVisitor() {

			@Override
			public void visit(MarkupElement element, int level) {
				MarkupWriter.this.writeElement(element);

			}
		});
	}
}