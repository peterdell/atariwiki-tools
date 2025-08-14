package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.log;
import static com.wudsn.productions.www.atariwiki.Utilities.logException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import com.wudsn.productions.www.atariwiki.Markup.Format;

public class AtariWikiChecker {

	private static MarkupElement readFile(File inputFileFolder) {
		MarkupElement rootElement;
		try {
			rootElement = MarkupIO.read(new File(inputFileFolder, "index.md"), Format.MD);

		} catch (IOException ex) {
			logException(ex);
			return null;
		}

		File[] folders = new File(inputFileFolder, "attachments").listFiles();
		for (File folder : folders) {
			if (folder.isFile()) {
				String fileName = folder.getName();
				fileName = fileName.substring(0, fileName.length() - 4);
				rootElement.addAttachment(fileName);
			}
		}

		return rootElement;

	}

	public void run(String[] args) {

		File inputFolder = new File("C:\\jac\\system\\WWW\\Programming\\Repositories\\atariwiki");

		final String filterPattern = ""; // "EASMD"; // "Teil10";
		FileFilter folderFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				if (file.getName().equals(".git")) {
					return false;
				}
				if (!file.isDirectory()) {
					return false;
				}

				if (filterPattern.isEmpty() || file.getName().indexOf(filterPattern) >= 0) {
					return true;
				}

				return false;

			};
		};
		if (!inputFolder.exists()) {
			Utilities.logError("Input folder %s' does not exist.", inputFolder.getAbsolutePath());
			return;
		}
		File[] inputFileFolders = inputFolder.listFiles(folderFilter);
		List<MarkupElement> rootElements = new ArrayList<MarkupElement>();

		for (File inputFileFolder : inputFileFolders) {
			Utilities.logInfo("Processing '%s'.", inputFileFolder.getAbsolutePath());
			MarkupElement rootElement = readFile(inputFileFolder);
			if (rootElement != null) {
				rootElements.add(rootElement);
			}

		}

//		checkConsistency(elements);
	}

//
//	private void checkConsistency(List<MarkupElement> elements) {
//		for (MarkupElement rootElement : elements) {
//
//		}
//	};

	public static void main(String[] args) {
		new AtariWikiChecker().run(args);
		log("Done.");

	}

}
