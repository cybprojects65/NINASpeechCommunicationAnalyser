package it.cnr.speech.orchestration;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import it.cnr.speech.annotation.AudioAnnotator;
import it.cnr.speech.audiofeatures.EnergyPitchExtractor;
import it.cnr.speech.clustering.ClustersAnalyser;
import it.cnr.speech.clustering.EnergyPitchClusterer;
import it.cnr.speech.dialogues.TextDialogueManipulator;
import it.cnr.speech.googlespeech2text.GoogleSpeechToText;
import it.cnr.speech.osspeech2text.DataMinerASR;
import it.cnr.speech.osspeech2text.SphinxASRNINA;
import it.cnr.speech.segmentation.Energy;
import it.cnr.speech.speakers.SpeakersExtraction;

import it.cnr.speech.textprocessing.TagCloudExtractor;

public class DialogueAnalyserEnergyPitch {

	public static void main(String[] args) throws Exception {
		Logger.getRootLogger().setLevel(Level.OFF);
		File audio = new File("./samples/audioCut.wav");
		//File audio = new File("./samples/audio.wav");
		// File audio = new File("./samples/audioCut_copy2.wav");
		// File audio = new File("./samples/audio_copy.wav");

		DialogueAnalyserEnergyPitch dt = new DialogueAnalyserEnergyPitch();
		dt.executeTaggingWorkflowEnergyPitch(audio);
	}

	public void cleanFolder(File folder) throws Exception {
		if (folder.exists() && folder.listFiles().length > 0)
			FileUtils.deleteDirectory(folder);
	}

	int nClusters;
	float maxSilence;
	float pitchWindowSec;
	float energyWindowSec;
	int windowSecToTakeForClustering;
	boolean useQuestionsForStatisticalAnalysis;
	boolean produceSpeakerSeparation;
	boolean useGoogleASR;
	boolean useSphinx;
	boolean doTextAnalysis;

	public DialogueAnalyserEnergyPitch() {
		defaultConfig();
	}

	public void defaultConfig() {
		nClusters = 2; // best is 2
		maxSilence = 0.1f; // best is 0.1
		pitchWindowSec = 0.2f; // default 0.2
		energyWindowSec = 0.1f; // default 0.1
		windowSecToTakeForClustering = 3; // default 3
		useQuestionsForStatisticalAnalysis = false;
		produceSpeakerSeparation = false;
		useGoogleASR = true; // false;//true;
		useSphinx = false;
		doTextAnalysis = true;
	}

	public DialogueAnalyserEnergyPitch(int nClusters, float maxSilence, float pitchWindowSec, float energyWindowSec,
			int windowSecToTakeForClustering, boolean useQuestionsForStatisticalAnalysis) {
		this.nClusters = nClusters;
		this.maxSilence = maxSilence;
		this.pitchWindowSec = pitchWindowSec;
		this.energyWindowSec = energyWindowSec;
		this.windowSecToTakeForClustering = windowSecToTakeForClustering;
		this.useQuestionsForStatisticalAnalysis = useQuestionsForStatisticalAnalysis;
	}

	public void executeTaggingWorkflowEnergyPitch(File audioFile) throws Exception {

		long t00 = System.currentTimeMillis();
		File tempFolder = new File(audioFile.getName().replace(".wav", ""));
		long t0 = 0;
		long t1 = 0;

		cleanFolder(tempFolder);
		System.out.println("Temporary folder " + tempFolder);
		System.out.println("1- Segment based on energy");
		t0 = System.currentTimeMillis();
		Energy energy = new Energy();
		energy.setWindowIns(maxSilence);
		energy.segmentSignal(audioFile, tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("2- Extract Pitch and Energy");
		t0 = System.currentTimeMillis();
		EnergyPitchExtractor epe = new EnergyPitchExtractor(energyWindowSec, pitchWindowSec);
		epe.extractEnergyPitchOfWaveFiles(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("3- Cluster the segments");
		t0 = System.currentTimeMillis();
		EnergyPitchClusterer epc = new EnergyPitchClusterer(windowSecToTakeForClustering, nClusters);
		epc.clusterEnergyPitch(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("4- Detecting the most alarming cluster and annotate the original signal");
		t0 = System.currentTimeMillis();
		ClustersAnalyser ca = new ClustersAnalyser();
		int warnCluster = ca.detectWarningCluster(tempFolder, useQuestionsForStatisticalAnalysis, energyWindowSec,
				pitchWindowSec);

		String[] labels = new String[nClusters];
		labels[warnCluster] = "Dialogue alteration";

		AudioAnnotator aa = new AudioAnnotator();
		aa.tagAudioWithClusters(tempFolder, audioFile, labels);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0)) + "ms");

		if (doTextAnalysis) {
			System.out.println("5- Transcribing");
			t0 = System.currentTimeMillis();
			File clusterFolder = new File(tempFolder, ClustersAnalyser.CLUSTERPREFIX + warnCluster);

			if (useGoogleASR) {
				GoogleSpeechToText asr = new GoogleSpeechToText();
				boolean speakerSeparation = false;
				asr.transcribeAll(clusterFolder, speakerSeparation);
			} else {
				// DataMinerASR asr = new DataMinerASR("it-IT");
				// asr.transcribeAllParallel(clusterFolder);
				if (useSphinx) {
					boolean speakerSeparation = true;
					SphinxASRNINA asr = new SphinxASRNINA("it-IT", speakerSeparation);
					// asr.setNumberOfThreadsToUse(10);
					asr.setNumberOfThreadsToUse(1);
					asr.transcribeAll(clusterFolder);
					// asr.transcribeAllSequential(clusterFolder);
				} else {
					System.out.println("Calling Dataminer");
					DataMinerASR asr = new DataMinerASR("it-IT.k");
					asr.transcribeAllParallel(clusterFolder);
					System.out.println("Calling Dataminer DONE!");
				}
			}
			t1 = System.currentTimeMillis();
			System.out.println("Finished in " + ((float) (t1 - t0)) + "ms");

			System.out.println("6- Estimating the most important words");
			t0 = System.currentTimeMillis();
			System.out.println(" 6.1- Preparing dialogue text for alarming cluster");
			TextDialogueManipulator dialogueAnalyser = new TextDialogueManipulator();
			dialogueAnalyser.removeEmptyFiles(clusterFolder, false);
			dialogueAnalyser.reportTranscriptions(clusterFolder);
			// also adds question marks in the transcription
			dialogueAnalyser.createOneTranscription(clusterFolder, true);

			File dialog = new File(clusterFolder, TextDialogueManipulator.DIALOGFILE);
			File cloud = new File(audioFile.getAbsolutePath().replace(".wav", ".html"));
			System.out.println(" 6.2- Extracting tag cloud");
			TagCloudExtractor tagger = new TagCloudExtractor();
			tagger.run("it", dialog, true, cloud);
			t1 = System.currentTimeMillis();
			System.out.println("Finished in " + ((float) (t1 - t0)) + "ms");
		}

		if (produceSpeakerSeparation) {
			System.out.println("7- Separating speakers dialogues");
			SpeakersExtraction sps = new SpeakersExtraction();
			sps.extractSpeakersBatch(tempFolder);
		}

		long t11 = System.currentTimeMillis();
		System.out.println("All done. Elapsed:" + ((float) (t11 - t00) / 1000f) + "s");

	}

	public void segmentAudioForManualTagging(File audioFile) throws Exception {

		long t00 = System.currentTimeMillis();
		File tempFolder = new File(audioFile.getName().replace(".wav", ""));
		long t0 = 0;
		long t1 = 0;

		cleanFolder(tempFolder);
		System.out.println("Temporary folder " + tempFolder);
		System.out.println("1- Segment based on energy");
		t0 = System.currentTimeMillis();
		Energy energy = new Energy();
		energy.setWindowIns(maxSilence);
		energy.segmentSignal(audioFile, tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("2- Extract Pitch and Energy");
		t0 = System.currentTimeMillis();
		EnergyPitchExtractor epe = new EnergyPitchExtractor(energyWindowSec, pitchWindowSec);
		epe.extractEnergyPitchOfWaveFiles(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("3- Cluster the segments");
		t0 = System.currentTimeMillis();
		EnergyPitchClusterer epc = new EnergyPitchClusterer(windowSecToTakeForClustering, nClusters);
		epc.clusterEnergyPitch(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0) / 1000f) + "s");

		System.out.println("4- Detecting the most alarming cluster and annotate the original signal");
		t0 = System.currentTimeMillis();
		ClustersAnalyser ca = new ClustersAnalyser();
		int warnCluster = ca.detectWarningCluster(tempFolder, useQuestionsForStatisticalAnalysis, energyWindowSec,
				pitchWindowSec);

		String[] labels = new String[nClusters];
		labels[warnCluster] = "Dialogue alteration";

		AudioAnnotator aa = new AudioAnnotator();
		aa.tagAudioWithClusters(tempFolder, audioFile, labels);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in " + ((float) (t1 - t0)) + "ms");

		long t11 = System.currentTimeMillis();
		System.out.println("All done. Elapsed:" + ((float) (t11 - t00) / 1000f) + "s");
	}

}
