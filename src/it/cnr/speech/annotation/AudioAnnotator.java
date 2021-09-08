package it.cnr.speech.annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import it.cnr.speech.clustering.EnergyPitchClusterer;
import it.cnr.speech.sphinx.segmentation.EnergySegmenter;

public class AudioAnnotator {

		public void tagAudioWithClusters(File folder, File audioFile, String[] clustersLabels) throws Exception {

			File segmentation = new File(folder, EnergySegmenter.ENERGYSEGMENTATIONFILE);
			File clusterFile = new File(folder, EnergyPitchClusterer.CLUSTERINGFILE);

			BufferedWriter fw = new BufferedWriter(new FileWriter(new File(audioFile.getAbsolutePath().replace(".wav", ".lab"))));
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

		
	
}
