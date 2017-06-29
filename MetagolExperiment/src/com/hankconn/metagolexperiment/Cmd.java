package com.hankconn.metagolexperiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

public class Cmd extends Thread
{
	private Object output;
	private long pid;
	private long runningTime = -1;

	public void setRunningTime(long runningTime)
	{
		this.runningTime = runningTime;
	}
	public long getRunningTime()
	{
		return runningTime;
	}

	public void setOutput(Object output)
	{
		this.output = output;
	}
	public Object getOutput()
	{
		return output;
	}
	
	public void setPID(long pid)
	{
		this.pid = pid;
	}
	public long getPID()
	{
		return pid;
	}
	
	public static void main(String[] args)
	{
	}
	
	public static String checkDefinition(String predicate, String filename)
	{
		Random rg = new Random(System.nanoTime());
		String filename2 = "check_"+predicate+"_"+rg.nextInt(Integer.MAX_VALUE);
		
		//System.out.println("starting");
		Cmd t = new Cmd(){
			public void run()
			{
				ProcessBuilder ps = new ProcessBuilder(Constants.execStr);
				ps.directory(new File(Constants.dirName));
				StringBuilder output = new StringBuilder();
				StringBuilder stderr = new StringBuilder();
				
				try {
					Process process = ps.start();
					setPID(getProcessID(process));
					OutputStream stdin = process.getOutputStream();
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
					writer.write("consult("+filename.replace(Constants.dirName, "").replace(".pl", "")+").\r\n");
					writer.write("open('"+filename2+"',write, Stream), forall(findall([A,B],"+predicate+"(A,B),Z), write(Stream,Z)), close(Stream).\r\n");
			        writer.flush();
			        writer.close();
			        
			        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			        String line;
			        while ((line = in.readLine()) != null) {
			        	output.append(line+"\r\n");
			        }

			        in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			        while ((line = in.readLine()) != null) {
			        	stderr.append(line+"\r\n");
			        }
			        
			        process.waitFor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		        //System.out.println("output: "+output.toString());
		        //System.out.println("error: "+stderr.toString());
			}
		};
		t.start();

		//System.out.println("waiting");
		int timeout = 300 * 1000; // this should never time out!
		long millis = System.currentTimeMillis();
		long runningTime = System.currentTimeMillis() - millis;
		boolean alert=false;
		while(t.isAlive() && runningTime < timeout)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runningTime = System.currentTimeMillis() - millis;
			
			if(runningTime > 120000 && !alert){
				//System.out.println("long running checkDefinition...");
				alert=true;
			}
		}
		
		//if(alert)
		//	System.out.println("checkDefinition took "+runningTime);
		
		//System.out.println("done waiting: "+runningTime);
		
		if(runningTime >= timeout)
		{
			killProcess(t.getPID());
		}

		//System.out.println("done");
		
		return filename2;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Object> execCode(String filename)
	{
		setFilePermissions(filename);
		
		//System.out.println("starting");
		Cmd t = new Cmd(){
			public void run()
			{
				setOutput(new HashMap<String,List<String>>());
				
				ProcessBuilder ps = new ProcessBuilder(Constants.execStr);
				ps.directory(new File(Constants.dirName));
				StringBuilder output = new StringBuilder();
				StringBuilder stderr = new StringBuilder();
				
				try {
					Process process = ps.start();
					setPID(getProcessID(process));
					OutputStream stdin = process.getOutputStream();
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
					writer.write("print('Starting metagol web interface...\n\n').\r\n");
					writer.write("consult("+filename.replace(Constants.dirName, "").replace(".pl", "")+").\r\n");
			        
					writer.flush();
					stdin.flush();
			        Thread.sleep(5000);
			        
					long start = System.currentTimeMillis();
					writer.write("a.\r\n");
					writer.write("print('\nnDone running metagol web interface.').\r\n");
			        writer.flush();
			        writer.close();
			        
			        process.waitFor();
			        setRunningTime(System.currentTimeMillis() - start); // rough estimate
			        
			        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			        String line;
			        while ((line = in.readLine()) != null) {
			        	output.append(line+"\r\n");
			        }

			        in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			        while ((line = in.readLine()) != null) {
			        	stderr.append(line+"\r\n");
			        }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String out = output.toString();
				String err = stderr.toString();
				
		        //System.out.println("output: "+out);
		        //System.out.println("error: "+err);
				
				Map<String,List<String>> learnedPredicates = new HashMap<String,List<String>>();
				int index = out.indexOf("Starting metagol web interface...");
				if(index > -1)
				{
					out = out.substring(index);
					String cleanStr = "Done running metagol web interface.";
					index = out.lastIndexOf(cleanStr);
					if(index > -1)
					{
						out = out.substring(0, index + cleanStr.length());

						// parse out the learned definition to return as separate output.
						int start = out.lastIndexOf("\n% clauses: ");
						if(start > -1)
							start = out.indexOf("\n", start + 1);
						if(start > -1)
							start += 1;
						
						while(start > -1)
						{
							int end = out.indexOf("\n", start);
							
							if(end == -1)
								break;
							if(end == start)
							{
								start +=1;
								continue;
							}
							
							String line = out.substring(start, end);
							
							if(line.equals(cleanStr))
							{
								break;
							}
							
							int lineIndex = line.indexOf("(");
							if(lineIndex > -1)
							{
								String predicateName = line.substring(0, lineIndex);
								lineIndex += 1;
								int numArgs = 1;
								int numParens = 0;
								while(true)
								{
									String lineSub = line.substring(lineIndex, lineIndex + 1);
									
									if(numParens == 0 && lineSub.equals(","))
										numArgs++;
									else if(lineSub.equals("("))
										numParens++;
									else if(lineSub.equals(")") && numParens > 0)
										numParens--;
									else if(lineSub.equals(")"))
										break;
									
									lineIndex++;
									if(lineIndex == line.length())
										break;
								}
								predicateName += "/" + numArgs;
								
								if(!learnedPredicates.containsKey(predicateName))
									learnedPredicates.put(predicateName, new ArrayList<String>());

								learnedPredicates.get(predicateName).add(line);
							}
							
							start = end + 1;
						}
					}
				}
				
				setOutput(learnedPredicates);
			}
		};
		t.start();

		//System.out.println("waiting");
		int timeout = 60 * 1000;
		long millis = System.currentTimeMillis();
		long runningTime = System.currentTimeMillis() - millis;
		while(t.isAlive() && runningTime < timeout)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runningTime = System.currentTimeMillis() - millis;
		}
		
		//System.out.println("done waiting: "+runningTime);
		
		if(runningTime >= timeout)
		{
			//System.out.println("killing " + t.getPID() + " - runningTime: "+runningTime+", timeout: "+timeout);
			killProcess(t.getPID());
		}

		//System.out.println("done");
		List<Object> out = new ArrayList<Object>();
		out.add(t.getOutput());
		out.add(new Long(t.getRunningTime()));
		return out;
	}
	
	public static String generatePosFile(String bkFileName, String predicate)
	{
		Random rg = new Random(System.nanoTime());
		String filename = "pos_"+predicate+"_"+rg.nextInt(Integer.MAX_VALUE)+".txt";
		
		//System.out.println("starting");
		Cmd t = new Cmd(){
			public void run()
			{
				ProcessBuilder ps = new ProcessBuilder(Constants.execStr);
				ps.directory(new File(Constants.dirName));
				StringBuilder output = new StringBuilder();
				StringBuilder stderr = new StringBuilder();
				
				try {
					Process process = ps.start();
					setPID(getProcessID(process));
					OutputStream stdin = process.getOutputStream();
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
					writer.write("consult("+bkFileName.replace(Constants.dirName, "").replace(".pl", "")+").\r\n");
					writer.write("consult(kinship_definitions).\r\n");
					writer.write("open('"+filename+"',write, Stream), forall(findall([A,B],"+predicate+"(A,B),Z), write(Stream,Z)), close(Stream).\r\n");
			        writer.flush();
			        writer.close();
			        
			        
			        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			        String line;
			        while ((line = in.readLine()) != null) {
			        	output.append(line+"\r\n");
			        }

			        in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			        while ((line = in.readLine()) != null) {
			        	stderr.append(line+"\r\n");
			        }
			        
			        process.waitFor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		        //System.out.println("output: "+output.toString());
		        //System.out.println("error: "+stderr.toString());
			}
		};
		t.start();

		//System.out.println("waiting");
		int timeout = 5 * 1000;
		long millis = System.currentTimeMillis();
		long runningTime = System.currentTimeMillis() - millis;
		while(t.isAlive() && runningTime < timeout)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runningTime = System.currentTimeMillis() - millis;
		}
		
		//System.out.println("done waiting: "+runningTime);
		
		if(runningTime >= timeout)
		{
			//System.out.println("killing " + t.getPID());
			killProcess(t.getPID());
		}

		//System.out.println("done");
		
		return filename;
	}
	
	public static void setFilePermissions(String filename)
	{
		ProcessBuilder ps = new ProcessBuilder("icacls.exe", filename, "/grant", "Everyone:(OI)(CI)F");
		try {
			Process process = ps.start();
	        process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void killProcess(long pid)
	{
		ProcessBuilder ps = new ProcessBuilder("taskkill.exe", "/F", "/T", "/PID", ""+pid);
		try {
			Process process = ps.start();
	        process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static long getProcessID(Process p)
    {
        long result = -1;
        try
        {
            //for windows
            if (p.getClass().getName().equals("java.lang.Win32Process") ||
                   p.getClass().getName().equals("java.lang.ProcessImpl")) 
            {
                Field f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);              
                long handl = f.getLong(p);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE hand = new WinNT.HANDLE();
                hand.setPointer(Pointer.createConstant(handl));
                result = kernel.GetProcessId(hand);
                f.setAccessible(false);
            }
            //for unix based operating systems
            else if (p.getClass().getName().equals("java.lang.UNIXProcess")) 
            {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                result = f.getLong(p);
                f.setAccessible(false);
            }
        }
        catch(Exception ex)
        {
            result = -1;
        }
        return result;
    }
}
