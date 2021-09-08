package it.cnr.speech.speakers;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import edu.cmu.sphinx.demo.speakerid.SpeakerIdentificationWideRange;
import edu.cmu.sphinx.speakerid.Segment;
import edu.cmu.sphinx.speakerid.SpeakerCluster;
import it.cnr.speech.audiofeatures.AudioBits;
import it.cnr.speech.audiofeatures.AudioWaveGenerator;
import it.cnr.speech.googlespeech2text.GoogleSpeechToText;
import it.cnr.speech.osspeech2text.SphinxASRNINA;
import marytts.util.data.audio.AudioConverterUtils;

public class SpeakersExtraction {

	public static void main1(String[] args) throws Exception {
		//String nina = "D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_126_ds.wav";
		String nina = "D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds.wav";
		SpeakersExtraction sps = new SpeakersExtraction();
		sps.extractSpeakers(new File(nina));
		
		File [] allFiles = new File(nina).getParentFile().listFiles();
		String words[] = {"un","uno","due","tre","ventila","venti","modino","mettiti","metti"};
		List<String> alltr = new ArrayList<>();
		int nwords = 0;
		int totwords = 0;
		for (File f: allFiles) {
			
			if (f.getName().contains("_spk_")&& !f.getName().endsWith(".txt")) {
				
				SphinxASRNINA asr = new SphinxASRNINA("it-IT");
				File tr = new File(f.getAbsolutePath().replace(".wav", ".txt"));
				asr.transcribe(f, tr);
				String transcription = new String(Files.readAllBytes(tr.toPath()));
				alltr.add(transcription);
				totwords+=transcription.split(" ").length;
				
				for (String word:words) {
					if (transcription.contains(word))
						nwords++;
				}
				//GoogleSpeechToText asr = new GoogleSpeechToText();
				//asr.recognize(f, new File(f.getAbsolutePath().replace(".wav", ".txt")));
			}
			
		}
		float acc = (float)nwords/(float)totwords;
		
		System.out.println("Acc: "+acc);
		System.out.println(alltr.toString().replace(", ", "\n"));
	}
	
	public static void main(String[] args) throws Exception {
		String nina = "D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\speakers_separation\\audio.wav";
		String audioFolder = "./audio/";
		SpeakersExtraction sps = new SpeakersExtraction();
		//sps.extractSpeakers(new File(nina));
		sps.extractSpeakersBatch(new File(audioFolder));
	}
	
	public void extractSpeakersAndDeleteOriginal (File waveFile) throws Exception{
		
		AudioBits ab = new AudioBits(waveFile);
		if (ab.getAudioFormat().getSampleRate() != 16000) {
			File newAudiofile = new File(waveFile.getAbsolutePath().replace(".wav", "_ds.wav"));
			AudioInputStream ais = AudioSystem.getAudioInputStream(waveFile);
			System.out.println("Downsampling");
			AudioInputStream out = AudioConverterUtils.downSampling(ais, 16000);
			AudioSystem.write(out, Type.WAVE, newAudiofile);
			out.close();
			ais.close();
			waveFile.delete();
			waveFile = newAudiofile;
		}
		
		extractSpeakers(waveFile);
		waveFile.delete();
		
	}
	
	public File extractSpeakersBatch(File waveFilesFolder) throws Exception{
		
		File[] allFiles = waveFilesFolder.listFiles();
		File outputFolder = new File(waveFilesFolder,"speakers_audio");
		outputFolder.mkdir();
		System.out.println("Separating speakers");
		for (File f: allFiles) {
			
			if (f.getName().endsWith(".wav") && f.lastModified()!=0) {
				System.out.println(f.getName());
				List<File> spkFiles = extractSpeakers(f);
				for (File sf:spkFiles){
					System.out.println("\t->"+sf.getName());
					Files.move(sf.toPath(), new File(outputFolder,sf.getName()).toPath());
				}				
			}
		}
		
		System.out.println("Speakers separation done.");
		return outputFolder;
	}
	
	public List<File> extractSpeakers (File waveFile) throws Exception{
		
		AudioBits ab = new AudioBits(waveFile);
		if (ab.getAudioFormat().getSampleRate() != 16000) {
			File newAudiofile = new File(waveFile.getAbsolutePath().replace(".wav", "_ds.wav"));
			AudioInputStream ais = AudioSystem.getAudioInputStream(waveFile);
			System.out.println("Downsampling");
			AudioInputStream out = AudioConverterUtils.downSampling(ais, 16000);
			AudioSystem.write(out, Type.WAVE, newAudiofile);
			out.close();
			ais.close();
			waveFile = newAudiofile;
		}
				
		SpeakerIdentificationWideRange sd = new SpeakerIdentificationWideRange();
		AudioBits audio = new AudioBits(waveFile);
		short [] signal = audio.getShortVectorAudio();
		int samplingfreq = (int)audio.getAudioFormat().getSampleRate();
		float signalLengthSec = (float)signal.length/(float)samplingfreq;
		System.out.println("Signal is "+signalLengthSec+" seconds and "+signal.length+" samples long");
		
		URL url = waveFile.toURI().toURL();
		System.out.println("Clustering audio "+waveFile.getName());
		ArrayList<SpeakerCluster> clusters = sd.cluster(url.openStream());
        int idx = 0;
        List<File> spkfiles = new ArrayList<>();
        
        for (SpeakerCluster spk : clusters) {
        	idx++;
        	System.out.println("Processing Speaker "+idx);
        	ArrayList<Segment> segments = spk.getSpeakerIntervals();
	        List<short[]> signalSegments = new ArrayList<>();
	        int s=0;
	        int overallSegmentsLengths = 0;
	        System.out.println("Speaker "+idx+" has "+segments.size()+" segments");
	        
	        for (Segment seg : segments) {
	        	s++;
	        	
	        	float fadet = 0.100f;//0.100f best without fading
	        	int silence = (int)(0.5*samplingfreq);
	        	
	        	float t0 = (float)seg.getStartTime()/1000f;
	          	float t1 = (float)(seg.getStartTime()+seg.getLength())/1000f;
	          	
	            System.out.println("Segment n. "+s+": "+ t0+ " s - " +t1+" s");
	            int t0Idx = (int)((t0-fadet)*samplingfreq);
	            int t0IdxT = (int)((t0)*samplingfreq);
	            int t1Idx = (int)((t1+fadet)*samplingfreq);
	            int t1IdxT = (int)((t1)*samplingfreq);
	            t0Idx = Math.max(t0Idx, 0);
	            t1Idx = Math.min(t1Idx, signal.length-1);
	            
	            if (t1Idx<=t0Idx)
	            	continue;
	            
	            
	            boolean fadeinout = false;
	            short subsignal []=new short[(t1Idx-t0Idx+1)+2*silence];
	            int k = 0;
	            for (int t=t0Idx;t<=t1Idx;t++) {
	            	float multiplier = 1; 
	            	if (fadeinout) {
	            	if (t<t0IdxT)
	            		multiplier = (float)(t-t0Idx)/(float)(t0IdxT-t0Idx);
	            	else if (t>t1IdxT)
		            		multiplier = (float)(t1Idx-t)/(float)(t1Idx-t1IdxT);
	            	}
	           
	            	subsignal[k+silence] = (short) (multiplier * signal[t]); 
	            	k++;
	            }
	           
	            overallSegmentsLengths+=subsignal.length;
	            signalSegments.add(subsignal);
	        }
	        
	        System.out.println("Rebuilding the whole speaker's signal");
	        short[] speakerSignal = new short [overallSegmentsLengths];
	        int j = 0;
	        
	        for (short[] subsig : signalSegments) {
	        	
	        	for (int g=0;g<subsig.length;g++) {
	        		speakerSignal [g+j] = subsig[g]; 
	        	}
	        	
	        	j +=subsig.length;
	        }
	        
	        File speakerWave = new File(waveFile.getAbsolutePath().replace(".wav", "_spk_"+idx+".wav"));
	        System.out.println("Saving speaker signal to "+speakerWave.getName());
	        AudioWaveGenerator.generateWaveFromSamples(speakerSignal, speakerWave, audio.getAudioFormat());
	        spkfiles.add(speakerWave);
        }
        
        System.out.println("Speakers separation done.");
        
        return spkfiles;
	}

}
