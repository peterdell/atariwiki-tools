package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.*;

import java.io.File;

import com.wudsn.productions.www.atariwiki.jsp.AtariWikiConverter;

public class AtariWikiTool {

	public static void main(String[] args) {

		boolean convert = false;
		boolean check = false;
		String baseFolderPath = "";
		File baseFolder = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("--convert")) {
				convert = true;
			} else if (arg.equals("--check")) {
				convert = true;
			} else {
				if (baseFolder == null) {
					baseFolderPath = arg;

					baseFolder = new File(baseFolderPath);
					if (!baseFolder.exists() || !baseFolder.isDirectory()) {
						Utilities.logError("Base folder %s' does not exist.", baseFolder.getAbsolutePath());
						return;
					}
				}
			}
		}

		boolean parameterError = false;
		if (!convert && !check) {
			parameterError = true;
		}
		if (baseFolder == null) {
			parameterError = true;
		}
		if (parameterError) {
			Utilities.logInfo(
					"Usage: [--convert] [--check] <base folder> containing 'atariwiki.jsp' and 'atariwiki' folders.");
			return;
		}

		logInfo("AtariWikiTool - 2025-08-15");

		logInfo("Base Folder: %s", baseFolderPath);
		logInfo("Convert: %b", convert);
		logInfo("Check: %b", convert);
		
		if (convert) {
			new AtariWikiConverter().run(baseFolder);
			log("Conversion Done.");
		}
		if (check) {
			new AtariWikiChecker().run(baseFolder);
		}

	}
}
