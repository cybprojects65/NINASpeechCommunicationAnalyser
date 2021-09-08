package it.cnr.speech.batchprocesses;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import it.cnr.speech.clustering.ClustersAnalyser;

public class CommentsEvaluation {

	
	public static void main(String[] args) throws Exception {
		String prefix = ""; //"./W3_nC2/";
		File cloudsFolder = new File("D:\\WorkFolder\\Experiments\\NINA\\AUDIO_SIMULAZIONI_WAV\\");
		File manualAnnotationsFile = new File("./ManualAnnotations/");
		File[] allCSVFiles = manualAnnotationsFile.listFiles();
		
		int nCSV = 0;
		StringBuffer sb = new StringBuffer();
		
		for (File annotatedFile : allCSVFiles) {
			if (annotatedFile.getName().endsWith(".csv")) {
			System.out.println("Evaluating " + annotatedFile.getName());
			HashSet<String> ineffectiveFiles = new HashSet<>();
			HashSet<String> effectiveFiles = new HashSet<>();
			List<String> allAnnotations = Files.readAllLines(annotatedFile.toPath(), Charset.forName("ISO-8859-1"));
			int annotationCounter = 0;
			
			for (String annotation : allAnnotations) {
				if (annotationCounter > 0) {
					String[] elements = annotation.split(",");
					if (elements != null && elements.length>0 && elements[0].endsWith(".wav")) {
						String audiosegment = elements[0];
						String isIneffective = elements[1];
						String comment = "";
						if (elements.length>2)
							comment = elements[2];
						
						comment = comment.replaceAll("[\\]\\[!\"#$%&'()\\*\\+,\\.:;<=>\\\\\\?@\\^_`\\{\\|\\}~-]", " ");
						comment = comment.replaceAll("[°/]", " ");
						comment = comment.replaceAll("\\s+", " ");
						comment = comment.trim().toLowerCase();
						
						if (isIneffective.equalsIgnoreCase("N")) {
							effectiveFiles.add(audiosegment);
						} else {
							if (comment.length()>0)
								sb.append(comment+"\n");
							ineffectiveFiles.add(audiosegment);
						}
					}
				}
				annotationCounter++;
				}
			}

		}
		
		System.out.println("All comments:\n"+sb.toString());
			

		File allOrigFiles [] = cloudsFolder.listFiles();
		HashMap<String,Integer> hm = new HashMap<>();
		
		for (File origFile:allOrigFiles) {
			if (origFile.getName().endsWith(".html")) {
				List<String> allLines = Files.readAllLines(origFile.toPath());
				for (String line:allLines) {
					int i0 = line.indexOf("font-size: ");
					if (i0>-1) {
						line = line.substring(i0+"font-size: ".length());
						String scoreS = line.substring(0,line.indexOf("px"));
						int score = Integer.parseInt(scoreS);
						String word = line.substring(line.indexOf(">")+1,line.indexOf("<"));
						if (hm.get(word)==null)
							hm.put(word, score);
						else
							hm.put(word, score+hm.get(word));
					}
				}
			}
		}
		
		System.out.println(hm);
		String[] commentsLines = sb.toString().split("\n");
		HashMap<String,Integer> commonWords = new HashMap<>();
		
		for (String word:hm.keySet()) {
			for (String comment:commentsLines) {
				if (comment.contains(word)) {
					Integer score = commonWords.get(word);
					if (score == null)
						commonWords.put(word,1);
					else
						commonWords.put(word,score+1);
					break;
				}
			}
		}
		
		System.out.println("Common words:\n"+commonWords.toString().replace(",", "\n").replace("=", "\t").replace("{", "").replace("}", ""));
		
		double wordsCoverage = (double)commonWords.size()/(double)hm.keySet().size();
		
		System.out.println("Words coverage in comments: "+wordsCoverage);
		
		System.out.println("Word\tImportance");
		for (String word:hm.keySet()) {
			String unescapedWord = StringEscapeUtils.unescapeHtml4(word);
			int score = hm.get(word);
			System.out.println(unescapedWord+"\t"+score);
		}
		
	}// end main

	
}
