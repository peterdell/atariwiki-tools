package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.log;
import static com.wudsn.productions.www.atariwiki.Utilities.logException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.wudsn.productions.www.atariwiki.Markup.Format;

public class AtariWikiConverter {

	private static void convertJSPTextFile(File inputFile, File outputFile) {
		MarkupElement rootElement;
		try {
			rootElement = MarkupIO.read(inputFile, Format.JSP);
		} catch (IOException ex) {
			logException(ex);
			return;
		}

		try {

			MarkupIO.write(rootElement, outputFile);

		} catch (IOException ex) {
			logException(ex);
			return;
		}

	}

	// JSP Wiki handling for Attachments
	private static void convertJSPAttachmentFolder(File inputFileAttachmentsFolder, File outputFileAttachmentsFolder) {
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
				Utilities.logDebug("Reading attachments for '%s' from '%s'.", fileName, folder.getAbsolutePath());
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
					Path targetFilePath = new File(outputFileAttachmentsFolder, fileName).toPath();
					Utilities.logDebug("Copying attachment version '%s' for '%s' from '%s' to '%s'.", versionName,
							fileName, folder.getAbsolutePath(), targetFilePath.toString());
					try {
						outputFileAttachmentsFolder.mkdir();
						Files.copy(new File(folder, versionName).toPath(), targetFilePath,
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ex) {
						logException(ex);
					}
				}
			}
		}
	}

	private void runFile(File inputFile, File inputFileAttachmentsFolder, File outputFile,
			File outputFileAttachmentsFolder) {
		convertJSPTextFile(inputFile, outputFile);
		convertJSPAttachmentFolder(inputFileAttachmentsFolder, outputFileAttachmentsFolder);
	}

	public void run(String[] args) {

		File baseFolder = new File("C:\\jac\\system\\WWW\\Programming\\Repositories");
		File inputFolder = new File(baseFolder, "atariwiki.jsp\\jspwiki");
		File outputFolder = new File(baseFolder, "atariwiki");
		File contentFolder = new File(outputFolder, "content");

		final String filterPattern = ""; // "EASMD"; // "Teil10";
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
		if (!inputFolder.exists()) {
			Utilities.logError("Input folder %s' does not exist.", inputFolder.getAbsolutePath());
			return;
		}
		File[] inputFiles = inputFolder.listFiles(filter);

		MarkupElement toc = new MarkupElement();

		for (File inputFile : inputFiles) {
			String baseName = inputFile.getName();
			baseName = baseName.substring(0, baseName.length() - 4);
			File outputFileFolder = new File(contentFolder, baseName);
			outputFileFolder.mkdir();
			File outputFile = new File(outputFileFolder, "index.md");

			MarkupElement ulElement = toc.addChild(MarkupElement.Type.UL);

			MarkupElement linkElement = ulElement.addChild(MarkupElement.Type.LINK);
			linkElement.setContent(Utilities.decodeURL(baseName));
			linkElement.setURL("content/" + baseName);
			ulElement.getChildren().add(linkElement);

			ulElement.addChild(MarkupElement.Type.BR);

			Utilities.logInfo("Processing '%s'.", inputFile.getAbsolutePath());
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
		try {
			MarkupIO.write(toc, tocFile);
		} catch (IOException ex) {
			Utilities.logException(ex);
			return;
		}
	}

	public static void main(String[] args) {
		new AtariWikiConverter().run(args);
		log("Done.");

	}

}
