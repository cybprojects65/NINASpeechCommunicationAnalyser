package it.cnr.speech.batchprocesses;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import it.cnr.speech.clustering.ClustersAnalyser;
import it.cnr.speech.utils.Utils;

public class PerformanceEvaluation {

	public static void main(String[] args) throws Exception {
		String prefix = "./W3_nC2/";
		File manualAnnotationsFile = new File("./ManualAnnotations/");
		File[] allCSVFiles = manualAnnotationsFile.listFiles();
		boolean kappaABS = true;
		int AFP = 0;
		int AFN = 0;
		int ATP = 0;
		int ATN = 0;
		
		double maxAccuracy = 0;
		double maxPrecision = 0;
		double maxRecall = 0;
		double maxF1 = 0;
		double minAccuracy = Integer.MAX_VALUE;
		double minPrecision = Integer.MAX_VALUE;
		double minRecall = Integer.MAX_VALUE;
		double minF1 = Integer.MAX_VALUE;
		HashMap<String,Double> totalrecalls =new HashMap<>();
		HashMap<String,Double> totalaccuracies =new HashMap<>();
		HashMap<String,Double> totalprecision =new HashMap<>();
		HashMap<String,Double> totalf1 =new HashMap<>();
		HashMap<String,Double> totalKappa =new HashMap<>();
		HashMap<String,String> totalKappaInterp =new HashMap<>();
		
		int maxCSV = 100;
		if (args!=null && args.length>0) {
			maxCSV=Integer.parseInt(args[0]);
			prefix = "";
		}
		int nCSV = 0;
		for (File annotatedFile : allCSVFiles) {
			if (annotatedFile.getName().endsWith(".csv")) {
			System.out.println("Evaluating " + annotatedFile.getName());
			HashSet<String> ineffectiveFiles = new HashSet<>();
			HashSet<String> effectiveFiles = new HashSet<>();
			List<String> allAnnotations = Files.readAllLines(annotatedFile.toPath(), Charset.forName("ISO-8859-1"));
			int annotationCounter = 0;
			int FP = 0;
			int FN = 0;
			int TP = 0;
			int TN = 0;

			for (String annotation : allAnnotations) {
				if (annotationCounter > 0) {
					String[] elements = annotation.split(",");
					if (elements != null && elements.length>0 && elements[0].endsWith(".wav")) {
						String audiosegment = elements[0];
						String isIneffective = elements[1];
						if (isIneffective.equalsIgnoreCase("N")) {
							effectiveFiles.add(audiosegment);
						} else
							ineffectiveFiles.add(audiosegment);
					}
				}
				annotationCounter++;
			}
			System.out.println("ALL segments:" + (effectiveFiles.size() + ineffectiveFiles.size()));
			System.out.println("Ineffective segments:" + ineffectiveFiles.size());
			System.out.println("Effective segments:" + effectiveFiles.size());

			File analysisFolder = new File(prefix+annotatedFile.getName().replace(".csv", "") + "Audio");
			System.out.println("Folder with analysis data:" + analysisFolder);
			
			if (!analysisFolder.exists())
				continue;
			
			String warningcluster = new String(Files.readAllBytes(new File(analysisFolder, ClustersAnalyser.WARNINGCLUSTERFILE).toPath())).trim();
			warningcluster = warningcluster.substring(warningcluster.indexOf(",") + 1);
			System.out.println("Warning cluster:" + warningcluster);
			File warningClusterFolder = new File(analysisFolder, "cluster" + warningcluster);
			File allwarningsegments[] = warningClusterFolder.listFiles();

			for (File warningSegment : allwarningsegments) {
				if (warningSegment.getName().endsWith(".wav")) {
					if (ineffectiveFiles.contains(warningSegment.getName())) {
						TP++;
						System.out.println("OK - " + warningSegment.getName());
					} else {
						FP++;
						System.out.println("False Positive - " + warningSegment.getName());
					}
				}
			}

			for (String ineff : ineffectiveFiles) {
				boolean found = false;
				for (File warningSegment : allwarningsegments) {
					if (warningSegment.getName().equals(ineff)) {
						found = true;
						break;
					}
				}
				if (!found) {
					if (new File(analysisFolder,ineff).exists()) {
						FN++;
						System.out.println("Missed False Negative - " + ineff);
					}
					else
						System.out.println("Previously Deleted file - " + ineff);
				}
			}

			TN = ineffectiveFiles.size() + effectiveFiles.size() - TP - FP - FN;

			double precision = (double) TP / ((double) (TP + FP));
			double recall = (double) TP / ((double) (TP + FN));
			double accuracy = (double) (TP + TN) / ((double) (TP + FP + TN + FN));
			double fmeasure = 2 * (precision * recall) / (precision + recall);
			double kappa = Utils.cohensKappaForDichotomy(TP, FP, FN, TN,kappaABS);
			String kappaFleiss = Utils.kappaClassificationFleiss(kappa);
			String kappaLandis = Utils.kappaClassificationLandisKoch(kappa);
			totalKappa.put(annotatedFile.getName(), kappa);
			totalKappaInterp.put(annotatedFile.getName(), kappaFleiss+"/"+kappaLandis);
			
			totalrecalls.put(annotatedFile.getName(),recall);
			totalprecision.put(annotatedFile.getName(),precision);
			totalaccuracies.put(annotatedFile.getName(),accuracy);
			totalf1.put(annotatedFile.getName(),fmeasure);
			
			System.out.println("TP:" + TP);
			System.out.println("TN:" + TN);
			System.out.println("FP:" + FP);
			System.out.println("FN:" + FN);
			System.out.println("Accuracy:" + accuracy);
			System.out.println("Precision:" + precision);
			System.out.println("Recall:" + recall);
			System.out.println("F1:" + fmeasure);
			ATP += TP;
			ATN += TN;
			AFP += FP;
			AFN += FN;
			
			
			
			if (minAccuracy>accuracy)
				minAccuracy=accuracy;
			if (maxAccuracy<accuracy)
				maxAccuracy=accuracy;
			if (minPrecision>precision)
				minPrecision=precision;
			if (maxRecall<precision)
				maxRecall=precision;
			if (minRecall>recall)
				minRecall=recall;
			if (maxRecall<recall)
				maxRecall=recall;
			if (minF1>fmeasure)
				minF1=fmeasure;
			if (maxF1<fmeasure)
				maxF1=fmeasure;
			
			// break;

			if (nCSV>maxCSV)
				break;
			nCSV++;

			}
		} // end for on all manual CSV files

		double precision = (double) ATP / ((double) (ATP + AFP));
		double recall = (double) ATP / ((double) (ATP + AFN));
		double accuracy = (double) (ATP + ATN) / ((double) (ATP + AFP + ATN + AFN));
		double fmeasure = 2 * (precision * recall) / (precision + recall);
		double kappa = Utils.cohensKappaForDichotomy(ATP, AFP, AFN, ATN,kappaABS);
		String kappaFleiss = Utils.kappaClassificationFleiss(kappa);
		String kappaLandis = Utils.kappaClassificationLandisKoch(kappa);
		
		System.out.println("Overall TP:" + ATP);
		System.out.println("Overall TN:" + ATN);
		System.out.println("Overall FP:" + AFP);
		System.out.println("Overall FN:" + AFN);
		
		System.out.println("Overall Accuracy:" + accuracy);
		System.out.println("Overall Precision:" + precision);
		System.out.println("Overall Recall:" + recall);
		System.out.println("Overall F1:" + fmeasure);
		System.out.println("Overall Kappa:" + kappa);
		System.out.println("Overall Kappa Interp:" + kappaFleiss+"/"+kappaLandis);
		
		System.out.println("Range Accuracy:" + minAccuracy+";"+maxAccuracy);
		System.out.println("Range Precision:" + minPrecision+";"+maxPrecision);
		System.out.println("Range Recall:" + minRecall+";"+maxRecall);
		System.out.println("Range F1:" + minF1+";"+maxF1);
		
		System.out.println("file,recall,accuracy,precision,f1");
		for (String file:totalrecalls.keySet()) {
			System.out.println(file.replace(".csv", "")+","+totalrecalls.get(file)
			+","+totalaccuracies.get(file)
			+","+totalprecision.get(file)
			+","+totalf1.get(file)
			+","+totalKappa.get(file)
			+","+totalKappaInterp.get(file)
			);
		}

	}// end main

}
