package it.cnr.speech.batchprocesses;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import it.cnr.speech.utils.OSCommands;
import it.cnr.speech.utils.Utils;

public class OrganizeSegmentsForManualAnnotation {

	public static void main(String args[]) throws Exception{
		
		String[] allfolders = {"PS12Audio" , 
				"PS13Audio" , 
				"PS14Audio" , 
				"PS15Audio" , 
				"PS16Audio" , 
				"PS1Audio" , 
				"PS3Audio" , 
				"PS4Audio" , 
				"PS5Audio" , 
				"PS7Audio" , 
				"PS9Audio"};
		
		for (String folder: allfolders) {
			File folderF = new File(folder);
			File outputFile = new File(folderF,"manual_annotation.csv");
			if (outputFile.exists())
				outputFile.delete();
			
			File[] allSubFiles = folderF.listFiles();
			Utils.sortByNumber(allSubFiles);
			String header = "File Name,Notable(Y/N),Comment\n";
			FileWriter fw = new FileWriter(outputFile,true);
			fw.write(header);
			StringBuffer sb = new StringBuffer();
			for (File sf:allSubFiles) {
				if (sf.getName().endsWith(".wav")) {
					String element =sf.getName()+",,\n";
					fw.write(element);
					sb.append(sf.getAbsolutePath()+" ");
				}
			}
			fw.close();
			
			String command ="zip -j "+folder+".zip"+" ./"+folder+"/*.wav "+"./"+folder+"/"+outputFile.getName();
			String[] commands= {command};
			OSCommands.executeOSCommands(commands);
			//break;
		}
		
		
	}
	
	
}
