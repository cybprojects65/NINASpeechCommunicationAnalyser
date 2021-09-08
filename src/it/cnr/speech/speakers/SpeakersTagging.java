package it.cnr.speech.speakers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import it.cnr.speech.audiofeatures.AudioBits;
import it.cnr.speech.clustering.KMeans;
import it.cnr.speech.segmentation.AudioSegmentsAnalyser;
import it.cnr.speech.utils.Utils;

public class SpeakersTagging {
	File workingFolder;
	int nSpeakers;

	public SpeakersTagging(File workingFolder, int nSpeakers) {
		this.workingFolder = workingFolder;
		this.nSpeakers = nSpeakers;
	}

	public void extractFeatures(File audioFile) throws Exception {

		System.out.println("Features Extraction: Extracting audio file " + audioFile.getName());
		AudioBits audio = new AudioBits(audioFile);
		System.out.println("Features Extraction: Extracting short values");
		short vettShort[] = audio.getShortVectorAudio();
		System.out.println("Features Extraction: Cutting start/end");

		System.out.println("Features Extraction: Extracting MFCC");
		it.cnr.speech.audiofeatures.MfccExtraction mfcc = new it.cnr.speech.audiofeatures.MfccExtraction(audio.getAudioFormat().getSampleRate(), 13);
		double matr[][] = mfcc.extractMFCC(vettShort);

		System.out.println("Features Extraction: done for " + audioFile.getName());
		System.out.println("Features Extraction: features extracted " + matr.length);
		File mfccFile = new File(audioFile.getAbsolutePath().replace(".wav", ".mfcc"));
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(mfccFile));
		oos.writeObject(matr);
		oos.close();
		audio.deallocateAudio();
	}

	public void extractAllFeatures(File folder) throws Exception {
		File[] allfiles = folder.listFiles();

		for (File f : allfiles) {
			if (f.getName().endsWith(".wav")) {
				extractFeatures(f);
			}
		}
	}

	public void clusterFeatures() throws Exception {
		File[] allfiles = workingFolder.listFiles();
		float fs = 0;
		for (File f : allfiles) {
			if (f.getName().endsWith(".wav")) {
				AudioBits ab = new AudioBits(f);
				fs = ab.getAudioFormat().getSampleRate();
				ab.deallocateAudio();
			}
		}

		allfiles = Utils.getFiles(allfiles, ".mfcc");
		Utils.sortByNumber(allfiles);

		int nmfcc = Math.round(500f * 2 / 11.6f); // 500 ms

		double[][] allFeatures = new double[allfiles.length][];
		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		int nFeaturesCounter = 0;
		for (File f : allfiles) {
			System.out.println("Extracted MFCCs from " + f.getName());
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
			double[][] mfccs = (double[][]) ois.readObject();
			/*
			 * int maxmfcc = 13; double[][] cutmfccs = new
			 * double[mfccs.length][maxmfcc]; for (int
			 * g=0;g<cutmfccs.length;g++) { for (int d=0;d<maxmfcc;d++) {
			 * cutmfccs[g][d]=mfccs[g][d]; } }
			 * 
			 * mfccs=cutmfccs;
			 */
			ois.close();
			if (mfccs.length < nmfcc)
				continue;

			System.out.println("Extracted MFCCs #: " + mfccs.length);
			System.out.println("Single MFCC length " + mfccs[0].length);
			// take the central piece ms

			int i0 = ((mfccs.length - nmfcc) / 2) + 1;
			double[] centralFeatures = new double[nmfcc * mfccs[0].length];
			System.out.println("Total length of features " + centralFeatures.length);
			System.out.println("Start MFCC Index " + i0);
			int k = 0;
			for (int i = 0; i < nmfcc; i++) {

				double[] mfccvector = mfccs[i0 + i];
				for (int j = 0; j < mfccvector.length; j++) {
					// System.out.print(k+" ");
					centralFeatures[k] = mfccvector[j];
					k++;
				}
				// System.out.print(" | ");
			}
			labels.put("" + (nFeaturesCounter + 1), f.getName().replace(".mfcc", ""));
			allFeatures[nFeaturesCounter] = centralFeatures;
			System.out.println("\nFinal k index " + k);
			nFeaturesCounter++;
			// break;
		}

		System.out.println("Submitting overall " + allFeatures.length + " features with length " + allFeatures[0].length);

		KMeans kmeans = new KMeans(workingFolder, labels);
		int nclusters = nSpeakers + 1; // most uniform distribution -
										// difformities:
										// 2:0.10;3:0.07;4:0.09;5:0.11
		kmeans.compute(nclusters, 10, 10000, 2, allFeatures);

		int sum = 0;
		for (String key : kmeans.pointsPerCluster.keySet()) {
			int np = kmeans.pointsPerCluster.get(key);
			sum = sum + np;
		}
		float uniformityScore = 1f / (float) nclusters;
		System.out.println("Uniformity: " + uniformityScore);
		float difformity = 0;
		for (String key : kmeans.pointsPerCluster.keySet()) {
			int np = kmeans.pointsPerCluster.get(key);
			float score = ((float) np / (float) sum);
			System.out.println("C1%:" + score);
			// difformity =
			// difformity+(Math.abs(score-uniformityScore)/uniformityScore);
			difformity = difformity + (Math.abs(score - uniformityScore));
		}

		difformity = (float) difformity / (float) kmeans.pointsPerCluster.keySet().size();
		System.out.println("Difformity: " + difformity);
		/*
		 * XMeans xmeans = new XMeans(workingFolder,labels); xmeans.compute(2,2,
		 * 10, 1, allFeatures);
		 */

	}

	public static int nclusters = 3;
	public void clusterEnergyPitch() throws Exception {

		int windowSecToTake = 2; //seconds;
		
		int nEnergyFeaturesToTake = (int)((float)windowSecToTake/AudioSegmentsAnalyser.energyWindowSec);
		System.out.println("Smallest number of energy features to take " + nEnergyFeaturesToTake);
		//double[][] allFeatures = new double[nFiles][];
		
		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		int nFeaturesCounter = 0;
		List<String> allEnergies = Files.readAllLines(new File(workingFolder, "energy.csv").toPath());
		List<double[]>allFeaturesList = new ArrayList<>();
		
		for (String energyLine : allEnergies) {
			if (nFeaturesCounter == 0) {
				nFeaturesCounter++;
				continue;
			}

			String[] elements = energyLine.split(",");
			if (elements.length > nEnergyFeaturesToTake) {
				String label = elements[0];
				double[] features = new double[elements.length - 1];
				for (int i = 1; i < elements.length; i++) {
					features[i - 1] = Double.parseDouble(elements[i]);
				}

				System.out.println("Extracted Energy Features #: " + features.length);

				int i0 = ((features.length - nEnergyFeaturesToTake) / 2);
				double[] centralFeatures = new double[nEnergyFeaturesToTake];
				
				for (int i = 0; i < nEnergyFeaturesToTake; i++) {
					double mfccvector = features[i0 + i];
					centralFeatures[i]=mfccvector; 
				}
				
				System.out.println("Total length of features " + centralFeatures.length);
				System.out.println("Start ENERGY Index " + i0);

				labels.put("" + (nFeaturesCounter), label);
				allFeaturesList.add(centralFeatures);

				nFeaturesCounter++;
			}else {
				String label = elements[0];
				System.out.println("Deleting file "+label+" for insufficient energy");
				new File(workingFolder,label).delete();
			}
		}

		double[][] allFeatures = new double[nFeaturesCounter-1][];
		
		List<String> allPitch = Files.readAllLines(new File(workingFolder,"pitch.csv").toPath());
		int nPitchFeaturesToTake = (int)((float)windowSecToTake/AudioSegmentsAnalyser.pitchWindowSec);
		System.out.println("N Pitch segments to take "+nPitchFeaturesToTake);
		for (int g=0;g<(nFeaturesCounter-1);g++) {
			
			double[]energy =allFeaturesList.get(g);
			String file = labels.get(""+(g+1));
			String pline = "";
			for (String pitchF:allPitch) {
				String[] pf = pitchF.split(",");
				String pfn = pf[0];
				if (pfn.equals(file)) {
					pline = pitchF;
					break;
				}
			}
			
			if (pline.length()>0) {
			String [] pitchSig = pline.split(",");
			List<Double> pitchL = new ArrayList<>();
			for (int k=0;k<(pitchSig.length-2);k++) {
				String pitchString = pitchSig[k+2].trim();
				if (pitchString!=null && pitchString.length()>0)
					pitchL.add(Double.parseDouble(pitchString));
			}
			
			Double pitch [] =  new Double[pitchL.size()];
			pitch = pitchL.toArray(pitch);
					
			int i0 = ((pitch.length - nPitchFeaturesToTake) / 2);
			System.out.println("Pitch start index "+i0 +" vs length "+pitch.length+" ("+nPitchFeaturesToTake+")");
			
			double[] centralPitchFeatures = new double[nPitchFeaturesToTake];
			
			for (int i = 0; i < nPitchFeaturesToTake; i++) {
				double vector = pitch[i0 + i];
				centralPitchFeatures[i]=vector; 
			}
			
			double[] enerpitch = new double[nEnergyFeaturesToTake+nPitchFeaturesToTake];
			int co = 0;
			for (double d:energy) {
				enerpitch [co]= d;
				co++;
			}
			for (double d:centralPitchFeatures) {
				enerpitch [co]= d;
				co++;
			}
			
			allFeatures[g]=enerpitch; //centralPitchFeatures;//enerpitch;//centralPitchFeatures;//enerpitch;
		}else {
			System.out.println("DID not find pitch correspondence for file "+file);
		}
			
	}
		
		System.out.println("Submitting overall " + allFeatures.length + " features with length " + allFeatures[0].length);

		KMeans kmeans = new KMeans(workingFolder, labels);
		
		kmeans.compute(nclusters, 100, 10000, 2, allFeatures);

		int sum = 0;
		for (String key : kmeans.pointsPerCluster.keySet()) {
			int np = kmeans.pointsPerCluster.get(key);
			sum = sum + np;
		}
		float uniformityScore = 1f / (float) nclusters;
		System.out.println("Uniformity: " + uniformityScore);
		float difformity = 0;
		for (String key : kmeans.pointsPerCluster.keySet()) {
			int np = kmeans.pointsPerCluster.get(key);
			float score = ((float) np / (float) sum);
			System.out.println("C1%:" + score);
			// difformity =
			// difformity+(Math.abs(score-uniformityScore)/uniformityScore);
			difformity = difformity + (Math.abs(score - uniformityScore));
		}

		difformity = (float) difformity / (float) kmeans.pointsPerCluster.keySet().size();
		System.out.println("Difformity: " + difformity);
		
		File[] allFiles = workingFolder.listFiles();
		List<String> allclusters = Files.readAllLines(new File(workingFolder,"kmeansclustering.csv").toPath());
		
		for (File af:allFiles) {
			if (af.getName().endsWith(".wav")) {
				boolean found = false;
				for (String clustered:allclusters) {
					String[] pf = clustered.split(",");
					String pfn = pf[1];
					if (pfn.equals(af.getName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					//System.out.println("Deleting "+af.toString());
					af.delete();
				}
			}
		
		}
		
	}

	public static void main(String[] args) throws Exception {

		File folder = new File("./segments_v2");
		int nSpeakers = 2;
		SpeakersTagging st = new SpeakersTagging(folder, nSpeakers);
		// st.extractAllFeatures(folder);
		st.clusterFeatures();
	}

}
