package it.cnr.speech.textprocessing;

import java.io.File;

public class WordSegment {

	public double startTimeSec=0d;
	public double endTimeSec = 0d;
	public String word = "";
	public File containerFile;
	
	public String toString() {
		return startTimeSec+"s"+" - "+endTimeSec+"s"+" - "+word+" - "+containerFile.getAbsolutePath();
	}
	
	public File getContainerFile() {
		return containerFile;
	}

	public void setContainerFile(File containerFile) {
		this.containerFile = containerFile;
	}

	public double getStartTimeSec() {
		return startTimeSec;
	}

	public void setStartTimeSec(double startTimeSec) {
		this.startTimeSec = startTimeSec;
	}

	public double getEndTimeSec() {
		return endTimeSec;
	}

	public void setEndTimeSec(double endTimeSec) {
		this.endTimeSec = endTimeSec;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}


	
	public WordSegment(double startTimeSec,double endTimeSec, String word,File containerFile) {
			this.startTimeSec = startTimeSec;
			this.endTimeSec =endTimeSec;
			this.word = word;
			this.containerFile = containerFile;
	}
}
