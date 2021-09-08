package it.cnr.speech.batchprocesses;

import java.io.File;

import it.cnr.speech.orchestration.DialogueAnalyserEnergyPitch;

public class EvaluteSinglePerformance {

	public static void main(String[] args) throws Exception{
		
		File rootFolder = new File("D:\\WorkFolder\\Experiments\\NINA\\AUDIO_SIMULAZIONI_WAV\\");
		File allWaveFiles []= rootFolder.listFiles();
		int maxFiles = 0;
		int fcounter = 0;
		for (File audio:allWaveFiles) {
			if (audio.getName().endsWith(".wav")) {
				{
					File lab = new File(audio.getAbsolutePath().replace(".wav", ".lab"));
					if (!lab.exists()) {
						System.out.println("Processing "+audio.getName());
						DialogueAnalyserEnergyPitch dt = new DialogueAnalyserEnergyPitch();
						//dt.segmentAudioForManualTagging(audio);
						dt.executeTaggingWorkflowEnergyPitch(audio);
						lab.delete();
						if (fcounter>=maxFiles)
							break;
					}
				}
				fcounter++;
			}
		}
		
		String arg []= {""+maxFiles};
		PerformanceEvaluation.main(arg);
		
	}
	
}
