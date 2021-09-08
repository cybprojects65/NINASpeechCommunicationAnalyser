package it.cnr.speech.osspeech2text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class AudioFromString {

	public static void string2audio(File audioStringFile, File audiofile) throws Exception{
		byte audiobytes[] = file2bytes(audioStringFile);
		bytes2audio(audiobytes,audiofile);
	}
	
	public static byte[] readWAVAudioFileData(final String filePath){
        byte[] data = null;
        try {
            final ByteArrayOutputStream baout = new ByteArrayOutputStream();
            final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, baout);
            audioInputStream.close();
            baout.close();
            data = baout.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
	
	public static void bytes2audio(byte[] audiobytes, File audiofile) throws Exception{
		InputStream b_in = new ByteArrayInputStream(audiobytes);
		AudioFormat format = new AudioFormat(8000f, 16, 1, true, false);
		AudioInputStream stream = new AudioInputStream(b_in, format,audiobytes.length);
		AudioSystem.write(stream, Type.WAVE, audiofile);
		stream.close();
		b_in.close();
	}
	
	public static byte[] file2bytes(File audioStringFile) throws Exception{
		//List<String> lines = Files.readAllLines(audioStringFile.toPath());
		
		//String bytesString = lines.get(0);
		String bytesString = new String(Files.readAllBytes(audioStringFile.toPath()),"UTF-8");
		//bytesString = URLDecoder.decode(bytesString,"UTF-8");
		return bytesString.getBytes();
	}
	
	public static String bytesToString(byte[] bytes) {
		
		String encodedString = Base64.getEncoder().encodeToString(bytes);
		return encodedString;
				
	}
	
	
	public static byte [] stringToBytes(String bytesString) {
		
		byte [] encodedBytes = Base64.getDecoder().decode(bytesString);
				
		return encodedBytes;
				
	}

}
