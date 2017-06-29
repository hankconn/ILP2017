package com.hankconn.metagolexperiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class FileIO
{
	public static String writeTempFile(String prefix, String suffix, String filedata)
	{
		Random rg = new Random(System.nanoTime());
		String filename = Constants.dirName+prefix+rg.nextInt(Integer.MAX_VALUE)+suffix;
		
		try {
			PrintWriter out = new PrintWriter(filename);
			out.print(filedata);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return filename;
	}
	
	public static void setPermissions(String filename)
	{
		File f = new File(filename);
		f.setExecutable(true, false);
		f.setReadable(true, false);
		f.setWritable(true, false);
	}
}
