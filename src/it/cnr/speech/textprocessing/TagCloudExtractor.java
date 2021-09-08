package it.cnr.speech.textprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;

public class TagCloudExtractor {
	private String token = "3a8e6a79-1ae0-413f-9121-0d59e5f2cea2-843339462"; // gianpaolo.coro
																				// in
																				// Rproto
	static String dataMinerProcessUrl = "http://dataminer-prototypes.d4science.org/wps/WebProcessingService?request=Execute&service=WPS&Version=1.0.0";

	int refreshTime = 1000;
	File outputJsonFile;
	File outputAnnotationFile;

	public static void main(String[] args) throws Exception {

		// File textFile = new File("./segments_v2/dialog.txt");
		File textFile = new File("./segments_v2/dialog_cluster_0.txt");
		File textFolder = new File("./segments_v2/");

		/*
		 * String language = "it"; TagCloudExtractor caller = new
		 * TagCloudExtractor(); caller.run(language, textFile, true, new
		 * File("./segments_v2/cloud.html"));
		 * System.out.println("JSON output is: "+caller.getOutputJsonFile());
		 * System.out.println("Annotated text is: "+caller.
		 * getOutputAnnotationFile());
		 * 
		 */
		
		String t = "asf..args def..args ser";
		t = t.replaceAll("\\.+", ".");
		System.out.println("t:"+t);
		
		//TagCloudExtractor tagger = new TagCloudExtractor();
		 //tagger.annotateAllDialogues(textFolder);
		
	}

	public void annotateAllDialogues(File folder) throws Exception {
		File allFiles[] = folder.listFiles();
		for (File f : allFiles) {
			if (f.getName().startsWith("dialog_cluster_")) {
				System.out.println("Annotating " + f.getName());
				File outputFile = new File(f.getParentFile(), f.getName().replace("dialog_cluster_", "cloudtag").replace(".txt", ".html"));
				run("it", f, true, outputFile);
			}
		}
	}

	

	public File getOutputJsonFile() {
		return outputJsonFile;
	}

	public File getOutputAnnotationFile() {
		return outputAnnotationFile;
	}

	public TagCloudExtractor() {
	}

	private static void pipe(Reader reader, Writer writer) throws IOException {
		char[] buf = new char[1024];
		int read = 0;
		while ((read = reader.read(buf)) >= 0) {
			writer.write(buf, 0, read);
		}
		writer.flush();
	}

	public static void postData(Reader data, URL endpoint, Writer output) throws Exception {
		HttpURLConnection urlc = null;
		try {
			urlc = (HttpURLConnection) endpoint.openConnection();
			try {
				urlc.setRequestMethod("POST");
			} catch (ProtocolException e) {
				throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
			}
			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setUseCaches(false);
			urlc.setAllowUserInteraction(false);
			urlc.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");

			OutputStream out = urlc.getOutputStream();

			try {
				Writer writer = new OutputStreamWriter(out, "UTF-8");
				pipe(data, writer);
				writer.close();
			} catch (IOException e) {
				throw new Exception("IOException while posting data", e);
			} finally {
				if (out != null)
					out.close();
			}

			InputStream in = urlc.getInputStream();
			try {
				Reader reader = new InputStreamReader(in);
				pipe(reader, output);
				reader.close();
			} catch (IOException e) {
				throw new Exception("IOException while reading response", e);
			} finally {
				if (in != null)
					in.close();
			}

		} catch (IOException e) {
			throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
		} finally {
			if (urlc != null)
				urlc.disconnect();
		}
	}

	public static String getStatus(String endpoint) {
		String result = null;

		// Send a GET request to the servlet
		try {
			// Send data
			String urlStr = endpoint;

			URL url = new URL(urlStr);
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(120000);
			conn.setReadTimeout(120000);

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			result = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public void run(String language, File textFile, boolean useverbs, File outputCloudFile) throws Exception {
		System.out.println("NLPHUB client has started");
		File template = null;
		template = new File("./config/CloudTagger.txt");
		byte[] encoded = Files.readAllBytes(Paths.get(template.getPath()));
		String content = new String(encoded, "UTF-8");
		System.out.println("Preprocessing text");
		String text = preprocess(textFile);
		text += ". " + text;
		text = text.replaceAll("\\.+", ".");
		System.out.println("NLP Cloud Tag - sending the following text: \n"+text);
		content = content.replace("#LANGUAGE#", language);
		content = content.replace("#CONTENT#", text);
		content = content.replace("#VERBS#", "" + useverbs);

		File tempFile = new File("NLPHUB_" + UUID.randomUUID() + ".txt");
		FileWriter fw = new FileWriter(tempFile);
		fw.write(content);
		fw.close();

		StringWriter sw = new StringWriter();
		FileReader fr = new FileReader(tempFile);

		System.out.println("Running...");
		long t0 = System.currentTimeMillis();
		postData(fr, new URL(dataMinerProcessUrl + "&gcube-token=" + token), sw);

		fr.close();

		String answer = sw.toString();

		String statusLocation = answer.substring(answer.indexOf("statusLocation=\"") + "statusLocation=\"".length(), answer.indexOf("\">"));

		// System.out.println(sw.toString());
		// System.out.println(statusLocation);

		String status = getStatus(statusLocation + "&gcube-token=" + token);

		while (!(status.contains("wps:ProcessSucceeded") || status.contains("wps:ProcessFailed"))) {
			// System.out.println(status);
			status = getStatus(statusLocation + "&gcube-token=" + token);
			Thread.sleep(refreshTime);
		}
		long t1 = System.currentTimeMillis();

		System.out.println("Finished in " + (t1 - t0) + " ms");

		// System.out.println(status);
		tempFile.delete();

		if (status.contains("wps:ProcessFailed")) {
			System.out.println("Process Failed!");
			System.out.println(status);
			throw new Exception("Process failed");
		} else {
			status = status.substring(status.indexOf("</d4science:Data>") + "</d4science:Data>".length() + 1);
			String UrlToJSON = status.substring(status.indexOf("<d4science:Data>") + "<d4science:Data>".length(), status.indexOf("</d4science:Data>"));
			System.out.println("NLPHub - stems:" + UrlToJSON);
			outputJsonFile = new File(outputCloudFile.getParentFile(), outputCloudFile.getName().replace(".html", "_stems.txt"));
			outputJsonFile.createNewFile();
			downloadFile(UrlToJSON, outputJsonFile.getAbsolutePath());
			status = status.substring(status.indexOf("</d4science:Data>") + "</d4science:Data>".length() + 1);
			status = status.substring(status.indexOf("</d4science:Data>") + "</d4science:Data>".length() + 1);
			String UrlToAnnotation = status.substring(status.indexOf("<d4science:Data>") + "<d4science:Data>".length(), status.indexOf("</d4science:Data>"));
			System.out.println("NLPHub - Cloud:" + UrlToAnnotation);
			outputAnnotationFile = outputCloudFile;
			downloadFile(UrlToAnnotation, outputAnnotationFile.getAbsolutePath());

		}
	}

	public static void downloadFile(String fileurl, String localFile) throws Exception {
		URL smpFile = new URL(fileurl);
		URLConnection uc = (URLConnection) smpFile.openConnection();
		InputStream is = uc.getInputStream();
		// System.out.println("Retrieving from " + fileurl + " to :" +
		// localFile);
		inputStreamToFile(is, localFile);
		is.close();
		is = null;
		System.gc();
	}

	public static void inputStreamToFile(InputStream is, String path) throws FileNotFoundException, IOException {
		FileOutputStream out = new FileOutputStream(new File(path));
		byte buf[] = new byte[1024];
		int len = 0;
		while ((len = is.read(buf)) > 0)
			out.write(buf, 0, len);
		out.close();
	}

	public static String preprocess(File textFile) throws Exception {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(textFile.getPath()));
			String content = new String(encoded, "UTF-8");
			content = cleanCharacters(content);
			return content;
		} catch (Exception e) {
			System.out.println("Eror while reading file " + e.getMessage());
			throw e;
		}
	}

	public static String cleanCharacters(String source) {

		char c = 0;
		for (int i = 0; i < source.length(); i++) {
			c = source.charAt(i);
			if (!((c >= 33 && c <= 90) || (c >= 97 && c <= 122) || (c >= 128 && c <= 167) || (c >= 180 && c <= 183) || (c >= 210 && c <= 212) || (c >= 214 && c <= 216) || (c >= 224 && c <= 255))) {
				source = source.replace(source.substring(i, i + 1), " ");
			}
		}

		source = source.replaceAll("[\\s]+", " ").trim();
		source = source.replaceAll("<", " ").trim();
		source = source.replaceAll(">", " ").trim();
		return source;
	}
}
