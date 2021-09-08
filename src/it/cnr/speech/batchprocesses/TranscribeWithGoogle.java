package it.cnr.speech.batchprocesses;

import java.io.File;

import it.cnr.speech.googlespeech2text.GoogleSpeechToText;

public class TranscribeWithGoogle {
	
	
	public static void main(String[] args) throws Exception{
		
		
		File folder = new File("./PS12Audio/");
		File allFiles [] = folder.listFiles(); 
		for (File f:allFiles) {
			if (f.getName().endsWith(".wav")) {
				GoogleSpeechToText gs2t = new GoogleSpeechToText();
				gs2t.recognize(f, new File(f.getAbsolutePath()+"_fulltranscription.txt"));
			}
		}
	}
}
