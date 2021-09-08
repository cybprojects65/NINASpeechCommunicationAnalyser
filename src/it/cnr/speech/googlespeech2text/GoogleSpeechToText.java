package it.cnr.speech.googlespeech2text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.cnr.speech.audiofeatures.NoiseReduction;
import it.cnr.speech.speakers.SpeakersExtraction;
import it.cnr.speech.utils.OSCommands;

public class GoogleSpeechToText {

	public String token = "";
	public boolean denoise = false;
	
	public void setDenoise(boolean denoise) {
		this.denoise = denoise;
		
	}
	
	// gcloud auth application-default login
	// gcloud auth application-default print-access-token
	// NOTE: set GOOGLE_APPLICATION_CREDENTIALS as environment variable
	public String produceJSONToSend(File outputFile, String base64Wave) throws Exception {

		if (base64Wave.length() > 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("{" + "\"config\": {" + "\"encoding\":\"LINEAR16\"," + "\"languageCode\": \"it-IT\","
					//+"\"sampleRateHertz\": 44100," //automatically guessed
					
					+ "\"enableWordTimeOffsets\": true" + "}," + "\"audio\": {" + "\"content\":\"");
			sb.append(base64Wave);
			sb.append("\"}}");
			FileWriter fw = new FileWriter(outputFile);
			fw.write(sb.toString());
			fw.close();

			String executeRecognition = "curl -X POST -H \"Authorization: Bearer \"#TOKEN# -H \"Content-Type: application/json; charset=utf-8\" -d @"
					+ outputFile + " https://speech.googleapis.com/v1/speech:recognize";
			return executeRecognition;
		} else
			throw new Exception("No data in the audio file");

	}

	public void recognize(File audioFile, File recognitionOutput) throws Exception {

		System.out.println("Input Audio File " + audioFile);
		if (denoise)
		{
			new NoiseReduction().denoise(audioFile);
		}
		
		File tokenFile = new File("token" + UUID.randomUUID() + ".txt");
		tokenFile.delete();
		String getToken = "gcloud auth application-default print-access-token>" + tokenFile.getName();
		String[] commandsGT = new String[3];
		File f = new File("SpeechRecognitionNINA-b72db26d0dce.json");
		commandsGT[0] = "cmd /c " + getToken; // cmd -c
		// commandsGT[1] = "set
		// GOOGLE_APPLICATION_CREDENTIALS="+f.getAbsolutePath()+" && "+getToken;
		// commandsGT[2] = getToken;
		OSCommands.executeOSCommands(commandsGT);
		String token = Files.readAllLines(tokenFile.toPath()).get(0);
		System.out.println("TOKEN:" + token);

		String base64File = toBase64(audioFile);
		File toSend = new File("rec" + UUID.randomUUID() + ".json");

		String recognitionCommand = produceJSONToSend(toSend, base64File);
		recognitionCommand = recognitionCommand.replace("#TOKEN#", token);

		String[] commands = new String[2];

		commands[0] = "cmd -c " + recognitionCommand;
		// commands[1] = "set
		// GOOGLE_APPLICATION_CREDENTIALS="+f.getAbsolutePath()+" &&
		// "+recognitionCommand;
		commands[1] = recognitionCommand;
		// String recognition ="";
		String recognition = OSCommands.executeOSCommands(commands);
		recognition = recognition.substring(recognition.indexOf("{"), recognition.lastIndexOf("}") + 1);
		if (!recognition.contains("\"code\": 400")) {
			BufferedWriter fw = new BufferedWriter(new FileWriter(recognitionOutput));
			fw.write(recognition);
			fw.close();
		}
		tokenFile.delete();
		toSend.delete();
	}

	public String toBase64(File audioFile) {

		byte[] bytes;
		String encoded = "";
		try {
			bytes = Files.readAllBytes(audioFile.toPath());
			encoded = Base64.getEncoder().encodeToString(bytes);
		} catch (IOException e) {

			e.printStackTrace();
		}

		return encoded;
	}

	//
	private class GThread implements Callable<Integer> {
		File f, transcription;

		public GThread(File f, File transcription) {
			this.f = f;
			this.transcription = transcription;

		}

		@Override
		public Integer call() throws Exception {
			try {
				recognize(f, transcription);
			} catch (Exception e) {
				System.out.println("Error executing thread " + f.getName());
			}
			updateIdx();
			return null;
		}

	}

	int overallTranscriptions = 0;

	public synchronized void updateIdx() {
		overallTranscriptions++;
	}

	public void transcribeAllParallel(File filesDirectory) throws Exception {
		int numberOfThreadsToUse = 8;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadsToUse);

		File[] allfiles = filesDirectory.listFiles();
		int counter = 0;
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav")) {
				File transcription = new File(f.getAbsolutePath().replace(".wav", ".txt"));

				GThread thread = new GThread(f, transcription);
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
	}

	public void transcribeAll(File filesDirectory) throws Exception {
		transcribeAll(filesDirectory, false);
	}
	
	public void transcribeAll(File filesDirectory, boolean splitSpeakers) throws Exception {
		File[] allfiles = filesDirectory.listFiles();
		
		
		if (splitSpeakers) {
			SpeakersExtraction se = new SpeakersExtraction();
			for (File f : allfiles) {
				if (f.getName().endsWith(".wav") && !f.getName().contains("_spk_")) {
					se.extractSpeakersAndDeleteOriginal(f);
				}
			}
			allfiles = filesDirectory.listFiles();
		}
		
		
		int counter = 0;
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav")) {
				
				if (splitSpeakers && !f.getName().contains("_spk_"))
					continue;
				
				File transcription = new File(f.getAbsolutePath().replace(".wav", ".txt"));
				
				if (splitSpeakers)
					transcription = new File(f.getParent(),"t_r_"+counter+".txt");
				
				recognize(f, transcription);
				counter++;
			}
		}
		
	}

	public static void main(String[] args) throws Exception {
		GoogleSpeechToText gsst = new GoogleSpeechToText();
		// gsst.recognize(new File("./\\samples\\audioCut.wav"),new
		// File("./samples/audioCut_full.txt"));
		// File("transcription.txt"));
		// gsst.transcribeAllParallel(new File("./segments_T"));
		// gsst.transcribeAll(new File("./segments_T"));
		//gsst.transcribeAllParallel(new File("./segments_T"));
		//gsst.transcribeAllParallel(new File("D:\\WorkFolder\\Experiments\\Sphinx\\sphinx4\\testset_4_google\\"));
		//gsst.transcribeAllParallel(new File("D:\\WorkFolder\\Experiments\\Sphinx\\sphinx4\\gpcorpus_4_google\\"));
		//gsst.recognize(new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds_denoised.wav"), new File("testGoogleTranscr.txt"));
		//gsst.recognize(new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds.wav"), new File("testGoogleTranscrNoise.txt"));
		//gsst.transcribeAllParallel(new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon digits\\Wave_Cifre_Isolate_4_google\\"));
		gsst.transcribeAllParallel(new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test_4_google\\"));
		
	}
}
