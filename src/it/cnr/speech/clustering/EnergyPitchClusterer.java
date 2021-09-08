package it.cnr.speech.clustering;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.io.FileUtils;

import it.cnr.speech.audiofeatures.EnergyPitchExtractor;
import it.cnr.speech.segmentation.AudioSegmentsAnalyser;

public class EnergyPitchClusterer {

	int windowSecToTake;
	int nClusters;
	public static String CLUSTERINGFILE = "clustering.csv";
	public String clusteringAlgorithm = "KMEANS"; //"XMEANS";//"KMEANS"; //XMEANS
	
	public EnergyPitchClusterer(int windowSecToTake, int nClusters) {
		this.windowSecToTake = windowSecToTake;
		this.nClusters = nClusters;
	}
	
	public void clusterEnergyPitch(File workingFolder) throws Exception {

		int nEnergyFeaturesToTake = (int)((float)windowSecToTake/AudioSegmentsAnalyser.energyWindowSec);
		System.out.println("Smallest number of energy features to take " + nEnergyFeaturesToTake);
		//double[][] allFeatures = new double[nFiles][];
		
		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		int nFeaturesCounter = 0;
		List<String> allEnergies = Files.readAllLines(new File(workingFolder,EnergyPitchExtractor.ENERGYFILE).toPath());
		List<double[]>allFeaturesList = new ArrayList<>();
		
		for (String energyLine : allEnergies) {
			if (nFeaturesCounter == 0) {
				nFeaturesCounter++;
				continue;
			}

			String[] elements = energyLine.split(",");
			if (elements.length > nEnergyFeaturesToTake+1) {
				String label = elements[0];
				double[] features = new double[elements.length - 1];
				for (int i = 1; i < elements.length; i++) {
					features[i - 1] = Double.parseDouble(elements[i]);
				}

				//System.out.println("Extracted Energy Features #: " + features.length);

				int i0 = ((features.length - nEnergyFeaturesToTake) / 2);
				double[] centralFeatures = new double[nEnergyFeaturesToTake];
				
				for (int i = 0; i < nEnergyFeaturesToTake; i++) {
					double mfccvector = features[i0 + i];
					centralFeatures[i]=mfccvector; 
				}
				
				//System.out.println("Total length of features " + centralFeatures.length);
				//System.out.println("Start ENERGY Index " + i0);

				labels.put("" + (nFeaturesCounter), label);
				allFeaturesList.add(centralFeatures);

				nFeaturesCounter++;
			}else {
				String label = elements[0];
				System.out.println("Deleting file "+label+" (insufficient number of energy samples < "+nEnergyFeaturesToTake+" )");
				File todel=	new File(workingFolder,label);
				todel.delete();
				todel.deleteOnExit();
				todel.setLastModified(0);
			}
		}

		double[][] allFeatures = new double[nFeaturesCounter-1][];
		
		List<String> allPitch = Files.readAllLines(new File(workingFolder,EnergyPitchExtractor.PITCHFILE).toPath());
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
			//System.out.println("Pitch start index "+i0 +" vs length "+pitch.length+" ("+nPitchFeaturesToTake+")");
			
			double[] centralPitchFeatures = new double[nPitchFeaturesToTake];
			
			if (pitch.length<nPitchFeaturesToTake) {
				System.out.println("Deleting file "+file+" (insufficient number of pitch samples < "+nPitchFeaturesToTake+" )");
				File todel=	new File(workingFolder,file);
				todel.delete();
				todel.deleteOnExit();
				todel.setLastModified(0);
				continue;
			}
			
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
		System.out.println("Estimated number of features " + allFeatures.length);
		int nRealFeatures = 0;
		Queue<double[]> nonNullFeatures = new LinkedList<double[]>();
		int allFeaturesCounter = 0;
		LinkedHashMap<String, String> newlabels = new LinkedHashMap<String, String>();
		
		for (double[] v: allFeatures) {
			
			if (v!=null) {
				nonNullFeatures.add(v);
				String label = labels.get(""+(allFeaturesCounter+1));
				newlabels.put(""+(nRealFeatures+1), label);
				nRealFeatures++;
			}
			
			allFeaturesCounter++;
		}
		
		labels = newlabels;
		allFeatures = new double[nRealFeatures][];
		allFeatures=nonNullFeatures.toArray(allFeatures);
		
		System.out.println("Submitting overall " + allFeatures.length + " features with length " + allFeatures[0].length);
		if (clusteringAlgorithm.equals("KMEANS")) {
		KMeans kmeans = new KMeans(workingFolder, labels);
		
		kmeans.compute(nClusters, 100, 10000, 2, allFeatures);

		int sum = 0;
		for (String key : kmeans.pointsPerCluster.keySet()) {
			int np = kmeans.pointsPerCluster.get(key);
			sum = sum + np;
		}
		float uniformityScore = 1f / (float) nClusters;
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
		}else if (clusteringAlgorithm.equals("XMEANS")) {
			XMeans xmeans = new XMeans(workingFolder,labels); 
			xmeans.compute(2,nClusters,10,10000, allFeatures);
		}
		
		File[] allFiles = workingFolder.listFiles();
		List<String> allclusters = Files.readAllLines(new File(workingFolder,CLUSTERINGFILE).toPath());
		
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
					af.setLastModified(0);
				}
			}
		
		}
		
	}
	
}
