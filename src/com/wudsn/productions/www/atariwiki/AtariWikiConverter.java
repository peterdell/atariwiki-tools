package com.wudsn.productions.www.atariwiki;

/*
https://atariwiki.org/wiki/Wiki.jsp?page=TextFormattingRules

Line Formats:
----       = make a horizontal ruler. Extra '-' is ignored.
\\         = force a line break

!heading   = small heading with text 'heading'
!!heading  = medium heading with text 'heading'
!!!heading = large heading with text 'heading'

* text     = make a bulleted list item with 'text'
# text     = make a numbered list item with 'text'

Word Formats:

[link]     = create a hyperlink to an internal WikiPage called 'Link'.
[this is also a link] = create a hyperlink to an internal WikiPage called
             'ThisIsAlsoALink' but show the link as typed with spaces.
[a sample|link] = create a hyperlink to an internal WikiPage called
             'Link', but display the text 'a sample' to the
             user instead of 'Link'.
[link#headline] = create a hyperlink to an internal WikiPage called 'link' and use anchor 'headline'          
~NoLink    = disable link creation for the word in CamelCase.
[1]        = make a reference to a footnote numbered 1.
[#1]       = mark the footnote number 1.
[[link]     = create text '[link]'.


''text''   = print 'text' in italic.
__text__   = print 'text' in bold.
{{text}}   = print 'text' in monospaced font.
[ text|]    = print 'text' underscored (dummy hyperlink)

;term:ex   = make a definition for 'term' with the explanation 'ex'


Example:
https://atariwiki.org/wiki/Wiki.jsp?page=Atari+Calculator

*/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Stack;

public class AtariWikiConverter {

	public static final boolean DEBUG = false;

	
	private static String NEW_LINE = "\n";

	private Element rootElement;

	private Stack<Element> stack;
	private Element currentElement;

	private static void log(String message) {
		System.out.println(message);
	}

	private void init() {
		rootElement = new Element(null, Element.Type.ROOT, "Root");
		stack = new Stack<Element>();
		currentElement = null;

		pushElement(rootElement);
	}

	private void pushElement(Element element) {
		stack.push(element);
		currentElement = element;
	}

	private void popElement() {
		stack.pop();
		currentElement = stack.peek();
	}

	private Element addChildElement(Element element) {
		currentElement.getChildren().add(element);
		return element;
	}

	private Element addChildElement(Element.Type type, String content) {
		return addChildElement(new Element(currentElement, type, content));
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
			addChildElement(Element.Type.IMAGE, description).setURL(url);

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
		addChildElement(Element.Type.LINK, description).setURL(url);
	}

	private void addChildElementWithFormat(String content) {

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < content.length(); i++) {
			String prefix = content.substring(i);
			if (prefix.startsWith("[")) {
				int endIndex = prefix.indexOf("]");
				if (endIndex > 1) {
					String link = prefix.substring(1, endIndex).trim();
					addChildElement(Element.Type.TEXT, builder.toString());
					builder.setLength(0);
					addLink(link);
					i += endIndex;
				}
			} else {
				builder.append(content.charAt(i));
			}

		}
		if (builder.length() > 0) {
			addChildElement(Element.Type.TEXT, builder.toString());
		}
	}

	private void addChildElementWithLines(Element.Type type, String content) {

		Element element = new Element(currentElement, type, content);
		addChildElement(element);

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

		// popElement();
	}

	private void addNewLine() {
		addChildElement(Element.Type.BR, "");
	}

	private void consumeLine(String line) {
		String condensedLine = line.replace(" ", "");
		if (currentElement.getType() == Element.Type.CODE) {

			if (line.startsWith("}}}")) {
				popElement();
				return;
			} else {
				if (currentElement.getContent().isBlank()) {
					currentElement.setContent(line);
				} else {
					currentElement.setContent(currentElement.getContent() + NEW_LINE + line);
				}
				return;
			}
		}

		// log(line);

		if (condensedLine.startsWith("---")) {
			addChildElement(Element.Type.SEP, "---");
			return;
		}

		if (line.startsWith("!!!")) {
			addChildElementWithLines(Element.Type.H1, line.substring(3).trim());
			addNewLine();
			return;
		}
		if (line.startsWith("!!")) {
			addChildElementWithLines(Element.Type.H2, line.substring(2).trim());
			addNewLine();

			return;
		}
		if (line.startsWith("!")) {
			addChildElementWithLines(Element.Type.H3, line.substring(1).trim());
			addNewLine();
			return;
		}

		if (line.startsWith("* ")) {
			addChildElementWithLines(Element.Type.UL, line.substring(2).trim());
			addNewLine();
			return;
		}
		if (line.startsWith("# ")) {
			addChildElementWithLines(Element.Type.OL, line.substring(2).trim());
			addNewLine();
			return;
		}

		if (condensedLine.startsWith("[{TableOfContents}]")) {
			addChildElement(Element.Type.TOC, "Table of Contents");
			return;
		}

		if (line.startsWith("{{{")) {
			Element childElemment = addChildElement(Element.Type.CODE, "");
			pushElement(childElemment);
			return;
		}

		addChildElementWithLines(Element.Type.LINE, line);
		addNewLine();

	}

	private void printElement(Element element, long level, MarkupWriter markupWriter) {
		// log("Level " + level + " Type=" + element.getType() + " Content=" +
		// element.getContent());
		markupWriter.writeElement(element);
		for (Element childElement : element.getChildren()) {
			printElement(childElement, level + 1, markupWriter);
		}
	}

	private void runTextFile(File inputFile, File outputFile) {
		Reader reader = null;
		BufferedReader bufferedReader = null;

		init();
		try {
			reader = new java.io.FileReader(inputFile);
			bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			while (line != null) {
				consumeLine(line);
				line = bufferedReader.readLine();
			}

		} catch (java.io.IOException ex) {
			ex.printStackTrace();
			return;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}

		PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(outputFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		MarkupWriter markupWriter = new MarkupWriter(printWriter);
		printElement(rootElement, 1, markupWriter);
		printWriter.close();
	}

	private void runAttachmentFolder(File inputFileAttachmentsFolder, File outputFileAttachmentsFolder) {
		if (!inputFileAttachmentsFolder.exists() || !inputFileAttachmentsFolder.isDirectory()) {
			return;
		}
		File[] folders = inputFileAttachmentsFolder.listFiles();
		for (File folder : folders) {
			if (folder.isDirectory() || !folder.getName().endsWith("-dir")) {
				String fileName = Utilities.decodeURL(folder.getName());
				fileName = fileName.substring(0, fileName.length() - 4);
				int extensionIndex = fileName.lastIndexOf('.');
				String extension = fileName.substring(extensionIndex);
				log("Reading attachments for '" + fileName + "' from " + folder.getAbsolutePath());
				File[] versions = folder.listFiles();
				int maxVersionNumber = 0;
				String versionName = "";
				for (File version : versions) {
					versionName = version.getName();
					if (versionName.equals("attachment.properties")) {
						continue;
					}

					int newVersionNumber = Integer.parseInt(versionName.substring(0, versionName.indexOf('.')));
					if (newVersionNumber > maxVersionNumber) {
						maxVersionNumber = newVersionNumber;
					}
				}
				if (maxVersionNumber > 0) {
					versionName = Integer.toString(maxVersionNumber) + extension;
					log("Copying attachment version '" + versionName + "' for '" + fileName + "' from "
							+ folder.getAbsolutePath() + " to " + outputFileAttachmentsFolder.getAbsolutePath());
					try {
						outputFileAttachmentsFolder.mkdir();
						Files.copy(new File(folder, versionName).toPath(),
								new File(outputFileAttachmentsFolder, fileName).toPath(),
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void runFile(File inputFile, File inputFileAttachmentsFolder, File outputFile,
			File outputFileAttachmentsFolder) {
		runTextFile(inputFile, outputFile);
		runAttachmentFolder(inputFileAttachmentsFolder, outputFileAttachmentsFolder);
	}

	public void run(String[] args) {

		File inputFolder = new File("C:\\jac\\system\\Atari800\\Programming\\Repositories\\atariwiki.jsp\\jspwiki");
		File outputFolder = new File("C:\\jac\\system\\Atari800\\Programming\\Repositories\\atariwiki.web");
		File contentFolder = new File(outputFolder, "content");

		final String filterPattern = "EASMD"; // "Teil10";
		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.getName().toLowerCase().endsWith(".txt")) {
					if (filterPattern.isEmpty()) {
						return true;
					} else {
						if (pathname.getName().indexOf(filterPattern) >= 0) {
							return true;
						}
					}
					return false;
				}
				return false;
			};
		};
		File[] inputFiles = inputFolder.listFiles(filter);

		Element toc = new Element(null, Element.Type.ROOT, "");

		for (File inputFile : inputFiles) {
			String baseName = inputFile.getName();
			baseName = baseName.substring(0, baseName.length() - 4);
			File outputFileFolder = new File(contentFolder, baseName);
			outputFileFolder.mkdir();
			File outputFile = new File(outputFileFolder, "index.md");

			Element ulElement = new Element(toc, Element.Type.UL, "");
			toc.getChildren().add(ulElement);

			Element linkElement = new Element(ulElement, Element.Type.LINK, "");
			linkElement.setContent(Utilities.decodeURL(baseName));
			linkElement.setURL("content/" + baseName);
			ulElement.getChildren().add(linkElement);

			Element brElement = new Element(ulElement, Element.Type.BR, "");
			ulElement.getChildren().add(brElement);

			log("INFO: Processing " + inputFile.getAbsolutePath() + ".");
			File inputAttachmentsFolder = new File(inputFolder, "attachments");
			File outputFileAttachmentsFolder = new File(outputFileFolder, "attachments");

			File inputFileAttachmentsFolder = new File(inputAttachmentsFolder, baseName);
			try {
				runFile(inputFile, inputFileAttachmentsFolder, outputFile, outputFileAttachmentsFolder);
			} catch (RuntimeException ex) {
				linkElement.setContent(linkElement.getContent() + " ERROR: " + ex.getMessage());
				ex.printStackTrace();
			}

		}
		File tocFile = new File(outputFolder, "TOC.md");
		PrintWriter printWriter;
		MarkupWriter tocWriter;
		try {
			printWriter = new PrintWriter(tocFile);
			tocWriter = new MarkupWriter(printWriter);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		printElement(toc, 1, tocWriter);
		printWriter.close();

	}

	public static void main(String[] args) {
		new AtariWikiConverter().run(args);
		log("Done.");

	}

}
