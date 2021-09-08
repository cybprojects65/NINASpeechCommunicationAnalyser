package it.cnr.speech.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Utils {

	public static File[] getFiles(File[] files, String extension) {
		List<File> allFiles = new ArrayList<>();
		
		for (File f : files) {
			if (f.getName().endsWith(extension))
				allFiles.add(f);
		}
		File[] newFiles = new File[allFiles.size()];
		newFiles = allFiles.toArray(newFiles);
		return newFiles;
	}
	
	 public static void sortByNumber(File[] files) {
         Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    int n1 = extractNumber(o1.getName());
                    int n2 = extractNumber(o2.getName());
                    return n1 - n2;
                }

                private int extractNumber(String name) {
                    int i = 0;
                    try {
                        int s = name.lastIndexOf('_')+1;
                        int e = name.lastIndexOf('.');
                        String number = name.substring(s, e);
                        i = Integer.parseInt(number);
                    } catch(Exception e) {
                        i = 0; 
                    }
                    return i;
                }
            });

            for(File f : files) {
                System.out.println(f.getName());
            }
        }
        
	public static void writeSignal2File(double[] signal,File output) {
		
		StringBuffer sb = new StringBuffer();
		for (int k=0;k<signal.length;k++) {
			sb.append(signal[k]);
			if (k<signal.length-1)
				sb.append(",");
		}	
		FileWriter fw;
		try {
			fw = new FileWriter(output);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static short findMax(short[] signal) {
		
		short max = -Short.MAX_VALUE;
		for (int i=0;i<signal.length;i++) {
			if (signal[i]>max)
				max = signal[i];
		}
		return max;
	} 
	
	public static double findMax(double[] signal) {
		
		double max = -Short.MAX_VALUE;
		for (int i=0;i<signal.length;i++) {
			if (signal[i]>max)
				max = signal[i];
		}
		return max;
	} 
	
	public static double[] derivative(double[] signal) {
		
		double derivative [] = new double[signal.length-1];
		
		for (int i=1;i<signal.length;i++) {
			derivative [i-1] = signal[i]-signal[i-1]; 
		}
		
		return derivative;
	}
	
	public static double mean(double[] signal) {
		
		double mean = 0;
		for (int i=0;i<signal.length;i++) {
				mean = signal[i]+mean;
		}
		
		return mean/(double)signal.length;
	}

	public static double roundDecimal(double number,int decimalposition){
		
		double n = (double)Math.round(number * Math.pow(10.00,decimalposition))/Math.pow(10.00,decimalposition);
		return n;
	}
	
	public static double cohensKappaForDichotomy(long NumOf_A1_B1, long NumOf_A1_B0, long NumOf_A0_B1, long NumOf_A0_B0, boolean abs){
		long  T = NumOf_A1_B1+NumOf_A1_B0+NumOf_A0_B1+NumOf_A0_B0;
		
		double Pra = (double)(NumOf_A1_B1+NumOf_A0_B0)/(double) T ;
		double Pre1 = (double) (NumOf_A1_B1+NumOf_A1_B0) * (double) (NumOf_A1_B1+NumOf_A0_B1)/(double) (T*T);
		double Pre2 = (double) (NumOf_A0_B0+NumOf_A0_B1) * (double) (NumOf_A0_B0+NumOf_A1_B0)/(double) (T*T);
		double Pre = Pre1+Pre2;
		double Kappa = (Pra-Pre)/(1d-Pre);
		if (abs)
			Kappa = Math.abs(Pra-Pre)/(1d-Pre);
		return roundDecimal(Kappa,3);
	}
	
	public static double cohensKappaForDichotomy(long NumOf_A1_B1, long NumOf_A1_B0, long NumOf_A0_B1, long NumOf_A0_B0){
		long  T = NumOf_A1_B1+NumOf_A1_B0+NumOf_A0_B1+NumOf_A0_B0;
		
		double Pra = (double)(NumOf_A1_B1+NumOf_A0_B0)/(double) T ;
		double Pre1 = (double) (NumOf_A1_B1+NumOf_A1_B0) * (double) (NumOf_A1_B1+NumOf_A0_B1)/(double) (T*T);
		double Pre2 = (double) (NumOf_A0_B0+NumOf_A0_B1) * (double) (NumOf_A0_B0+NumOf_A1_B0)/(double) (T*T);
		double Pre = Pre1+Pre2;
		double Kappa = (Pra-Pre)/(1d-Pre);
		return roundDecimal(Kappa,3);
	}
	
	public static String kappaClassificationLandisKoch(double kappa){
		if (kappa<0)
			return "Poor";
		else if ((kappa>=0)&&(kappa<=0.20))
			return "Slight";
		else if ((kappa>=0.20)&&(kappa<=0.40))
			return "Fair";
		else if ((kappa>0.40)&&(kappa<=0.60))
			return "Moderate";
		else if ((kappa>0.60)&&(kappa<=0.80))
			return "Substantial";
		else if (kappa>0.80)
			return "Almost Perfect";
		else
			return "Not Applicable";
	}
	
	public static String kappaClassificationFleiss(double kappa){
		if (kappa<0)
			return "Poor";
		else if ((kappa>=0)&&(kappa<=0.40))
			return "Marginal";
		else if ((kappa>0.4)&&(kappa<=0.75))
			return "Good";
		else if (kappa>0.75)
			return "Excellent";
		else
			return "Not Applicable";
	}
}
