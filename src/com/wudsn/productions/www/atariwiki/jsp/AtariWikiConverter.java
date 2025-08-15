package com.wudsn.productions.www.atariwiki.jsp;

import static com.wudsn.productions.www.atariwiki.Utilities.logException;
import static com.wudsn.productions.www.atariwiki.Utilities.logWarning;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.wudsn.productions.www.atariwiki.Markup;
import com.wudsn.productions.www.atariwiki.Markup.Format;
import com.wudsn.productions.www.atariwiki.MarkupElement;
import com.wudsn.productions.www.atariwiki.MarkupIO;
import com.wudsn.productions.www.atariwiki.Utilities;

public class AtariWikiConverter {

	private static Properties cleanNamesMap = new Properties();

	// JSP Wiki handling for Attachments
	private static void convertJSPAttachmentFolder(File inputFileAttachmentsFolder, File outputFileAttachmentsFolder) {
		if (!inputFileAttachmentsFolder.exists() || !inputFileAttachmentsFolder.isDirectory()) {
			return;
		}
		File[] folders = inputFileAttachmentsFolder.listFiles();
		for (File folder : folders) {
			if (folder.isDirectory() || !folder.getName().endsWith("-dir")) {
				String fileName = folder.getName();
				fileName = fileName.substring(0, fileName.length() - 4);
				fileName = cleanFileName(fileName);
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

	private MarkupElement convertJSPFile(File inputFile, String readableName, File inputFileAttachmentsFolder,
			File outputFile, File outputFileAttachmentsFolder) {
		MarkupElement rootElement;
		try {
			rootElement = MarkupIO.read(inputFile, Format.JSP);
			rootElement.setContent(readableName);
			rootElement.setURL(inputFile.toPath().toString());
		} catch (IOException ex) {
			logException(ex);
			return null;
		}

		try {

			MarkupIO.write(rootElement, outputFile);

		} catch (IOException ex) {
			logException(ex);
			return null;
		}

		convertJSPAttachmentFolder(inputFileAttachmentsFolder, outputFileAttachmentsFolder);
		return rootElement;
	}

	public void run(File baseFolder) {



		File inputFolder = new File(baseFolder, "atariwiki.jsp");
		File textInputFolder = new File(inputFolder, "p/web/www-data/jspwiki");
		File attachmentInputFolder = new File(inputFolder, "jspwiki/Attachments");
		File outputFolder = new File(baseFolder, "atariwiki");
		File contentFolder = new File(outputFolder, "content");

		final String filterPattern = ""; // "EASMD"; // "Teil10";
		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				if (file.getName().toLowerCase().endsWith(".txt")) {
					if (filterPattern.isEmpty()) {
						return true;
					} else {
						if (file.getName().indexOf(filterPattern) >= 0) {
							return true;
						}
					}
					return false;
				}
				return false;
			};
		};
		if (!textInputFolder.exists()) {
			Utilities.logError("Input folder %s' does not exist.", textInputFolder.getAbsolutePath());
			return;
		}
		File[] inputFiles = textInputFolder.listFiles(filter);
		List<MarkupElement> elements = new ArrayList<MarkupElement>();
		MarkupElement toc = new MarkupElement();

		for (File inputFile : inputFiles) {
			String baseName = inputFile.getName();
			baseName = baseName.substring(0, baseName.length() - 4);
			String cleanBaseName = cleanFileName(baseName);
			String readableName = Utilities.decodeURL(baseName);
			File outputFileFolder = new File(contentFolder, cleanBaseName);
			outputFileFolder.mkdir();
			File outputFile = new File(outputFileFolder, "index.md");

			MarkupElement ulElement = toc.addChild(MarkupElement.Type.UL);

			MarkupElement linkElement = ulElement.addChild(MarkupElement.Type.LINK);
			linkElement.setContent(readableName);
			linkElement.setURL(cleanBaseName + "/index.md");

			ulElement.addChild(MarkupElement.Type.BR);

			Utilities.logInfo("Converting '%s' to '%s'.", inputFile.getAbsolutePath(), cleanBaseName);
			File outputFileAttachmentsFolder = new File(outputFileFolder, "attachments");

			File inputFileAttachmentsFolder = new File(attachmentInputFolder, baseName + "-att");
			try {
				MarkupElement element = convertJSPFile(inputFile, readableName, inputFileAttachmentsFolder, outputFile,
						outputFileAttachmentsFolder);
				if (element != null) {
					elements.add(element);
				}
			} catch (RuntimeException ex) {
				linkElement.setContent(linkElement.getContent() + " ERROR: " + ex.getMessage());
				ex.printStackTrace();

			}
		}

		File tocFile = new File(contentFolder, "TOC.md");
		try {
			MarkupIO.write(toc, tocFile);
		} catch (IOException ex) {
			Utilities.logException(ex);
			return;
		}

		saveCleanNamesMap(inputFolder);
	}

	private static File getMapFile(File inputFolder) {
		return new File(inputFolder, "AtariWikiConverter.properties");
	}

	public static Properties loadCleanNamesMap(File inputFolder) throws IOException {
		Properties properties = new Properties();
		File mapFile = getMapFile(inputFolder);
		Utilities.logInfo("Loading clean names map from '%s'.", mapFile.getAbsolutePath());
		properties.loadFromXML(new FileInputStream(mapFile));
		return properties;
	}

	private static void saveCleanNamesMap(File inputFolder) {
		try {
			File mapFile = getMapFile(inputFolder);
			Utilities.logInfo("Storing clean names map to '%s'.", mapFile.getAbsolutePath());
			cleanNamesMap.storeToXML(new FileOutputStream(mapFile), "");
		} catch (IOException ex) {
			logException(ex);
		}
	}

	public static String cleanFileName(String fileName) {
		String name = Utilities.decodeURL(fileName);
		name = name.replace("&", "and");
		name = name.replace(" ", "_");
		name = name.replace(",", "");
		name = name.replace("[", "");
		name = name.replace("]", "");
		name = name.replace("(", "");
		name = name.replace(")", "");
		name = name.replace("ü", "ue");
		name = name.replace("u\u0308", "ue");
		name = name.replace("@", "at");
		name = name.replace("©", "c");
		name = name.replace("<", "-");
		name = name.replace(">", "-");
		name = name.replace("=", "-");
		name = name.replace("*", "-");
		name = name.replace("#", "");
		int index = name.indexOf("/");
		if (index >= 0) {
			name = name.substring(index + 1);
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Markup.FILE_NAME_CHARACTERS.indexOf(c) < 0) {
				logWarning("Invalid character '" + c + "' at position " + i + " in '" + name + "'.");
			}
		}
		if (!name.equals(fileName)) {
			cleanNamesMap.put(fileName, name);
		}
		return name;
	}



}
