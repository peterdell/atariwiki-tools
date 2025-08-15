package com.wudsn.productions.www.atariwiki;

import static com.wudsn.productions.www.atariwiki.Utilities.log;
import static com.wudsn.productions.www.atariwiki.Utilities.logException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.wudsn.productions.www.atariwiki.MarkupElement.Attachment;
import com.wudsn.productions.www.atariwiki.MarkupElement.Type;

public class MarkupChecker {

	private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Mobile Safari/537.36";

	public boolean validateLinks;
	public boolean validateHttpLinks;

	private File rootFolder;
	private Map<String, MarkupElement> rootElementsMap;
	private long issueCount;

	public MarkupChecker() {

	}

	private void logError(MarkupElement element, IOException ex) {
		log("ISSUE: " + Long.toString(issueCount));
		log("ROOT : " + element.getRoot().getURL());
		log("LINK : " + element.getURL() + " with description '" + element.getContent() + "'");
		logException(ex);
		issueCount = issueCount + 1;
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

	private MarkupElement getElementForRelativePath(File rootElementFile, String url) {
		File file = new File(rootElementFile.getParentFile(), url);
		try {
			String filePath = file.getCanonicalPath();
			return rootElementsMap.get(filePath);

		} catch (IOException ex) {
			logException(ex);
			return null;
		}
	}

	public void checkConsistency(File rootFolder, List<MarkupElement> elements) {
		this.rootFolder = rootFolder;
		this.rootElementsMap = new TreeMap<String, MarkupElement>();
		for (MarkupElement rootElement : elements) {
			rootElementsMap.put(rootElement.getURL(), rootElement);
		}
		issueCount = 0;
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
							logError(element, ex);
						}
					}

				}

			});
		}
	};
}
