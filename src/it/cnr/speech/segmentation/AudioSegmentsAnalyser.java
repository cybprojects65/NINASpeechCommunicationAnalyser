package it.cnr.speech.segmentation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import it.cnr.speech.sphinx.segmentation.EnergySegmenter;
import it.cnr.speech.utils.OSCommands;

public class AudioSegmentsAnalyser {

	public static void main(String[] args) throws Exception {
		AudioSegmentsAnalyser asa = new AudioSegmentsAnalyser();
		File folder = new File("./segments_v2");
		File signal = new File("./samples/audio.wav");
		// asa.analyseClusters(folder);
		// String[] labels = {"Tono concitato o interrogativo","Comunicazione
		// chiara", "Tono tranquillo e collaborativo"};
		String statistics = asa.statisticalAnalysisofAudioClusters(folder);
		int warnCluster = asa.detectWarningCluster(statistics); // automatically
																// detects the
																// cluster to
																// set as
																// warning
																// dialogue
		String[] labels = new String[3];
		labels[warnCluster] = "Warning";

		asa.tagSignalWithClusters(folder, signal, labels);

	}

	// statistical analysis on time series features:
	// - mean energy in the cluster
	// - max energy in the cluster
	// - mean pitch in the cluster -> mean agitation
	// - mean signal length in the cluster -> mean overlap
	// - mean number of questions

	public void analyseClusters(File folder) throws Exception {

		File[] allFiles = folder.listFiles();
		for (File f : allFiles) {

			if (f.isDirectory() && f.getName().startsWith("cluster")) {

				int clusterNumber = Integer.parseInt(f.getName().substring(7));
				System.out.println("Analysing Cluster " + clusterNumber);
				File[] allClusterSegments = f.listFiles();
				File energyFile = new File(f, "energy.csv");
				if (energyFile.exists())
					energyFile.delete();

				File pitchFile = new File(f, "pitch.csv");
				if (pitchFile.exists())
					pitchFile.delete();

				BufferedWriter fw = new BufferedWriter(new FileWriter(energyFile, true));
				fw.write("filename,e0\n");
				BufferedWriter fwP = new BufferedWriter(new FileWriter(pitchFile, true));
				fwP.write("filename,is_question,f0\n");
				for (File fs : allClusterSegments) {
					if (fs.getName().endsWith(".wav")) {

						EnergySegmenter nrg = new EnergySegmenter();
						System.out.println("Extracting energy for " + fs.getName());
						double[] normalisedEnergyCurve = nrg.energyCurve(0.10f, fs, false, false);
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
						syllabPitchExtr.setPitchWindowSec(0.2);
						boolean isq = syllabPitchExtr.isQuestionRough(fs.getAbsolutePath());
						System.out.println("File " + fs.getName() + " is question? " + isq);
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
	
	
	public static double pitchWindowSec = 0.2;
	public static float energyWindowSec = 0.1f;
	
	public void extractEnergyPitchOfWaveFiles(File folder) throws Exception {
		File[] allClusterSegments = folder.listFiles();
		File energyFile = new File(folder, "energy.csv");
		if (energyFile.exists())
			energyFile.delete();

		File pitchFile = new File(folder, "pitch.csv");
		if (pitchFile.exists())
			pitchFile.delete();

		BufferedWriter fw = new BufferedWriter(new FileWriter(energyFile, true));
		fw.write("filename,e0\n");
		BufferedWriter fwP = new BufferedWriter(new FileWriter(pitchFile, true));
		fwP.write("filename,is_question,f0\n");
		for (File fs : allClusterSegments) {
			if (fs.getName().endsWith(".wav")) {

				EnergySegmenter nrg = new EnergySegmenter();
				System.out.println("Extracting energy for " + fs.getName());
				double[] normalisedEnergyCurve = null;
				
				try {
					normalisedEnergyCurve = nrg.energyCurve(energyWindowSec, fs, false, true);
				}catch(Exception e) {
					System.out.println("Cannot extract energy from "+fs.getName());
					//e.printStackTrace();
					System.out.println("Deleting file "+fs.getName()+" for insufficient length");
					fs.delete();
					continue;
				}
				
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
				System.out.println("File " + fs.getName() + " is question? " + isq);
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

	public String statisticalAnalysisofAudioClusters(File folder) throws Exception {
		String command = "cmd /c Rscript --vanilla ./RStatistics/energypitchstatistics.R " + folder.getAbsolutePath().replace("\\", "/") + "/";
		String[] allcommands = { command };
		String log = OSCommands.executeOSCommands(allcommands);
		String statistics = log.substring(log.indexOf(">") + 1, log.indexOf("<"));
		System.out.println("Statistics:\n" + statistics);
		return statistics;
	}

	public int detectWarningCluster(String statistics,boolean useQuestions) {

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
	
	public int detectWarningCluster(String statistics) {

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
				double score = meanEnergy * meanPitch * nquestions;
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

	// tag the original signal with the clusters
	public void tagSignalWithClusters2(File folder, File signal, String[] clustersLabels) throws Exception {

		File segmentation = new File(folder, "energy_segmentation.csv");
		File clusterFile = new File(folder, "kmeansclustering.csv");

		BufferedWriter fw = new BufferedWriter(new FileWriter(new File(signal.getAbsolutePath().replace(".wav", ".lab"))));
		BufferedReader brSeg = new BufferedReader(new FileReader(segmentation));

		String line = brSeg.readLine();
		while (line != null) {
			String lineElements[] = line.split(" ");
			String waveFile = lineElements[2];
			String startTime = lineElements[0];
			String endTime = lineElements[1];
			BufferedReader brCluster = new BufferedReader(new FileReader(clusterFile));
			String lineCluster = brCluster.readLine();// skip headers
			lineCluster = brCluster.readLine();
			int foundCluster = -1;
			while (lineCluster != null) {
				String clusterLine[] = lineCluster.split(",");
				if (waveFile.equals(clusterLine[1])) {
					foundCluster = Integer.parseInt(clusterLine[2]);
					break;
				}
				lineCluster = brCluster.readLine();
			}
			
			if(foundCluster>-1)
					System.out.println(waveFile+"->"+foundCluster+" "+startTime+"-"+endTime+" ["+foundCluster+"]" + " "+clustersLabels[foundCluster]);
			
			if (foundCluster == -1) {
				fw.write(startTime + " " + endTime + " " + " " + "\n");
				
			}else {
				
				if (clustersLabels[foundCluster] != null)
					fw.write(startTime + " " + endTime + " " + clustersLabels[foundCluster] + "\n");
				else
					fw.write(startTime + " " + endTime + " " + " " + "\n");
			}
			brCluster.close();
			line = brSeg.readLine();
		}

		brSeg.close();
		fw.close();
	}

	
	// tag the original signal with the clusters
	public void tagSignalWithClusters(File folder, File signal, String[] clustersLabels) throws Exception {

		File segmentation = new File(folder, "energy_segmentation.csv");
		File clusterFile = new File(folder, "kmeansclustering.csv");

		BufferedWriter fw = new BufferedWriter(new FileWriter(new File(signal.getAbsolutePath().replace(".wav", ".lab"))));
		BufferedReader brSeg = new BufferedReader(new FileReader(segmentation));

		String line = brSeg.readLine();
		while (line != null) {
			String lineElements[] = line.split(" ");
			String waveFile = lineElements[2];
			String startTime = lineElements[0];
			String endTime = lineElements[1];
			BufferedReader brCluster = new BufferedReader(new FileReader(clusterFile));
			String lineCluster = brCluster.readLine();// skip headers
			lineCluster = brCluster.readLine();
			int foundCluster = -1;
			while (lineCluster != null) {
				String clusterLine[] = lineCluster.split(",");
				if (waveFile.equals(clusterLine[1] + ".wav")) {
					foundCluster = Integer.parseInt(clusterLine[2]);
					break;
				}
				lineCluster = brCluster.readLine();
			}
			//System.out.println(waveFile+"->"+foundCluster);
			if (foundCluster == -1)
				fw.write(startTime + " " + endTime + " " + " " + "\n");
			else {
				
				if (clustersLabels[foundCluster] != null)
					fw.write(startTime + " " + endTime + " " + clustersLabels[foundCluster] + "\n");
				else
					fw.write(startTime + " " + endTime + " " + " " + "\n");
			}
			brCluster.close();
			line = brSeg.readLine();
		}

		brSeg.close();
		fw.close();
	}

}
