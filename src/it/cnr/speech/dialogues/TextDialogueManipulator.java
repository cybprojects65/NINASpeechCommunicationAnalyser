package it.cnr.speech.dialogues;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import it.cnr.speech.googlespeech2text.GoogleSpeechToText;
import it.cnr.speech.sphinx.segmentation.EnergySegmenter;
import it.cnr.speech.utils.Utils;

public class TextDialogueManipulator {

	public void removeEmptyFiles(File folder) throws Exception{
		removeEmptyFiles(folder,true);
	}
	
	public void removeEmptyFiles(File folder, boolean deleteWaveFile) throws Exception{
		File[] files = folder.listFiles();
		for (File f:files) {
			if (f.getName().endsWith(".wav")) {
				File txtFile = new File(f.getAbsolutePath().replace(".wav", ".txt"));
				if (!txtFile.exists())
					continue;
				
				List<String> allLines = Files.readAllLines(txtFile.toPath());
				if (allLines.size()==0 ||  (allLines.size()==1 && allLines.get(0).equals("{}"))) {
					System.out.println("Empty file: "+f.getName());
					txtFile.delete();
					if (deleteWaveFile)
						f.delete();
				}
			}
		}
	}
	
	public void reportTranscriptions(File folder) throws Exception{
		File[] files = folder.listFiles();
		for (File f:files) {
			if (f.getName().endsWith(".txt")) {
				List<String> allLines = Files.readAllLines(f.toPath());
				for (String line:allLines) {
					String search = "\"transcript\":";
					if (line.contains(search)) {
						line = line.substring(line.indexOf(search)+search.length());
						line = line.replace("\"", "");
						line = line.trim();
						line = line.replaceAll("\\p{Punct}", " ");
						line = line.replaceAll(" +", " ");
						File transcr = new File(f.getAbsolutePath().replace(".txt", "_transcription.txt"));
						FileWriter fw = new FileWriter(transcr);
						fw.write(line);
						fw.close();
					}
				}
			}
		}
	}
	
	public void createOneTranscription(File[] files, File outputFile) throws Exception{
		
		StringBuffer sb = new StringBuffer();
		files = Utils.getFiles(files, "_transcription.txt");
		
		
		int maxInt = 0;
		for (File f:files) {
			if (f.getName().endsWith("_transcription.txt")) {
				String tr = f.getName().substring(f.getName().indexOf("_")+1);
				String number = tr.substring(tr.indexOf("_")+1, tr.lastIndexOf("_"));
				Integer doubNumber = Integer.parseInt(number);
				if (doubNumber>maxInt)
					maxInt=doubNumber;
			}
		}
		
		String[] allTranscriptions = new String[maxInt+1];
		
		for (File f:files) {
			
				String tr = f.getName().substring(f.getName().indexOf("_")+1);
				String number = tr.substring(tr.indexOf("_")+1, tr.lastIndexOf("_"));
				Integer doubNumber = Integer.parseInt(number);
				String transcription = "";
				if (f.exists()) {
					List<String> allLines = Files.readAllLines(f.toPath());
					transcription = allLines.get(0);
				}
				allTranscriptions[doubNumber] = transcription;
				
			
		}
		
		for (String transcr:allTranscriptions)
			if(transcr!=null && transcr.length()>0)
				sb.append(transcr+". ");
		
		
		FileWriter fw = new FileWriter(outputFile);
		fw.write(sb.toString());
		fw.close();
	}
	
	public String questionMark(File f) throws Exception{
		List<String> allPitchLines = Files.readAllLines(new File(f.getParentFile(),"pitch.csv").toPath());
		String searchString = f.getName().replace("_transcription.txt", "");
		for (String line:allPitchLines) {
			String elements[]=line.split(",");
			String file = elements[0].replace(".wav", "");
			String isQuestion = elements[1];
			if (file.equals(searchString)) {
				if (isQuestion.equals("true"))
					return "?";
				else
					break;
			}
		}
		return "";
		
	}
	
	public void createOneTranscription(File folder) throws Exception {
		createOneTranscription(folder,false);
	}
	
	public static String DIALOGFILE = "dialog.txt";
	public void createOneTranscription(File folder, boolean addQuestionMark) throws Exception{
		File[] files = folder.listFiles();
		StringBuffer sb = new StringBuffer();
		int maxInt = 0;
		for (File f:files) {
			if (f.getName().endsWith("_transcription.txt")) {
				System.out.println("Adding "+f.getName()+" to transcription");
				String tr = f.getName().substring(f.getName().indexOf("_")+1);
				String number = tr.substring(tr.indexOf("_")+1, tr.lastIndexOf("_"));
					
				Integer doubNumber = Integer.parseInt(number);
				if (doubNumber>maxInt)
					maxInt=doubNumber;
			}
		}
		
		String[] allTranscriptions = new String[maxInt+1];
		for (File f:files) {
			if (f.getName().endsWith("_transcription.txt")) {
				String tr = f.getName().substring(f.getName().indexOf("_")+1);
				String number = tr.substring(tr.indexOf("_")+1, tr.lastIndexOf("_"));
				Integer doubNumber = Integer.parseInt(number);
				List<String> allLines = Files.readAllLines(f.toPath());
				String transcription = "";
				if (allLines!=null && allLines.size()>0) {
					transcription = allLines.get(0);
				if (addQuestionMark) {
					String questionMark = questionMark(f);
					if (questionMark.length()>0&&transcription.endsWith(",")) 
						transcription=transcription.substring(0,transcription.length()-1);
					
					transcription += questionMark(f);
				}
				}
				allTranscriptions[doubNumber] = transcription.toLowerCase();
				
			}
		}
		
		for (String transcr:allTranscriptions) {
			if(transcr!=null && transcr.trim().length()>0) {
				sb.append(transcr.trim()+". ");
			}
		}
		
		String finalTranscription = sb.toString().trim().replaceAll("\\.+", "\\.");
		finalTranscription = finalTranscription.replace("?.", "?") ;
		finalTranscription = finalTranscription.replaceAll(" +\\?", "\\?") ;
		
		File dialog = new File(folder,TextDialogueManipulator.DIALOGFILE);
		FileWriter fw = new FileWriter(dialog);
		fw.write(finalTranscription);
		fw.close();
	}
	
	
	public void createClusteredDialogues(File clusteringFile) throws Exception{
		
		LinkedHashMap<String,List<File>> clusteredTranscriptions = new LinkedHashMap<>();
		List<String> lines = Files.readAllLines(clusteringFile.toPath());
		int i=0;
		for (String line:lines) {
			if (i>0) {//skip header
			String[]el = line.split(",");
			String filePrefix = el[1];
			String cluster = el[2];
			File cfile = new File(clusteringFile.getParent(),filePrefix+"_transcription.txt");
			List<File> cFiles = clusteredTranscriptions.get(cluster);
			if (cFiles==null)
				cFiles = new ArrayList<>();
			cFiles.add(cfile);
			clusteredTranscriptions.put(cluster, cFiles);
			}
			i++;
		}
		
		File allFilesInFolder []= clusteringFile.getParentFile().listFiles();
		for (File allfile:allFilesInFolder) {
			if (allfile.getName().startsWith("dialog_cluster_"))
					allfile.delete();
		}
		for (String key : clusteredTranscriptions.keySet()) {
			List<File> allFiles = clusteredTranscriptions.get(key);
			File[] allFilesArr = new File[allFiles.size()];
			allFilesArr = allFiles.toArray(allFilesArr);
			File clusterdialog = new File(clusteringFile.getParent(),"dialog_cluster_"+key+".txt");
			createOneTranscription(allFilesArr, clusterdialog);
		}
	}
	
	
	public static void main(String [] args) throws Exception {
		//File folder = new File("./segments_v2");
		//File audiofile = new File("./samples/audioHiVol.wav");
		File audiofile = new File("./samples/audio.wav");
		File folder = new File("./audioCut_copy2/cluster0");
		TextDialogueManipulator da = new TextDialogueManipulator();
		//da.removeEmptyFiles(folder);
		//da.reportTranscriptions(folder);
		//da.createOneTranscription(folder);
		//da.createTestDialogue(audiofile);
		//da.createClusteredDialogues(new File("./segments_v2/kmeansclustering.csv"));
		da.createOneTranscription(folder, true);
	}
	
}
