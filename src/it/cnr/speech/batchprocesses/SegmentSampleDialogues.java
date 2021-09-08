package it.cnr.speech.batchprocesses;

import java.io.File;

import it.cnr.speech.orchestration.DialogueAnalyserEnergyPitch;

public class SegmentSampleDialogues {

	public static void main(String[] args) throws Exception{
		
		File rootFolder = new File("D:\\WorkFolder\\Experiments\\NINA\\AUDIO_SIMULAZIONI_WAV\\");
		File allWaveFiles []= rootFolder.listFiles();
		
		for (File audio:allWaveFiles) {
			if (audio.getName().endsWith(".wav")) {
				{
					File lab = new File(audio.getAbsolutePath().replace(".wav", ".lab"));
					if (!lab.exists()) {
						System.out.println("Processing "+audio.getName());
						DialogueAnalyserEnergyPitch dt = new DialogueAnalyserEnergyPitch();
						dt.segmentAudioForManualTagging(audio);
						lab.delete();
						//break;
					}
					
				}
				
			}
			
		}
	}
	
}
