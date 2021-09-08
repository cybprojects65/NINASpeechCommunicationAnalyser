package it.cnr.speech.textprocessing;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.UUID;

import it.cnr.speech.utils.OSCommands;

public class POSTagger {

	static String regexClean = "[^a-zA-Z0-9ÀÁÄÈÉËÌÍÏÒÓÖÚÙÜáàäéèëíìïóòöùüýÿ]";
	static String TreeTaggerHome = "./TreeTaggerWindows/";
	
	public String cleanString(String inputString) {
		
		return inputString.replaceAll(regexClean, " ");
		
	}
	
	public File posTag(File inputFile) throws Exception{
		
		String inputText = new String(Files.readAllBytes(inputFile.toPath()));
		inputText = cleanString(inputText);
		File tempFileToTag = new File("tag_"+UUID.randomUUID());
		FileWriter fw = new FileWriter(tempFileToTag);
		fw.write(inputText);
		fw.close();
		File outputFile = new File(inputFile.getAbsolutePath().replace(".txt", "_POSTag.txt"));
		String commandIt= "perl "+TreeTaggerHome+"/cmd/tokenize.pl \"./"+tempFileToTag+"\" | \""+
				TreeTaggerHome+"/bin/tree-tagger.exe\" \""+TreeTaggerHome+"/lib/italian.par\" -token -lemma -no-unknown  > "+outputFile.getAbsolutePath();
		
		System.out.println("command "+commandIt);
		String[] commandsGT = new String[2];
		commandsGT[0] = "cmd /c";
		commandsGT[1] = commandIt;
		
		OSCommands.executeOSCommands(commandsGT);
		
		//tempFileToTag.delete();
		
		return outputFile;
	}
	
	public static void main(String[] args) throws Exception{
		
		POSTagger tag = new POSTagger();
		File inputFile = new File("D:\\EclipseWorkspaces\\workspacePrivate\\NINASpeechCommunicationAnalyser\\segments_v2\\audio_segment_1_transcription.txt");
		File output = tag.posTag(inputFile);
		System.out.println("Output wirtten in "+output.getName());
		
	}
}
