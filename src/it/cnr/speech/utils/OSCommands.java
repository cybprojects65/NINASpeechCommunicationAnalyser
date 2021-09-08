package it.cnr.speech.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class OSCommands {

	
	public static String printConsole(Process process) {
		StringBuffer uberbuffer = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			System.out.println("------");
			String line = br.readLine();
			StringBuffer sb = new StringBuffer();
			while (line != null) {
				line = br.readLine();
				sb.append(line + "\n");
			}
			System.out.println(sb);
			uberbuffer.append(sb.toString());

			br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			line = br.readLine();
			sb = new StringBuffer();

			while (line != null) {
				line = br.readLine();
				sb.append(line + "\n");
			}
			System.out.println(sb);
			uberbuffer.append(sb.toString());
			System.out.println("------");
		} catch (Exception e) {
			System.out.println("---END BY PROCESS INTERRUPTION---");
			e.printStackTrace();
		}

		return uberbuffer.toString();
	}

	

	public static String executeOSCommands(String[] commands)
			{

		try {
			Process process = Runtime.getRuntime().exec(commands[0]);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			for (String command : commands) {
				System.out.println(">>" + command);
				bw.write(command + "\n");
			}
			bw.close();
			return printConsole(process);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	


}
