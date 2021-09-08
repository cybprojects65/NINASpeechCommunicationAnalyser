package it.cnr.speech.audiofeatures;

import java.io.File;
import java.util.Arrays;

import edu.cmu.sphinx.frontend.DataProcessingException;
import marytts.signalproc.effects.VolumeEffect;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;

public class NoiseReduction {

	public static void main(String[] args) throws Exception{
		//File waveFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds.wav");
		//File waveFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds_spk_1.wav");
		//File waveFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds_copy.wav");
		File waveFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_copy.wav");
		NoiseReduction nr = new NoiseReduction();
		/*
		File waveFileOutput = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\audioCut_copy2\\cluster0\\audio_segment_113_ds_denoised.wav");
		
		AudioBits ab = new AudioBits(waveFile);
		double [] audioSamples = ab.getDoubleVectorAudio();
		
		
		double [] denoised = nr.process(audioSamples);
		
		short[] denoisedShort = nr.getShortVectorAudio(denoised);
		if (waveFileOutput.exists())
			waveFileOutput.delete();
		
		AudioWaveGenerator.generateWaveFromSamples(denoisedShort, waveFileOutput, ab.getAudioFormat());
		 */
		nr.denoise(waveFile);
		
		System.out.println("All done.");
	}
	
	public void denoise(File waveFile) throws Exception{
		denoise(waveFile,waveFile);
	}
	
	public void denoise(File waveFile, File outputWaveFile) throws Exception{
		
		AudioBits ab = new AudioBits(waveFile);
		double [] audioSamples = ab.getDoubleVectorAudio();
		NoiseReduction nr = new NoiseReduction();
		
		double [] denoised = nr.process(audioSamples);
		
		short[] denoisedShort = nr.getShortVectorAudio(denoised);
		
		if (outputWaveFile.exists())
			outputWaveFile.delete();
		
		AudioWaveGenerator.generateWaveFromSamples(denoisedShort, outputWaveFile, ab.getAudioFormat());
		
	}
	
	public short[] getShortVectorAudio(double [] samples) {

		int length = samples.length;

		short[] shortSamples = new short[length];
		
		for (int i = 0; i < length; i++) {
			Double tmp = samples[i];
			shortSamples[i] = tmp.shortValue();
		}
		
		return shortSamples;
	}
	
	double[] power;
    double[] noise;
    double[] floor;
    double[] peak;

    double lambdaPower=0.7;
    double lambdaA=0.995;
    double lambdaB = 0.5;
    double lambdaT = 0.85;
    double muT= 0.2;
    double maxGain= 20.0;
    int smoothWindow= 4;
    final static double EPS = 1e-10;
    
	public double [] process(double [] input) throws DataProcessingException {
	        
	        int length = input.length;

	        initStatistics(input, length);

	        updatePower(input);

	        estimateEnvelope(power, noise);

	        double[] signal = new double[length];
	        int i;
	        
	        for (i = 0; i < length; i++) {
	            signal[i] = Math.max(power[i] - noise[i], 0.0);
	        }

	        estimateEnvelope(signal, floor);

	        tempMasking(signal);

	        powerBoosting(signal);

	        double[] gain = new double[length];
	        for (i = 0; i < length; i++) {
	            gain[i] = signal[i] / (power[i] + EPS);
	            gain[i] = Math.min(Math.max(gain[i], 1.0 / maxGain), maxGain);
	        }
	        
	        double[] smoothGain = smooth(gain);
	        
	        /*
	        double[] output = new double[length];
	        for (i = 0; i < length; i++) {
	            output[i] = input[i]*smoothGain[i];
	        }
	        
	         */
	        
	        for (i = 0; i < length; i++) {
	            input[i] *= smoothGain[i];
	        }
	        
	        
	        VolumeEffect ve = new VolumeEffect();
	        ve.setParams("amount:5.0;");
	        //System.out.println(ve.getHelpText());
	        DoubleDataSource dds = new BufferedDoubleDataSource(input);
	        
	        DoubleDataSource volumed = ve.process(dds);
	        
	        input = volumed.getAllData();
	        
	        
	        return input;
	    }

		
		
	    private double[] smooth(double[] gain) {
	        double[] result = new double[gain.length];
	        for (int i = 0; i < gain.length; i++) {
	            int start = Math.max(i - smoothWindow, 0);
	            int end = Math.min(i + smoothWindow + 1, gain.length);
	            double sum = 0.0;
	            for (int j = start; j < end; j++) {
	                sum += gain[j];
	            }
	            result[i] = sum / (end - start);
	        }
	        return result;
	    }

	    private void powerBoosting(double[] signal) {
	        for (int i = 0; i < signal.length; i++) {
	            if (signal[i] < floor[i])
	                signal[i] = floor[i];
	        }
	    }

	    private void tempMasking(double[] signal) {
	        for (int i = 0; i < signal.length; i++) {
	            double in = signal[i];

	            peak[i] *= lambdaT;
	            if (signal[i] < lambdaT * peak[i])
	                signal[i] = peak[i] * muT;

	            if (in > peak[i])
	                peak[i] = in;
	        }
	    }

	    private void updatePower(double[] input) {
	        for (int i = 0; i < input.length; i++) {
	            power[i] = lambdaPower * power[i] + (1 - lambdaPower) * input[i];
	        }
	    }

	    private void estimateEnvelope(double[] signal, double[] envelope) {
	        for (int i = 0; i < signal.length; i++) {
	            if (signal[i] > envelope[i])
	                envelope[i] = lambdaA * envelope[i] + (1 - lambdaA) * signal[i];
	            else
	                envelope[i] = lambdaB * envelope[i] + (1 - lambdaB) * signal[i];
	        }
	    }

	    private void initStatistics(double[] input, int length) {
	        power = Arrays.copyOf(input, length);
	        noise = Arrays.copyOf(input, length);
	        floor = new double[length];
	        peak = new double[length];
	        for (int i = 0; i < length; i++) {
	            floor[i] = input[i] / maxGain;
	        }
	    }

}
