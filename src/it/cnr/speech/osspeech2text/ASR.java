package it.cnr.speech.osspeech2text;

import java.io.File;

public interface ASR {
	
	public void transcribeAll(File filesDirectory);
	public void transcribe(File audiofile, File output);

}
