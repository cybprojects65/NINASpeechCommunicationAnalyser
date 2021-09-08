package it.cnr.speech.osspeech2text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.speakerid.Segment;
import edu.cmu.sphinx.speakerid.SpeakerCluster;
import edu.cmu.sphinx.speakerid.SpeakerIdentification;
import edu.cmu.sphinx.util.TimeFrame;
import it.cnr.speech.audiofeatures.AudioBits;
import it.cnr.speech.speakers.SpeakersExtraction;
import marytts.util.data.audio.AudioConverterUtils;

/**
 * A simple example that shows how to transcribe a continuous audio file that
 * has multiple utterances in it.
 */
public class SphinxASRNINA extends AbstractASR {

	public String language;
	public boolean useSpeakersSeparation = true;
	Transform adaptation = null;

	public SphinxASRNINA(String language, boolean useSpeakersSeparation) {
		this.language = language;
		this.useSpeakersSeparation = useSpeakersSeparation;
	}

	public SphinxASRNINA(String language) {
		this.language = language;
		this.useSpeakersSeparation = true;
	}

	public String adjust(String transcription) {

		transcription = transcription.replaceAll("venti .* la", "ventila");
		transcription = transcription.replaceAll("bene la", "ventila");
		transcription = transcription.replaceAll("esborso", "lo so");
		transcription = transcription.replaceAll(" p ", " ");
		transcription = transcription.replaceAll("oltre", "tre");
		transcription = transcription.replaceAll("bettega", "ventila");
		
		return transcription;
	}

	Configuration configuration = null;
	
	public synchronized void init() throws Exception {
		if (configuration == null) {
			configuration = new Configuration();

			if (language.equals("it-IT") || language.equals("it")) {
				
				/*
				configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/it/paisa_690k.dic");
				configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/it/paisa_full.lm.bin");
				*/
				/*
				configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/it/conndigits.dic");
				configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/it/conndigits.lm");
				*/
				//configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/it/singledigits.dic");
				//configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/it/conndigits.lm");
				
				//configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/isti_voxforge_it.cd_cont_1000_32");
				configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/it");
				configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/it/paisa_nina_691k.dic");
				configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/it/paisa_nina_full.lm.bin");
				configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/isti_voxforge_it.cd_cont_1000_32");
			} else {
				configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
				configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
				configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
			}

		}

	}

	@Override
	public void transcribe(File audiofile, File output) {
		if (useSpeakersSeparation)
			transcribePerSpeaker(audiofile, output);
		else
			transcribeStandard(audiofile, output);
	}

	public void transcribeStandard(File audiofile, File output) {

		String transcription = "{}";
		try {
			audiofile = downsample(audiofile, 16000);
			InputStream stream = new FileInputStream(audiofile);
			stream.skip(44);
			init();

			StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
			if (adaptation != null)
				recognizer.setTransform(adaptation);

			recognizer.startRecognition(stream);
			SpeechResult result;
			StringBuffer bf = new StringBuffer();
			System.out.println("Recognizing " + audiofile.getName());
			int nWint = 0;
			while ((result = recognizer.getResult()) != null) {
				String hyp = result.getHypothesis();
				if (hyp != null) {

					bf.append(hyp + " ");

					List<WordResult> nW = null;

					try {
						nW = result.getWords();
					} catch (Exception eee) {
					}
					nWint += nW == null ? 0 : nW.size();

				}
				/*
				 * for alignment
				 * 
				 * System.out.format("Hypothesis: %s\n", result.getHypothesis());
				 * System.out.println("List of recognized words and their times:"); for
				 * (WordResult r : result.getWords()) { System.out.println(r); }
				 */
				/*
				 * System.out.println("Best 3 hypothesis:"); for (String s : result.getNbest(3))
				 * System.out.println(s);
				 */
				
			}
			System.out.println("Number of words in the lattice->" + nWint);
			recognizer.stopRecognition();
			stream.close();

			if (nWint > 0) { // report transcription only if the LM lattice is good
				transcription = bf.toString().trim();
				transcription = adjust(transcription);
			}

			System.out.println("OVERALL RECOGNITION: " + transcription);

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			FileWriter fw = new FileWriter(output);
			fw.write(transcription);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void transcribePerSpeaker(File audio, File output) {
		try {

			audio = downsample(audio, 16000);
			
			System.out.println("Extracting Speakers");
			SpeakersExtraction sps = new SpeakersExtraction();
			String audioPrefix = audio.getName().replace(".wav", "");
			sps.extractSpeakers(audio);

			File[] allFiles = audio.getParentFile().listFiles();
			List<File> allTranscriptions = new ArrayList<>();
			for (File f : allFiles) {
				if (f.getName().startsWith(audioPrefix) && f.getName().contains("_spk_")
						&& !f.getName().endsWith(".txt")) {
					// int spk =
					// Integer.parseInt(f.getName().substring(f.getName().indexOf("_spk_")+5,f.getName().indexOf(".wav")));
					File tr = new File(f.getParentFile(), "spk" + UUID.randomUUID());
					// File tr = new File(output.getAbsolutePath().replace("_transcription",
					// "_spk_"+spk+"_transcription"));
					transcribeStandard(f, tr);
					allTranscriptions.add(tr);
				}
			}

			StringBuffer sb = new StringBuffer();
			for (File trs : allTranscriptions) {
				String tr = new String(Files.readAllBytes(trs.toPath()));
				if (!tr.equals("{}")) {
					sb.append(tr + ". ");
				}
				trs.delete();

			}

			FileWriter fw = new FileWriter(output);
			fw.write(sb.toString());
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Transform adapt(File audio) {

		try {
			init();
			audio = downsample(audio, 16000);
			StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
			Stats stats = recognizer.createStats(1);
			SpeechResult result;
			StringBuffer sba = new StringBuffer();
			recognizer.startRecognition(audio.toURI().toURL().openStream());
			while ((result = recognizer.getResult()) != null) {
				sba.append(result.getHypothesis() + " ");
				try {
					stats.collect(result);
					System.out.println("Collecting stats...");
				}catch(Exception st) {System.out.println("Failed step to collect stats..");}
			}
			recognizer.stopRecognition();

			Transform profile = stats.createTransform();

			System.out.format("ORIGINAL TRANSCRIPTION: %s\n", sba.toString().trim());

			adaptation = profile;
			return profile;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void transcribeWithAdaptation(File audio, File output) {

		String transcription = "{}";

		try {
			init();
			audio = downsample(audio, 16000);
			
			SpeakerIdentification sd = new SpeakerIdentification();
			InputStream stream4cluster = audio.toURI().toURL().openStream();
			ArrayList<SpeakerCluster> speakers = sd.cluster(stream4cluster);
			System.out.println("Detected speakers: " + speakers.size() + " in " + audio.getName());
			stream4cluster.close();

			if (speakers.size() <= 0) {
				System.out.println("Transcribing directly");
				SphinxASRNINA asr = new SphinxASRNINA(language);
				asr.transcribe(audio, output);
				return;
			}

			StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);

			TimeFrame t;
			SpeechResult result;

			SpeakerCluster bestSpeaker = null;
			int maxdurspk = 0;
			// the best speaker is the one with the longest interval
			int nspk = 0;
			int spkIdx = 0;
			for (SpeakerCluster spk : speakers) {
				ArrayList<Segment> segments = spk.getSpeakerIntervals();
				int maxdur = 0;

				System.out.print("Speaker " + nspk + ": ");
				for (Segment s : segments) {
					System.out.print(" " + s.getStartTime() + " (" + s.getLength() + ") ");
					if (s.getLength() > maxdur)
						maxdur = s.getLength();
				}
				System.out.println();
				if (maxdurspk < maxdur) {
					maxdurspk = maxdur;
					bestSpeaker = spk;
					spkIdx = nspk;
				}

				nspk++;
			}
			System.out.println("ADAPTING to Speaker" + spkIdx + "..");
			SpeakerCluster spk = bestSpeaker;
			Transform profile = null;

			Stats stats = recognizer.createStats(1);
			ArrayList<Segment> segments = spk.getSpeakerIntervals();

			StringBuffer sba = new StringBuffer();
			for (Segment s : segments) {
				long startTime = s.getStartTime();
				long endTime = s.getStartTime() + s.getLength();
				t = new TimeFrame(startTime, endTime);

				recognizer.startRecognition(audio.toURI().toURL().openStream(), t);
				while ((result = recognizer.getResult()) != null) {
					// System.out.format("Original Hypothesis: %s\n",result.getHypothesis());
					sba.append(result.getHypothesis() + " ");
					stats.collect(result);
				}
				recognizer.stopRecognition();
			}

			// Create the Transformation
			profile = stats.createTransform();
			recognizer.setTransform(profile);
			StringBuffer sb = new StringBuffer();
			System.out.println("RECOGNIZING..");
			for (Segment seg : segments) {

				long startTime = seg.getStartTime();
				long endTime = seg.getStartTime() + seg.getLength();
				System.out.println("Seg: " + startTime + " : " + endTime);
				t = new TimeFrame(startTime, endTime);

				// Decode again with updated SpeakerProfile
				recognizer.startRecognition(audio.toURI().toURL().openStream(), t);

				while ((result = recognizer.getResult()) != null) {
					// System.out.format("Hypothesis: %s\n",result.getHypothesis());
					sb.append(result.getHypothesis() + " ");
				}
				recognizer.stopRecognition();

			}

			transcription = sb.toString().trim();
			System.out.format("ORIGINAL TRANSCRIPTION: %s\n", sba.toString().trim());
			System.out.format("OVERALL TRANSCRIPTION: %s\n", transcription);

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			FileWriter fw = new FileWriter(output);
			fw.write(transcription);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String args[]) {

		File sample = new File("./samples/sampleUNO16k.wav");
		File sample2 = new File("./samples/avanti.wav");
		File sample3 = new File("C:\\Users\\Gianpaolo Coro\\Desktop\\testgp1.wav");
		File sample4 = new File("C:\\Users\\Gianpaolo Coro\\Desktop\\vale.wav");
		// File nina = new
		// File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds.wav");
		File nina = new File(
				"D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds1.wav");

		nina = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audio\\cluster1\\audio_segment_267_ds_spk_2.wav");
		nina = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audio\\cluster1\\audio_segment_267.wav");
		
		//File output = new File(sample.getParent(), sample.getName().replace(".wav", "_transcription.txt"));
		File output = new File("test_OASR_transcription.txt");
		
		File output2 = new File(sample2.getParent(), sample2.getName().replace(".wav", "_transcription.txt"));

		File denoised = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds_denoised.wav");
		File noisy = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds.wav");
		
		boolean splitSpeakers = true;
		SphinxASRNINA asr = new SphinxASRNINA("it-IT", splitSpeakers);
		asr.setNumberOfThreadsToUse(1);
		asr.useSpeakersSeparation=false;
		//asr.transcribe(sample, output);
		// asr.transcribe(sample, output);
		// asr.transcribe(sample2, output2);
		// asr.transcribe(sample2, output2);
		// asr.transcribe(sample3, output2);
		// asr.transcribe(sample3, output2);
		// asr.transcribe(sample4, output2)
		//asr.transcribeWithAdaptation(nina, output);
		//asr.adapt(nina);
		//asr.transcribe(nina, output);

		// System.out.println(asr.adjust("aiuto dai venti e p la uno due"));
		// asr.transcribe(nina, output);

		// asr.transcribeAll(new File("./samples/folderofwaves//"));
		
		//asr.transcribe(noisy, output);
		//asr.transcribe(denoised, output);
		
		 //asr.transcribeAll(new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon digits\\Wave_Cifre_Isolate_4_CNR\\"));
		
		//asr.transcribeAll(new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test_4_CNR"));
		
		
		//asr.transcribe(new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon digits\\Wave_Cifre_Isolate_4_CNR\\SA005CI1.wav"), output);
		 
		File test = new File("sampleLongNews16k.wav");
		asr.transcribe(test, new File("sampleLongNews16k.txt"));
		
	}
}
