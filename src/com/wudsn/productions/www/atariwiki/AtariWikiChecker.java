package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.log;
import static com.wudsn.productions.www.atariwiki.Utilities.logException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.wudsn.productions.www.atariwiki.Markup.Format;
import com.wudsn.productions.www.atariwiki.MarkupElement.Type;

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

		File[] folders = new File(inputFile.getParentFile(), Markup.ATTACHMENTS).listFiles();
		if (folders != null) {
			for (File folder : folders) {
				if (folder.isFile()) {
					String fileName = folder.getName();
					fileName = fileName.substring(0, fileName.length() - 4);
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

	private String readContent(HttpURLConnection connection) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}

	private void checkLink(MarkupElement element) throws IOException {
		String url = element.getURL();
		if (url.contains("://")) {
			URL urlObject;
			try {
				urlObject = new URL(url);
			} catch (MalformedURLException ex) {
				throw new IOException("Malformed URL", ex);
			}

			URLConnection connection;
			try {

				connection = urlObject.openConnection();

			} catch (IOException ex) {
				throw new IOException("Cannot read content from URL", ex);
			}
			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
				httpURLConnection.setRequestMethod("GET");
				httpURLConnection.setRequestProperty("Accept-Language" , "en");
				httpURLConnection.connect();
				int code = httpURLConnection.getResponseCode();
				if (code != HttpURLConnection.HTTP_OK) {
					switch (code) {
					case HttpURLConnection.HTTP_MOVED_PERM:
					case HttpURLConnection.HTTP_MOVED_TEMP: {
						String content = httpURLConnection.getHeaderField("Location");

						throw new IOException("Server URL has changed to '" + content + "'.");
					}
					default:
						throw new IOException("Server returned response code " + code + " - "
								+ httpURLConnection.getResponseMessage() + ".");
					}
				}
				httpURLConnection.disconnect();
			}
		}
	}

	private void checkConsistency(List<MarkupElement> elements) {
		for (MarkupElement rootElement : elements) {
			rootElement.visit(new MarkupElementVisitor() {

				@Override
				public void visit(MarkupElement element, int level) {
					if (element.getType() == Type.ROOT) {

					}

					if (element.getType() == Type.LINK) {
						StringBuilder builder = new StringBuilder();
						for (MarkupElement childElement : element.getChildren()) {
							builder.append(childElement.getContent());
						}
						try {
							checkLink(element);
						} catch (IOException ex) {
							log("ROOT: " + element.getRoot().getURL());
							log("LINK: " + element.getURL() + " with description '" + element.getContent() + "'");
							logException(ex);
						}
					}

				}

			});
		}
	};

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

		log("Checking consistency of " + rootElements.size() + " root elements.");
		checkConsistency(rootElements);
	}

	public static void main(String[] args) {
		new AtariWikiChecker().run(args);
		log("Done.");

	}

}
