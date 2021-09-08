package it.cnr.speech.clustering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;

import it.cnr.speech.audiofeatures.EnergyPitchExtractor;
import it.cnr.speech.segmentation.PitchExtractor;
import it.cnr.speech.sphinx.segmentation.EnergySegmenter;
import it.cnr.speech.utils.OSCommands;

public class ClustersAnalyser {

	public ClustersAnalyser() {

	}
	
	public static String CLUSTERPREFIX = "cluster";
	public static String WARNINGCLUSTERFILE = "warningcluster.csv";
	
	public void organizeWavesInClusters(File folder) throws Exception {
		System.out.println("Organizing files in folder " + folder.getName() + " into clusters");
		List<String> allClustered = Files.readAllLines(new File(folder, EnergyPitchClusterer.CLUSTERINGFILE).toPath());
		int i = 0;
		for (String ac : allClustered) {
			if (i > 0) {
				String[] line = ac.split(",");
				String clusterNumber = line[2];
				File audiofile = new File(folder, line[1]);

				File clusterFolder = new File(folder, CLUSTERPREFIX + clusterNumber);
				if (!clusterFolder.exists())
					clusterFolder.mkdir();

				if (audiofile.exists() && audiofile.lastModified()>0)
					Files.copy(audiofile.toPath(), new File(clusterFolder, audiofile.getName()).toPath());
			}
			i++;
		}
	}

	public void extractEnergyPitchForClusters(File folder, float energyWindowSec, float pitchWindowSec) throws Exception {

		File[] allFiles = folder.listFiles();
		for (File f : allFiles) {

			if (f.isDirectory() && f.getName().startsWith(CLUSTERPREFIX)) {

				int clusterNumber = Integer.parseInt(f.getName().substring(7));
				System.out.println("Analysing Cluster " + clusterNumber);
				File[] allClusterSegments = f.listFiles();
				File energyFile = new File(f, EnergyPitchExtractor.ENERGYFILE);
				if (energyFile.exists())
					energyFile.delete();

				File pitchFile = new File(f, EnergyPitchExtractor.PITCHFILE);
				if (pitchFile.exists())
					pitchFile.delete();

				BufferedWriter fw = new BufferedWriter(new FileWriter(energyFile, true));
				fw.write("filename,e0\n");
				BufferedWriter fwP = new BufferedWriter(new FileWriter(pitchFile, true));
				fwP.write("filename,is_question,f0\n");
				for (File fs : allClusterSegments) {
					if (fs.getName().endsWith(".wav")) {

						EnergySegmenter nrg = new EnergySegmenter();
						//System.out.println("Extracting energy for " + fs.getName());
						double[] normalisedEnergyCurve = nrg.energyCurve(energyWindowSec, fs, false, false);
						fw.write(fs.getName() + ",");
						// System.out.println("N samples
						// "+normalisedEnergyCurve.length);
						for (int j = 0; j < normalisedEnergyCurve.length; j++) {
							double e = normalisedEnergyCurve[j];
							fw.write("" + e);
							if (j < normalisedEnergyCurve.length - 1)
								fw.write(",");
							else
								fw.write("\n");
						}

						PitchExtractor pitchExtr = new PitchExtractor();
						pitchExtr.calculatePitch(fs.getAbsolutePath());

						PitchExtractor syllabPitchExtr = new PitchExtractor();
						syllabPitchExtr.setPitchWindowSec(pitchWindowSec);
						boolean isq = syllabPitchExtr.isQuestionRough(fs.getAbsolutePath());
						//System.out.println("File " + fs.getName() + " is question? " + isq);
						fwP.write(fs.getName() + "," + isq + ",");

						Double[] pitch = pitchExtr.pitchCurve;
						for (int j = 0; j < pitch.length; j++) {
							if (pitch[j] == null || Double.isNaN(pitch[j]) || Double.isInfinite(pitch[j]))
								fwP.write(" ");
							else {
								double e = pitch[j];
								fwP.write("" + e);
							}
							if (j < pitch.length - 1)
								fwP.write(",");
							else
								fwP.write("\n");
						}
					}
				}

				fw.close();
				fwP.close();
			}

		}

	}
	
	
	public String statisticalAnalysisofAudioClusters(File folder) throws Exception {
		String command = "cmd /c Rscript --vanilla ./RStatistics/energypitchstatistics.R " + folder.getAbsolutePath().replace("\\", "/") + "/";
		String[] allcommands = { command };
		String log = OSCommands.executeOSCommands(allcommands);
		String statistics = log.substring(log.indexOf(">") + 1, log.indexOf("<"));
		System.out.println("Statistics:\n" + statistics);
		return statistics;
	}
	
	
	
	public int detectWarningClusterFromStatistics(String statistics, boolean useQuestions) {

		String[] lines = statistics.split("\n");
		int i = 0;
		double maxscore = 0;
		int warningcluster = -1;
		while (i < lines.length) {
			String line = lines[i];
			if (line.startsWith("Statistics for cluster ")) {
				int nCluster = Integer.parseInt(line.substring(23).trim());
				line = lines[i + 1];
				double meanEnergy = Double.parseDouble(line.substring(line.indexOf(")") + 1, line.indexOf("[")).trim());
				line = lines[i + 2];
				double meanPitch = Double.parseDouble(line.substring(line.indexOf(")") + 1, line.indexOf("db")).trim());
				line = lines[i + 3];
				int nquestions = (int) Double.parseDouble(line.substring(line.indexOf("[") + 1, line.indexOf("%")).trim());
				double score = meanEnergy * meanPitch;
				if (useQuestions)
					score = score * nquestions;
				
				System.out.println("Score of cluster " + nCluster + ": " + score);
				if (score > maxscore) {
					maxscore = score;
					warningcluster = nCluster;
				}

				i = i + 5;
			} else
				i++;
		}
		System.out.println("Warning cluster is " + warningcluster);
		return warningcluster;
	}
	
	public int detectWarningCluster(File folder, boolean useQuestions, float energyWindowSec, float pitchWindowSec) throws Exception{
		organizeWavesInClusters(folder);
		extractEnergyPitchForClusters(folder,energyWindowSec,pitchWindowSec);
		String statistics = statisticalAnalysisofAudioClusters(folder);
		int warningCluster = detectWarningClusterFromStatistics(statistics,useQuestions);
		FileWriter fw = new FileWriter(new File(folder,WARNINGCLUSTERFILE));
		fw.write("warning cluster,"+warningCluster+"\n");
		fw.close();
		return warningCluster;
	}
}
