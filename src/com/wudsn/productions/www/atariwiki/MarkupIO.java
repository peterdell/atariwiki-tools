package com.wudsn.productions.www.atariwiki;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import com.wudsn.productions.www.atariwiki.Markup.Format;

public abstract class MarkupIO {

	private MarkupIO() {

	}

	public static MarkupElement read(File inputFile, Format format) throws IOException {
		Reader reader = null;
		MarkupElement rootElement = null;
		try {
			reader = new java.io.FileReader(inputFile);
			MarkupParser markupParser = new MarkupParser(format);
			rootElement = markupParser.parse(reader);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		return rootElement;
	}

	public static void write(MarkupElement element, File outputFile) throws IOException {
		PrintWriter printWriter;

		printWriter = new PrintWriter(outputFile);
		MarkupWriter markupWriter = new MarkupWriter(printWriter);
		markupWriter.writeElementWithChildren(element);
		printWriter.close();
	}
}
