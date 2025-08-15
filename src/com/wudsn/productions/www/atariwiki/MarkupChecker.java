package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.*;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.wudsn.productions.www.atariwiki.MarkupElement.Attachment;
import com.wudsn.productions.www.atariwiki.MarkupElement.Type;
import com.wudsn.productions.www.atariwiki.jsp.AtariWikiConverter;

public class MarkupChecker {

	private static class RootElementIssues {
		MarkupElement rootElement;
		List<Issue> issuesList;

		public RootElementIssues(MarkupElement rootElement) {
			this.rootElement = rootElement;
			issuesList = new ArrayList<Issue>();
		}
	}

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
	private Map<String, RootElementIssues> issuesMap;

	public MarkupChecker() {

	}

	private void addIssue(MarkupElement element, IOException ex) {
		Issue issue = new Issue(element, ex);
		MarkupElement rootElement = element.getRoot();
		String key = element.getRoot().getURL();
		RootElementIssues rootElementIssues = issuesMap.get(key);
		if (rootElementIssues == null) {
			rootElementIssues = new RootElementIssues(rootElement);
			issuesMap.put(key, rootElementIssues);
		}
		rootElementIssues.issuesList.add(issue);
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

	public void checkConsistency(File rootFolder, List<MarkupElement> elements, Logger logger) {
		logger.logInfo("Checking consistency of %s root elements in folder '%s'.", Integer.toString(elements.size()),
				rootFolder.getAbsolutePath());

		this.rootFolder = rootFolder;
		this.rootElementsMap = new TreeMap<String, MarkupElement>();
		for (MarkupElement rootElement : elements) {
			rootElementsMap.put(rootElement.getURL(), rootElement);
		}
		issueCount = 0;
		issuesMap = new TreeMap<String, RootElementIssues>();
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

		List<RootElementIssues> rootElementIssuesList = new ArrayList<RootElementIssues>();
		rootElementIssuesList.addAll(issuesMap.values());
		Collections.sort(rootElementIssuesList, new Comparator<RootElementIssues>() {

			@Override
			public int compare(RootElementIssues o1, RootElementIssues o2) {
				return Integer.compare(o1.issuesList.size(), o2.issuesList.size());
			}
		});

		File jspFolder = new File("C:\\\\jac\\\\system\\\\WWW\\\\Programming\\\\Repositories\\\\atariwiki.jsp");
		Properties cleanNamesMap = null;

		File fromFolder = new File(
				"C:\\jac\\system\\WWW\\Programming\\Repositories\\atariwiki.jsp\\p\\web\\www-data\\jspwiki\\");
		try {
			cleanNamesMap = AtariWikiConverter.loadCleanNamesMap(jspFolder);
		} catch (IOException ex) {
			logger.logException(ex);
		}
		logger.logInfo("Found %s issues in %s pages at %s.", Long.toString(issueCount), Long.toString(issuesMap.size()),
				new Date().toString());
		for (RootElementIssues rootElementIssues : rootElementIssuesList) {
			String key = rootElementIssues.rootElement.getURL();
			File file = new File(key);
			String fileName = file.getParentFile().getName();

			logger.logInfo("ROOT : %d issues in %s", rootElementIssues.issuesList.size(), key);
			if (cleanNamesMap != null) {
				for (Map.Entry<Object, Object> entry : cleanNamesMap.entrySet()) {
					if (entry.getValue().equals(fileName)) {
						File fromFile = new File(fromFolder, ((String) entry.getKey()) + ".txt");
						if (file.exists()) {
							logger.logInfo("FROM : %s", fromFile.getAbsolutePath());
						}
					}
				}
			}

			for (Issue issue : rootElementIssues.issuesList) {
				logger.logInfo("ELEMENT: %s with description '%s' in line %d.", issue.element.getURL(),
						issue.element.getContent(), issue.element.getLineNumber());
				logger.logException(issue.exception);
			}
			logger.newLine();

		}
	};
}
