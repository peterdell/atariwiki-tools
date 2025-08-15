package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.*;
import static com.wudsn.productions.www.atariwiki.Utilities.logException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.wudsn.productions.www.atariwiki.MarkupElement.Attachment;
import com.wudsn.productions.www.atariwiki.MarkupElement.Type;

public class MarkupChecker {

	private static class Issue {

		private MarkupElement element;
		private IOException exception;

		public Issue(MarkupElement element, IOException exception) {
			this.element = element;
			this.exception = exception;
		}

	}

	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Mobile Safari/537.36";

	public boolean validateLinks;
	public boolean validateHttpLinks;

	private File rootFolder;
	private Map<String, MarkupElement> rootElementsMap;
	private long issueCount;
	private Map<String, List<Issue>> issuesMap;

	public MarkupChecker() {

	}

	private void addIssue(MarkupElement element, IOException ex) {
		Issue issue = new Issue(element, ex);
		String key = element.getRoot().getURL();
		List<Issue> issuesList = issuesMap.get(key);
		if (issuesList == null) {
			issuesList = new ArrayList<Issue>();
			issuesMap.put(key, issuesList);
		}
		issuesList.add(issue);
		issueCount++;
	}

//	private String readContent(HttpURLConnection connection) throws IOException {
//		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//		String inputLine;
//		StringBuilder response = new StringBuilder();
//
//		while ((inputLine = in.readLine()) != null) {
//			response.append(inputLine);
//		}
//		in.close();
//		return response.toString();
//	}

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
			if (connection instanceof HttpURLConnection && validateHttpLinks) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
				httpURLConnection.setRequestMethod("GET");
				httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
				httpURLConnection.setRequestProperty("Accept-Language", "en");
				httpURLConnection.connect();
				int code = httpURLConnection.getResponseCode();
				if (code != HttpURLConnection.HTTP_OK) {
					String responseCode = "Server returned response code " + code + " - "
							+ httpURLConnection.getResponseMessage() + ".";
					switch (code) {
					case HttpURLConnection.HTTP_MOVED_PERM:
					case HttpURLConnection.HTTP_MOVED_TEMP: {
						String location = httpURLConnection.getHeaderField("Location");
						if (!url.equals(location)) {
							throw new IOException("The server URL has changed to '" + location + "'.");
						}
					}
					default:
						throw new IOException(responseCode);
					}
				}
				httpURLConnection.disconnect();
			}
		} else {
			if (url.startsWith("..") && url.endsWith("/" + Markup.INDEX_MD)) {
				MarkupElement rootElement = element.getRoot();
				File rootElementFile = new File(rootElement.getURL());
				MarkupElement targetElement = getElementForRelativePath(rootElementFile, url);
				if (targetElement == null) {
					throw new IOException("Linked page with URL '" + url + "' not found.");
				}
			}
			if (url.startsWith(Markup.ATTACHMENTS_PREFIX)) {
				String fileName = url.substring(Markup.ATTACHMENTS_PREFIX_LENGTH);
				MarkupElement rootElement = element.getRoot();
				Attachment targetAttachement = rootElement.getAttachment(fileName);
				if (targetAttachement == null) {
					throw new IOException("Linked attachment '" + fileName + "' not found.");
				}
			}
		}
	}

	private MarkupElement getElementForRelativePath(File rootElementFile, String url) throws IOException {
		File file = new File(rootElementFile.getParentFile(), url);
		try {
			String filePath = file.getCanonicalPath();
			return rootElementsMap.get(filePath);

		} catch (IOException ex) {
			throw new IOException(String.format("Cannot get canonical path for absolute path '%s' of URL '%s'.",
					file.getAbsolutePath(), url), ex);
		}
	}

	public void checkConsistency(File rootFolder, List<MarkupElement> elements) {
		this.rootFolder = rootFolder;
		this.rootElementsMap = new TreeMap<String, MarkupElement>();
		for (MarkupElement rootElement : elements) {
			rootElementsMap.put(rootElement.getURL(), rootElement);
		}
		issueCount = 0;
		issuesMap = new TreeMap<String, List<Issue>>();
		for (MarkupElement rootElement : elements) {
			rootElement.visit(new MarkupElementVisitor() {

				@Override
				public void visit(MarkupElement element, int level) {
					if (element.getType() == Type.ROOT) {

					}

					if (element.getType() == Type.LINK && validateLinks) {
						StringBuilder builder = new StringBuilder();
						for (MarkupElement childElement : element.getChildren()) {
							builder.append(childElement.getContent());
						}
						try {
							checkLink(element);
						} catch (IOException ex) {
							addIssue(element, ex);
						}
					}

				}

			});
		}

		logInfo("Found %s issues in %s pages.", Long.toString(issueCount), Long.toString(issuesMap.size()));
		for (String key : issuesMap.keySet()) {
			List<Issue> issuesList = issuesMap.get(key);
			String from = key.substring(rootFolder.getAbsolutePath().length() + "\\content\\".length());
			from = from.replace("\\index.md", ".txt");
			from = "C:\\jac\\system\\WWW\\Programming\\Repositories\\atariwiki.jsp\\p\\web\\www-data\\jspwiki\\" + from;
			log("ROOT : " + issuesList.size() + " issues in " + key);
			log("FROM : " + from);

			for (Issue issue : issuesList) {
				log("ELEMENT: " + issue.element.getURL() + " with description '" + issue.element.getContent()
						+ "' in line " + issue.element.getLineNumber());
				logException(issue.exception);
			}
			log("");

		}
	};
}
