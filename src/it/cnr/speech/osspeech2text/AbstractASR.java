package it.cnr.speech.osspeech2text;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import it.cnr.speech.audiofeatures.AudioBits;
import marytts.util.data.audio.AudioConverterUtils;

public abstract class AbstractASR implements ASR{

	int numberOfThreadsToUse = 1;
	int overallTranscriptions = 0;

	public void setNumberOfThreadsToUse(int nThreads) {
		this.numberOfThreadsToUse=nThreads;
	}
	
	public synchronized void updateIdx() {
		overallTranscriptions++;
	}

	public abstract void transcribe(File audiofile, File output);
	
	
	public File downsample(File audio, int kHz) throws Exception{
		AudioBits ab = new AudioBits(audio);
		if (ab.getAudioFormat().getSampleRate() != kHz) {
			File newAudiofile = new File(audio.getAbsolutePath().replace(".wav", "_ds.wav"));
			AudioInputStream ais = AudioSystem.getAudioInputStream(audio);
			System.out.println("Downsampling");
			AudioInputStream out = AudioConverterUtils.downSampling(ais, 16000);
			AudioSystem.write(out, Type.WAVE, newAudiofile);
			out.close();
			ais.close();
			audio = newAudiofile;
		}
		
		return audio;
	}
	
	public void transcribeAllSequential(File filesDirectory) {
		
				File[] allfiles = filesDirectory.listFiles();
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav") && !f.getName().endsWith("_ds.wav") && !f.getName().contains("_spk_")) {
				File transcription = new File(f.getAbsolutePath().replace(".wav", "_transcription.txt"));
				transcribe(f, transcription);
			}
		}

		System.out.println("All transcriptions finished");

	}

	public void transcribeAll(File filesDirectory) {
		
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadsToUse);

		File[] allfiles = filesDirectory.listFiles();
		int counter = 0;
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav") && !f.getName().endsWith("_ds.wav") && !f.getName().contains("_spk_")) {
				File transcription = new File(f.getAbsolutePath().replace(".wav", "_transcription.txt"));

				DThread thread = new DThread(f, transcription);
				executorService.submit(thread);
				counter++;
			}
		}

		long time = 0;
		while (overallTranscriptions < counter) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
				transcribe(f, transcription);
				//transcribeWithAdaptation(f,transcription);
			} catch (Exception e) {
				System.out.println("Error executing thread " + f.getName());
			}
			updateIdx();
			return null;
		}

	}
	
}
