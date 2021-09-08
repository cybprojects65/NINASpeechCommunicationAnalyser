package it.cnr.speech.segmentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import it.cnr.speech.audiofeatures.AudioProcessing;
import it.cnr.speech.textprocessing.WordSegment;

public class AudioAnnotationSegmenter {

	public static void main(String[] args) throws Exception {

		File textFolder = new File("./segments_v2/");
		AudioAnnotationSegmenter aas = new AudioAnnotationSegmenter();
		// aas.extractMostImportantSegments(textFolder);
		aas.extractAudioWordsForClusters(textFolder);
	}

	public void extractAudioWordsForClusters(File folder) throws Exception {
		HashMap<String, List<WordSegment>> wordsSegmentsPerCluster = extractMostImportantSegments(folder);

		for (String key : wordsSegmentsPerCluster.keySet()) {
			System.out.println("Cluster " + key + " - saving slices");
			File clusterFolder = new File(folder, "cluster" + key);
			if (!clusterFolder.exists())
				clusterFolder.mkdir();
			else {
				File[] fs = clusterFolder.listFiles();
				for (File f : fs) {
					f.delete();
				}
			}

			List<WordSegment> wordsSegments = wordsSegmentsPerCluster.get(key);
			int k = 0;
			for (WordSegment ws : wordsSegments) {
				int startTime = (int) Math.floor(ws.startTimeSec);
				int duration = (int) Math.ceil(ws.endTimeSec - startTime);
				if (duration == 0)
					duration = 1;
				File audioFile = new File(folder, ws.containerFile.getName() + ".wav");
				File outputFile = new File(clusterFolder, audioFile.getName().replace(".wav", "_word" + k + ".wav"));
				System.out.println("Saving word " + ws.word + " from " + audioFile.getName() + " to " + outputFile.getName() + " t0: " + startTime + "s - duration: " + duration + "s");
				AudioProcessing.copyAudio(audioFile.getAbsolutePath(), outputFile.getAbsolutePath(), startTime, duration);
				k++;
			}
		}
	}

	public void organizeWavesInClusters(File folder) throws Exception {
		List<String> allClustered = Files.readAllLines(new File(folder, "kmeansclustering.csv").toPath());
		int i = 0;
		for (String ac : allClustered) {
			if (i > 0) {
				String[] line = ac.split(",");
				String clusterNumber = line[2];
				File audiofile = new File(folder, line[1]);

				File clusterFolder = new File(folder, "cluster" + clusterNumber);
				if (!clusterFolder.exists())
					clusterFolder.mkdir();

				if (audiofile.exists())
					Files.copy(audiofile.toPath(), new File(clusterFolder, audiofile.getName()).toPath());
			}
			i++;
		}

	}

	/**
	 * Reads the kmeansclustering file and records all files per cluster for
	 * each cluster, reads the tag cloud for each file in the cluster searches
	 * in the detailed transcription for the words in the tag cloud records the
	 * segments to extract
	 * 
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public HashMap<String, List<WordSegment>> extractMostImportantSegments(File folder) throws Exception {
		File allFiles[] = folder.listFiles();
		ArrayList<String> words = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		int maxScore = 0;
		System.out.println("Reading cluster files");
		HashMap<String, List<String>> allClusters = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(new File(folder, "kmeansclustering.csv")));
		String line = br.readLine();// skip header
		line = br.readLine();
		while (line != null) {
			String elements[] = line.split(",");
			String cluster = elements[2];
			String file = elements[1];
			List<String> clustList = allClusters.get(cluster);
			if (clustList == null)
				clustList = new ArrayList<String>();
			clustList.add(file);
			allClusters.put(cluster, clustList);
			line = br.readLine();
		}
		br.close();
		System.out.println("Reading finished");
		HashMap<String, List<WordSegment>> allClustersWordsSegments = new HashMap<>();

		for (File f : allFiles) {
			if (f.getName().startsWith("cloudtag") && f.getName().endsWith(".html")) {
				String cluster = f.getName().substring(8, f.getName().lastIndexOf("."));
				System.out.println("Cluster " + cluster);
				System.out.println("Detagging " + f.getName());
				List<String> allStrings = Files.readAllLines(f.toPath());
				for (String st : allStrings) {
					if (st.startsWith("<a href=")) {
						String search = "font-size:";
						st = st.substring(st.indexOf(search) + search.length() + 1);
						String score = st.substring(0, st.indexOf(";"));
						Integer scoreInt = Integer.parseInt(score.replace("px", ""));
						if (scoreInt > maxScore)
							maxScore = scoreInt;

						String word = StringEscapeUtils.unescapeHtml4(st.substring(st.indexOf(">") + 1, st.indexOf("<")));
						// System.out.println("word " + word);
						// System.out.println("score " + score);
						words.add(word.toLowerCase());
						scores.add(scoreInt);
					}
				}

				/*
				 * DEPRECATED: GET THE IMPORTANCE DISTRIBUTION - I VERIFIED THAT
				 * ALL WORDS ARE IMPORTANT int maxWords = words.size();
				 * Iterator<String> itw = words.iterator(); Iterator<Integer>
				 * its = scores.iterator();
				 * 
				 * for (int i=0;i<maxWords;i++) { String word = itw.next();
				 * Integer score = its.next(); float scoref =
				 * (float)score*100/(float)maxScore;
				 * System.out.println(word+":"+scoref); }
				 */
				// GET CLUSTER'S TRANSCRIPTIONS
				List<String> allFilesInTheCluster = allClusters.get(cluster);
				int totalWordsSegments = 0;
				int reportedWordSegments = 0;
				for (String fileincluster : allFilesInTheCluster) {
					System.out.println("Searching words in " + fileincluster);
					File transcription = new File(folder, fileincluster + ".txt");
					if (!transcription.exists())
						continue;
					
					List<String> allTranscribedWords = Files.readAllLines(transcription.toPath());
					boolean inWord = false;
					double startTime = -1d;
					double endTime = -1d;
					String word = "";

					for (String transcribed : allTranscribedWords) {

						if (transcribed.contains("\"startTime\":")) {
							inWord = true;
							startTime = Double.parseDouble(transcribed.substring(transcribed.indexOf(":") + 1, transcribed.indexOf(",")).replace("\"", "").replace("s", ""));
						} else if (inWord && transcribed.contains("\"endTime\":")) {
							endTime = Double.parseDouble(transcribed.substring(transcribed.indexOf(":") + 1, transcribed.indexOf(",")).replace("\"", "").replace("s", ""));
						} else if (inWord && transcribed.contains("\"word\":")) {

							word = (transcribed.substring(transcribed.indexOf(":") + 1).replace("\"", "")).toLowerCase().trim();

							inWord = false;
							if (words.contains(word)) {
								reportedWordSegments++;
								WordSegment ws = new WordSegment(startTime, endTime, word, new File(folder, fileincluster));
								System.out.println("Found important words as " + ws.toString());
								List<WordSegment> lw = allClustersWordsSegments.get(cluster);
								if (lw == null)
									lw = new ArrayList<>();
								lw.add(ws);
								allClustersWordsSegments.put(cluster, lw);
								startTime = -1d;
								endTime = -1d;
								word = "";
							}
							totalWordsSegments++;
						}

					}
				} // end search in the cluster
				System.out.println("Cluster " + cluster + " percentage of segmented words " + ((reportedWordSegments * 100f) / totalWordsSegments));
			}
		}

		return allClustersWordsSegments;

	}

}
