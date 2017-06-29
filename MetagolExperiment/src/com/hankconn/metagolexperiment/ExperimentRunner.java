package com.hankconn.metagolexperiment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ExperimentRunner
{
	public static void main(String[] args)
	{
		// main method for starting the experiment runner
		try {
			System.out.println("begin experiment");
			MySQL.init();
			
			// configurable values
			int treeMin = 4900;
			int treeMax = 5000;
			int numTrialsPerIncrement = 20;
			int experimentID = 1;
			
			// for each increment 0->40 (by 2s) launch x# of trial threads
			ExecutorService executor = Executors.newFixedThreadPool(6);
			
			for(int i=0;i<5;i++)
			{
				// generate a family tree
				FamilyTree tree = new FamilyTree(treeMin, treeMax);
				int result = tree.generateTree();
				System.out.println("Generated a tree with " + result + " individuals");
				
				for(int j=0;j<10;j++)
				{
		            Runnable worker = new Trial(tree, 0, 0, 1, 10, experimentID);
		            executor.execute(worker);
				}
			}
			
			System.out.println("waiting for trials to complete");
			int queued = ((ThreadPoolExecutor)executor).getQueue().size();
			int active = ((ThreadPoolExecutor)executor).getActiveCount();
			int notCompleted = queued + active; // approximate
        	System.out.println(notCompleted + " tasks remaining");
	        executor.shutdown();
	        int loop = 0;
	        long startTime = System.currentTimeMillis();
	        long avgTaskTime = 0;
	        long tasksCompleted = 0;
	        long diffTime = 0;
	        while(!executor.isTerminated())
	        {
	        	try
	        	{
	        		Thread.sleep(1000);
	        	}
	        	catch(InterruptedException ie)
	        	{
	        		
	        	}
	        	loop++;
	        	if(loop % 60 == 0)
	        	{
	    			queued = ((ThreadPoolExecutor)executor).getQueue().size();
	    			active = ((ThreadPoolExecutor)executor).getActiveCount();
	    			tasksCompleted += notCompleted - (queued + active);
	    			diffTime += System.currentTimeMillis() - startTime;
	    			startTime = System.currentTimeMillis();
	    			avgTaskTime = tasksCompleted == 0 ? 0 : diffTime/tasksCompleted;
	    			notCompleted = queued + active; // approximate
	            	System.out.println(notCompleted + " tasks remaining (approx "+(avgTaskTime*notCompleted / 1000 / 60)+"m)");
	    	        executor.shutdown();
	        	}
	        }
	        
	        System.out.println("done with experiment");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
