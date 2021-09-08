package it.cnr.speech.osspeech2text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class DataMinerASR {

	public static String dataMinerUrl = "http://dataminer-prototypes.d4science.org/wps/WebProcessingService?request=Execute&service=WPS&Version=1.0.0";
	//public static String dataMinerUrl = "http://dataminer1-proto.d4science.org/wps/WebProcessingService?request=Execute&service=WPS&Version=1.0.0";
	
	public static String token = "d35c72d3-f6b5-4363-afbe-8e330ef9a913-843339462"; // nlphubber
	
	public static boolean debug = false;
	public static int numberOfThreadsToUse = 1;
	public static int refreshStatusTime = 2000;
	public String language = "en-US";
	
	public DataMinerASR(String language) {
		this.language = language;
	}
	
	public DataMinerASR() {
		
	}

	public static String templateENG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<Execute xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:ns=\"http://www.opengis.net/ows/1.1\" xmlns:xlin=\"http://www.w3.org/1999/xlink\" service=\"WPS\" version=\"1.0.0\">\r\n"
			+ "   <ns:Identifier>org.gcube.dataanalysis.wps.statisticalmanager.synchserver.mappedclasses.transducerers.AUTOMATIC_SPEECH_RECOGNIZER</ns:Identifier>\r\n" + "   <DataInputs>\r\n" + "      <Input>\r\n" + "         <ns:Identifier>inputAudioFile</ns:Identifier>\r\n" + "         <Data>\r\n" + "            <ComplexData mimeType=\"application/d4science\">#INPUT#</ComplexData>\r\n"
			+ "         </Data>\r\n" + "      </Input>\r\n" + "      <Input>\r\n" + "         <ns:Identifier>language</ns:Identifier>\r\n" + "         <Data>\r\n" + "            <LiteralData dataType=\"xs:string\">en-US</LiteralData>\r\n" + "         </Data>\r\n" + "      </Input>\r\n" + "   </DataInputs>\r\n" + "   <ResponseForm>\r\n"
			+ "      <ResponseDocument storeExecuteResponse=\"true\" lineage=\"true\" status=\"true\">\r\n" + "         <Output>\r\n" + "            <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">non_deterministic_output</ows:Identifier>\r\n" + "         </Output>\r\n"
			+ "      </ResponseDocument>\r\n" + "   </ResponseForm>\r\n" + "</Execute>";
	
	public static String templateIT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<Execute xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:ns=\"http://www.opengis.net/ows/1.1\" xmlns:xlin=\"http://www.w3.org/1999/xlink\" service=\"WPS\" version=\"1.0.0\">\r\n"
			+ "   <ns:Identifier>org.gcube.dataanalysis.wps.statisticalmanager.synchserver.mappedclasses.transducerers.AUTOMATIC_SPEECH_RECOGNIZER_SPHINX</ns:Identifier>\r\n" + "   <DataInputs>\r\n" + "      <Input>\r\n" + "         <ns:Identifier>inputAudioFile</ns:Identifier>\r\n" + "         <Data>\r\n" + "            <ComplexData mimeType=\"application/d4science\">#INPUT#</ComplexData>\r\n"
			+ "         </Data>\r\n" + "      </Input>\r\n" + "      <Input>\r\n" + "         <ns:Identifier>language</ns:Identifier>\r\n" + "         <Data>\r\n" + "            <LiteralData dataType=\"xs:string\">it-IT</LiteralData>\r\n" + "         </Data>\r\n" + "      </Input>\r\n" + "   </DataInputs>\r\n" + "   <ResponseForm>\r\n"
			+ "      <ResponseDocument storeExecuteResponse=\"true\" lineage=\"true\" status=\"true\">\r\n" + "         <Output>\r\n" + "            <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">non_deterministic_output</ows:Identifier>\r\n" + "         </Output>\r\n"
			+ "      </ResponseDocument>\r\n" + "   </ResponseForm>\r\n" + "</Execute>";
	
	public static byte[] readWAVAudioFileData(final String filePath) {
		byte[] data = null;
		try {
			final ByteArrayOutputStream baout = new ByteArrayOutputStream();
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));

			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, baout);
			audioInputStream.close();
			baout.close();
			data = baout.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	public File buildTemplateFile(File audioFile) throws Exception {
		File uuidF = new File("audioTemplate" + UUID.randomUUID() + ".xml");
		byte[] audiobytes = readWAVAudioFileData(audioFile.getAbsolutePath());
		String template = "";
		if (language.equals("it-IT"))
			template = templateIT;
		else if(language.equals("it-IT.k")) {
			template = templateENG.replace(">en-US<", ">it-IT<");
		}
		else
			template = templateENG;
		
		String templateString = template.replace("#INPUT#", AudioFromString.bytesToString(audiobytes));
		FileWriter fw = new FileWriter(uuidF);
		fw.write(templateString);
		fw.close();

		return uuidF;
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

	private void pipe(Reader reader, Writer writer) throws IOException {
		char[] buf = new char[1024];
		int read = 0;
		while ((read = reader.read(buf)) >= 0) {
			writer.write(buf, 0, read);
		}
		writer.flush();
	}

	public void postData(Reader data, URL endpoint, Writer output) throws Exception {
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

	public String readStringFromURL(URL url) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine;
		StringBuffer sb = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine);
		}
		in.close();

		return sb.toString();
	}

	public String sendToDataMiner(File templatePrepared) throws Exception {
		FileReader fr = new FileReader(templatePrepared);
		StringWriter sw = new StringWriter();
		if (debug)
			System.out.println("Sending audio");
		postData(fr, new URL(dataMinerUrl + "&gcube-token=" + token), sw);
		fr.close();
		String answer = sw.toString();
		String statusLocation = answer.substring(answer.indexOf("statusLocation=\"") + "statusLocation=\"".length(), answer.indexOf("\">"));
		if (debug)
			System.out.println("Status check:" + statusLocation);
		String status = getStatus(statusLocation + "&gcube-token=" + token);

		while (status ==null || !(status.contains("wps:ProcessSucceeded") || status.contains("wps:ProcessFailed"))) {
			if (debug)
				System.out.println(status);
			status = getStatus(statusLocation + "&gcube-token=" + token);
			Thread.sleep(refreshStatusTime);
		}
		if (debug)
			System.out.println(status);

		if (status.contains("wps:ProcessFailed"))
			System.out.println("Process Failed!");
		else {
			if (debug) {
				String UrlToOutput1 = status.substring(status.indexOf("<d4science:Data>") + "<d4science:Data>".length(), status.indexOf("</d4science:Data>"));
				System.out.println("Url to log:" + UrlToOutput1);
				URL url1 = new URL(UrlToOutput1);
				System.out.println(readStringFromURL(url1));
			}

			String UrlToOutput = status.substring(status.lastIndexOf("<d4science:Data>") + "<d4science:Data>".length(), status.lastIndexOf("</d4science:Data>"));
			if (debug)
				System.out.println("Url to transcription:" + UrlToOutput);
			URL url2 = new URL(UrlToOutput);
			String transcription = readStringFromURL(url2);
			if (transcription.startsWith("R version"))
				transcription=null;
			return transcription;
		}

		return null;

	}

	public String transcribe(File audiofile) throws Exception {
		System.out.println("Transcribing audiofile " + audiofile.getName() + "...");
		String transcription = null;
		long t0 = System.currentTimeMillis();
		File templateFile = buildTemplateFile(audiofile);
		transcription = sendToDataMiner(templateFile);
		templateFile.delete();
		long t1 = System.currentTimeMillis();
		System.out.println("...Finished. Elapsed time: " + (t1 - t0) + " ms for " + audiofile.getName());
		return transcription;
	}

	public void transcribeToFile(File audiofile, File output) {
		String transcription = "";
		try {
			System.out.println("Transcribing audiofile " + audiofile.getName() + "...");

			long t0 = System.currentTimeMillis();
			File templateFile = buildTemplateFile(audiofile);
			transcription = sendToDataMiner(templateFile);
			templateFile.delete();
			long t1 = System.currentTimeMillis();
			System.out.println("...Finished. Elapsed time: " + (t1 - t0) + " ms for " + audiofile.getName());

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (transcription==null)
				transcription="{}";
			FileWriter fw = new FileWriter(output);
			
			transcription = transcription.replace("<oov>", " ");
			transcription = transcription.replaceAll(" +", " ");
			transcription = transcription.trim();
			
			fw.write(transcription);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	int overallTranscriptions = 0;

	public synchronized void updateIdx() {
		overallTranscriptions++;
	}

	public void transcribeAllParallel(File filesDirectory) throws Exception {
		
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadsToUse);

		File[] allfiles = filesDirectory.listFiles();
		int counter = 0;
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav")) {
				File transcription = new File(f.getAbsolutePath().replace(".wav", "_transcription.txt"));

				DThread thread = new DThread(f, transcription);
				executorService.submit(thread);
				counter++;
			}
		}

		long time = 0;
		while (overallTranscriptions < counter) {
			Thread.sleep(1000);
			time += 1000;
			if (time % 10000 == 0) {
				System.out.println("transcribed " + overallTranscriptions + " of " + counter);
			}
		}
		System.out.println("All transcriptions finished");
		executorService.shutdown();
		overallTranscriptions=0;
	}

	//
	private class DThread implements Callable<Integer> {
		File f, transcription;

		public DThread(File f, File transcription) {
			this.f = f;
			this.transcription = transcription;

		}

		@Override
		public Integer call() throws Exception {
			try {
				transcribeToFile(f, transcription);
			} catch (Exception e) {
				System.out.println("Error executing thread " + f.getName());
			}
			updateIdx();
			return null;
		}

	}
	
	

	public static void main(String[] args) throws Exception {

		//File audioFile = new File("hello.wav");
		//File audioFile = new File("sampleUNO16k.wav");
		//File audioFile = new File("sample16k.wav");
		//File audioFile = new File("it_0024.wav");
		//File audioFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy\\cluster0\\audio_segment_126.wav");
		//File audioFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audio_with_open_ASR\\cluster1\\audio_segment_17_ds.wav");
		//File audioFile = new File("sampleLongNews16k.wav");
		File audioFile = new File("discon.wav");
		// File audioFile = new File("helloworld16k.wav");
		//DataMinerASR asr = new DataMinerASR();
		//DataMinerASR asr = new DataMinerASR("it-IT");
		DataMinerASR asr = new DataMinerASR("it-IT.k");
		//DataMinerASR asr = new DataMinerASR("en-US");
		//String transcription = asr.transcribe(audioFile);
		asr.transcribeToFile(audioFile,new File("test.txt"));
		//System.out.println("Transcription:\n" + transcription);
		//File folder = new File("./testaudio");
		//asr.transcribeAllParallel(folder);
		
		
	}



}
