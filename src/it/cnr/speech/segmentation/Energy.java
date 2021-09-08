package it.cnr.speech.segmentation;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import it.cnr.speech.audiofeatures.AudioBits;
import it.cnr.speech.audiofeatures.AudioWaveGenerator;
import it.cnr.speech.utils.Utils;

public class Energy {

	public static void main(String[] args) throws Exception {

		// File audioFile = new File("./samples/avanti.wav");
		//File audioFile = new File("./samples/audio.wav");
		// File audioFile = new File("./samples/audioHiVol.wav");
		// File audioFile = new File("./samples/audioEqualLoud.wav");
		// File audioFile = new File("./samples/audioMaxVol.wav");
		// File audioFile = new File("./samples/audioDenoise.wav");
		// File audioFile = new File("./samples/audio16.wav");
		//new Energy().segmentSignal(audioFile, new File("segments"));
		//File audioFile = new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test\\SA009CD1.wav");
		//File audioFile = new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test\\SA018CD2.wav");
		//File audioFile = new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test\\SA018CN2.wav");
		File audioFile = new File("D:\\WorkFolder\\Experiments\\Speech Recognition Speecon\\Speecon connected digits\\test\\SA037CN2.wav");
		
		new Energy().segmentSignal(audioFile, new File("segmentsSpeecon"));

	}

	AudioBits bits;
	short[] signal;
	public double energyThr = 0.001;
	public float windowIns = 0.3f;
	public static String ENERGYSEGMENTATIONFILE = "energy_segmentation.csv";
	public static String DERIVATIVEFILE = "derivative.csv";

	public float getWindowIns() {
		return windowIns;
	}

	public void setWindowIns(float windowIns) {
		this.windowIns = windowIns;
	}

	public void segmentSignal(File audioFile, File outputFolder) throws Exception {

		double[] normalisedEnergyCurve = energyCurve(windowIns, audioFile, false);
		double[] derivative = Utils.derivative(normalisedEnergyCurve);
		double meanEnergy = Utils.mean(normalisedEnergyCurve);
		// double energyThr = 0.001;
		Utils.writeSignal2File(derivative, new File(DERIVATIVEFILE));
		// delete all segments
		if (!outputFolder.exists())
			outputFolder.mkdir();

		float sfrequency = bits.getAudioFormat().getSampleRate();
		int waveCounter = 0;
		int ntries = 0;
		int maxTries = 100;
		int minNumberOfWavesToFind = 3;
		double maxSNR = 0;
		
		while (ntries < maxTries) {
			maxSNR = 0;
			double time0 = 0;
			waveCounter = 0;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFolder, ENERGYSEGMENTATIONFILE), true));

			for (int i = 1; i < normalisedEnergyCurve.length; i++) {
				if (derivative[i - 1] < 0 && normalisedEnergyCurve[i] < energyThr) {
					// normalisedEnergyCurve[i]=2;
					double time1 = ((double) (i) * Math.round(windowIns * sfrequency)) / (double) sfrequency;
					// System.out.println("Segment " + time1 + " : " +
					// normalisedEnergyCurve[i]);
					// save signal segment t0 t1
					int i0 = (int) (time0 * sfrequency);
					int i1 = (int) (time1 * sfrequency);
					short[] subsignal = new short[i1 - i0 + 1];
					for (int k = i0; k <= i1; k++) {
						subsignal[k - i0] = signal[k];
					}
					try {
						double SNR = 10*Math.log10(normalisedEnergyCurve[i-1]/normalisedEnergyCurve[i]);
						if (SNR>maxSNR)
							maxSNR=SNR;
						//System.out.println("SNR = "+SNR);
						File outputWaveFile = new File(outputFolder, "audio_segment_" + waveCounter + ".wav");
						AudioWaveGenerator.generateWave44khzFromSamples(subsignal, outputWaveFile, bits.getAudioFormat());
						bw.write(time0 + " " + time1 + " " + outputWaveFile.getName() + "\n");
					} catch (Exception e) {
						e.printStackTrace();
					}

					time0 = time1;
					waveCounter++;
				}
				if (derivative[i - 1] < 0) {
					//System.out.println("Energy with neg der " + " : " + normalisedEnergyCurve[i] + " vs " + energyThr);
				}
			}
			bw.close();
			if (waveCounter > minNumberOfWavesToFind)
				ntries = maxTries;
			else {
				ntries++;
				System.out.println("Too few segments using energy threshold: " + energyThr + " mean energy " + meanEnergy);
				energyThr = energyThr + energyThr;
				System.out.println("Retrying segmentation using energy: " + energyThr);
			}

		} // end while

		if (waveCounter == 0)
			throw new Exception("Audio is too low for segmentation");
		
		System.out.println("SNR:"+maxSNR);
		// Utils.writeSignal2File(normalisedEnergyCurve, new
		// File(outputFolder,"outputsignal.csv"));
		
	}

	public double[] energyCurve(float windowInMs, File audioFile, boolean modelInitialSilence) {
		return energyCurve(windowInMs, audioFile, modelInitialSilence, true);
	}

	public double[] energyCurve(float windowInMs, File audioFile, boolean modelInitialSilence, boolean normalize) {
		// extract features
		bits = new AudioBits(audioFile);
		signal = bits.getShortVectorAudio();
		float sfrequency = bits.getAudioFormat().getSampleRate();
		double[] nrg = energyCurve(windowInMs, signal, sfrequency, modelInitialSilence, normalize);
		// bits.deallocateAudio();
		return nrg;
	}

	public double[] energyCurve(float windowIns, short[] signal, float sfrequency, boolean modelInitialSilence, boolean normalize) {

		// initial energy
		int windowSamplesInit = Math.round(0.1f * sfrequency);
		short[] signalSliceInit = new short[windowSamplesInit];
		for (int j = 0; j < windowSamplesInit; j++) {
			signalSliceInit[j] = signal[j];
		}
		double silenceEnergy = 1;
		if (modelInitialSilence)
			silenceEnergy = energy(signalSliceInit);

		if (silenceEnergy == 0)
			silenceEnergy = 1;

		int windowSamples = Math.round(windowIns * sfrequency);
		int steps = signal.length / windowSamples;

		// trace energy curve
		double[] energySignal = new double[steps];
		double maxEnergy = -Double.MAX_VALUE;
		for (int i = 0; i < steps; i++) {
			int currentIdx = i * windowSamples;
			short[] signalSlice = new short[windowSamples];
			for (int j = 0; j < windowSamples; j++) {
				signalSlice[j] = signal[currentIdx + j];
			}

			energySignal[i] = energy(signalSlice) / silenceEnergy;

			if (energySignal[i] > maxEnergy)
				maxEnergy = energySignal[i];
		}

		if (normalize) {
			for (int i = 0; i < steps; i++) {
				energySignal[i] = energySignal[i] / maxEnergy;
			}
		}
		return energySignal;
	}

	public static double energy(short[] signal) {

		double energy = 0;
		for (int g = 0; g < signal.length; g++) {
			energy += signal[g] * signal[g];
		}
		energy = energy / (double) signal.length;
		return energy;
	}

}
