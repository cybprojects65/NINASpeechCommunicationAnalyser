package it.cnr.speech.orchestration;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import it.cnr.speech.annotation.AudioAnnotator;
import it.cnr.speech.audiofeatures.EnergyPitchExtractor;
import it.cnr.speech.clustering.ClustersAnalyser;
import it.cnr.speech.clustering.EnergyPitchClusterer;
import it.cnr.speech.dialogues.TextDialogueManipulator;
import it.cnr.speech.googlespeech2text.GoogleSpeechToText;
import it.cnr.speech.segmentation.AudioAnnotationSegmenter;
import it.cnr.speech.segmentation.AudioSegmentsAnalyser;
import it.cnr.speech.speakers.SpeakersTagging;
import it.cnr.speech.sphinx.segmentation.EnergySegmenter;
import it.cnr.speech.textprocessing.TagCloudExtractor;
/**
 * DEPRECATED
 * @author Gianpaolo Coro
 *
 */
@Deprecated
public class DialogueAnalyserMFCC {

	public static void main(String[] args) throws Exception{
		//File audio = new File("./samples/audioCut.wav");
		File audio = new File("./samples/audio.wav");
		int nSpeakers = 2;
		float maxSilence = 0.1f; //best is 0.1
		float pitchWindowSec = 0.2f;
		float energyWindowSec = 0.1f;
		int windowSecToTakeForClustering = 2;
		boolean useQuestionsForStatisticalAnalysis = false;
		
		DialogueAnalyserMFCC dt = new DialogueAnalyserMFCC();
		dt.executeTaggingWorkflow(audio, nSpeakers, maxSilence);
		//dt.executeTaggingWorkflowEnergyPitch(audio, nSpeakers, maxSilence);
	}

	public void cleanFolder(File folder)  throws Exception{
		if (folder.exists() && folder.listFiles().length>0)
			FileUtils.deleteDirectory(folder);
	}
	
	public void executeTaggingWorkflow(File audioFile, int nSpeakers, float maxSilence) throws Exception{
		long t00 = System.currentTimeMillis();
		File tempFolder = new File("segments_T"); //new File("segments_"+UUID.randomUUID());
		long t0 = 0; long t1 = 0;
		TextDialogueManipulator dialogueAnalyser = new TextDialogueManipulator();
		cleanFolder(tempFolder);
		System.out.println("Temporary folder "+tempFolder);
		
		System.out.println("1- Segment based on energy");
		t0 = System.currentTimeMillis();
		EnergySegmenter energy = new EnergySegmenter();
		energy.setWindowIns(maxSilence);
		energy.segmentSignal(audioFile, tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		System.out.println("2- Recognize segments with Google");
		t0 = System.currentTimeMillis();
		GoogleSpeechToText asr= new GoogleSpeechToText();
		//asr.transcribeAll(tempFolder);
		asr.transcribeAllParallel(tempFolder);
		
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		System.out.println("3- Reporting transcription per segment");
		t0 = System.currentTimeMillis();
		
		dialogueAnalyser.removeEmptyFiles(tempFolder);
		dialogueAnalyser.reportTranscriptions(tempFolder);
		dialogueAnalyser.createOneTranscription(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		
		System.out.println("4- Cluster the segments");
		t0 = System.currentTimeMillis();
		SpeakersTagging st = new SpeakersTagging(tempFolder,nSpeakers);
		st.extractAllFeatures(tempFolder);
		st.clusterFeatures();
		File clusterFile = new File(tempFolder,"kmeansclustering.csv");
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		System.out.println("5- Create a subdialogue for each cluster");
		t0 = System.currentTimeMillis();
		dialogueAnalyser.createClusteredDialogues(clusterFile);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		System.out.println("6- Extracting keywords for each subdialogue");
		t0 = System.currentTimeMillis();
		TagCloudExtractor tagger = new TagCloudExtractor();
		tagger.annotateAllDialogues(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		System.out.println("7- Extracting the audio of the most important words per cluster");
		t0 = System.currentTimeMillis();
		AudioAnnotationSegmenter aas = new AudioAnnotationSegmenter();
		aas.extractAudioWordsForClusters(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		System.out.println("8- Detecting the most alarming cluster and annotate the original signal");
		t0 = System.currentTimeMillis();
		AudioSegmentsAnalyser asa = new AudioSegmentsAnalyser();
		asa.analyseClusters(tempFolder);
		String statistics = asa.statisticalAnalysisofAudioClusters(tempFolder);
		int warnCluster = asa.detectWarningCluster(statistics); //automatically detects the cluster to set as warning dialogue
		String [] labels = new String[nSpeakers+1];
		labels[warnCluster] = "Dialogue alteration";
		asa.tagSignalWithClusters(tempFolder,audioFile,labels);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		long t11 = System.currentTimeMillis();
		System.out.println("All done. Elapsed:"+((float)(t11-t00)/1000f)+"s");
	}
	
	
	public DialogueAnalyserMFCC(){
		
	}
	int nClusters; float maxSilence; float pitchWindowSec; float energyWindowSec; int windowSecToTakeForClustering; boolean useQuestionsForStatisticalAnalysis;
	
	public DialogueAnalyserMFCC(int nClusters, float maxSilence, float pitchWindowSec, float energyWindowSec, int windowSecToTakeForClustering, boolean useQuestionsForStatisticalAnalysis){
		this.nClusters = nClusters;
		this.maxSilence = maxSilence;
		this.pitchWindowSec=pitchWindowSec;
		this.energyWindowSec=energyWindowSec;
		this.windowSecToTakeForClustering=windowSecToTakeForClustering;
		this.useQuestionsForStatisticalAnalysis=useQuestionsForStatisticalAnalysis;
	}
	
	public void executeTaggingWorkflowEnergyPitch(File audioFile) throws Exception{
		//float pitchWindowSec = 0.2f;
		//float energyWindowSec = 0.1f;
		//int windowSecToTakeForClustering = 2;
		//boolean useQuestionsForStatisticalAnalysis = false;
		
		long t00 = System.currentTimeMillis();
		File tempFolder = new File("segmentation_with_EP");
		long t0 = 0; long t1 = 0;
		
		cleanFolder(tempFolder);
		System.out.println("Temporary folder "+tempFolder);
		System.out.println("1- Segment based on energy");
		t0 = System.currentTimeMillis();
		EnergySegmenter energy = new EnergySegmenter();
		energy.setWindowIns(maxSilence);
		energy.segmentSignal(audioFile, tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		
		System.out.println("2- Extract Pitch and Energy");
		t0 = System.currentTimeMillis();
		EnergyPitchExtractor epe = new EnergyPitchExtractor(energyWindowSec,pitchWindowSec);
		epe.extractEnergyPitchOfWaveFiles(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		
		System.out.println("3- Cluster the segments");
		t0 = System.currentTimeMillis();
		EnergyPitchClusterer epc = new EnergyPitchClusterer(windowSecToTakeForClustering, nClusters);
		epc.clusterEnergyPitch(tempFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0)/1000f)+"s");
		
		System.out.println("4- Detecting the most alarming cluster and annotate the original signal");
		t0 = System.currentTimeMillis();
		ClustersAnalyser ca = new ClustersAnalyser();
		int warnCluster = 1;//ca.detectWarningCluster(tempFolder, useQuestionsForStatisticalAnalysis);
		String [] labels = new String[SpeakersTagging.nclusters];
		labels[warnCluster] = "Dialogue alteration";
		AudioAnnotator aa = new AudioAnnotator();
		aa.tagAudioWithClusters(tempFolder, audioFile, labels);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		
		System.out.println("5- Transcribing");
		t0 = System.currentTimeMillis();
		File clusterFolder = new File(tempFolder,ClustersAnalyser.CLUSTERPREFIX+warnCluster);
		GoogleSpeechToText asr= new GoogleSpeechToText();
		asr.transcribeAll(clusterFolder);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		
		System.out.println("6- Estimating the most important words");
		t0 = System.currentTimeMillis();
		TextDialogueManipulator dialogueAnalyser = new TextDialogueManipulator();
		dialogueAnalyser.removeEmptyFiles(clusterFolder,false);
		dialogueAnalyser.reportTranscriptions(clusterFolder);
		//also adds question marks in the transcription
		dialogueAnalyser.createOneTranscription(clusterFolder,true);
		
		File dialog = new File(clusterFolder,TextDialogueManipulator.DIALOGFILE);
		File cloud = new File(audioFile.getAbsolutePath().replace(".wav", ".html"));
		
		TagCloudExtractor tagger = new TagCloudExtractor();
		tagger.run("it", dialog, true, cloud);
		t1 = System.currentTimeMillis();
		System.out.println("Finished in "+((float)(t1-t0))+"ms");
		
		long t11 = System.currentTimeMillis();
		System.out.println("All done. Elapsed:"+((float)(t11-t00)/1000f)+"s");
	}
	
}
