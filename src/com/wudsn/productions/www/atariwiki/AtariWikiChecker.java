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

	private FileFilter folderFilter;

	public AtariWikiChecker() {
		folderFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {

				if (!file.isDirectory()) {
					return false;
				}
				if (file.getName().equals(".git")) {
					return false;
				}
				if (file.getName().equals(Markup.ATTACHMENTS)) {
					return false;
				}

//				if (filterPattern.isEmpty() || file.getName().indexOf(filterPattern) >= 0) {
//					return true;
//				}

				return true;

			};
		};

	}

	private static MarkupElement readFile(File inputFile) {
		MarkupElement rootElement;
		try {
			rootElement = MarkupIO.read(inputFile, Format.MD);

		} catch (IOException ex) {
			logException(ex);
			return null;
		}

		File[] attachementFiles = new File(inputFile.getParentFile(), Markup.ATTACHMENTS).listFiles();
		if (attachementFiles != null) {
			for (File file : attachementFiles) {
				if (file.isFile()) {
					String fileName = file.getName();
					rootElement.addAttachment(fileName);
				}
			}
		}

		return rootElement;

	}

	private void runFolder(File rootFolder, File inputFolder, List<MarkupElement> rootElements) {
		File[] inputFileFolders = inputFolder.listFiles(folderFilter);

		for (File inputFileFolder : inputFileFolders) {
//			Utilities.logInfo("Processing '%s'.", inputFileFolder.getAbsolutePath());
			File inputFile = new File(inputFileFolder, Markup.INDEX_MD);
			if (inputFile.exists()) {
				MarkupElement rootElement = readFile(inputFile);
				if (rootElement != null) {
					rootElement.setURL(inputFile.getAbsolutePath());
					rootElements.add(rootElement);
				}
			}
			runFolder(rootFolder, inputFileFolder, rootElements);

		}
	}

	public void run(String[] args) {

		File rootFolder = new File("C:\\jac\\system\\WWW\\Programming\\Repositories\\atariwiki");

//		final String filterPattern = ""; // "EASMD"; // "Teil10";
		if (!rootFolder.exists()) {
			Utilities.logError("Root folder %s' does not exist.", rootFolder.getAbsolutePath());
			return;
		}
		Utilities.logInfo("Processing root folder '%s'.", rootFolder.getAbsolutePath());
		List<MarkupElement> rootElements = new ArrayList<MarkupElement>();

		runFolder(rootFolder, rootFolder, rootElements);

		MarkupChecker checker = new MarkupChecker();
		checker.validateLinks = true;
		checker.validateHttpLinks = false;
		
		log("Checking consistency of " + rootElements.size() + " root elements.");
		checker.checkConsistency(rootFolder, rootElements);
	}

	public static void main(String[] args) {
		new AtariWikiChecker().run(args);
		log("Done.");

	}

}
