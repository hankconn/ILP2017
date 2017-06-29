package com.hankconn.metagolexperiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

public class Trial extends Thread
{
	private int increment;
	private int trialNum;
	private FamilyTree tree;
	private Random rg;
	private List<String> predicates;
	private List<Map<String,List<String>>> accumulatedLearned;
	private List<String> constants;
	private int positiveTrainingSize;
	private int negativeTrainingSize;
	private int testSize;
	private int trainingPercent;
	private int testPercent;
	private int experimentID;
	
	public Trial(FamilyTree tree, int increment, int trialNum, int trainingPercent, int testPercent, int experimentID)
	{
		this.increment = increment;
		this.trialNum = trialNum;
		this.tree = tree;
		this.trainingPercent = trainingPercent;
		this.testPercent = testPercent;
		this.experimentID = experimentID;
	}
	
	public void run()
	{
		execTrial();
		tree = null;
		rg = null;
		predicates=null;
		accumulatedLearned=null;
		constants=null;
		System.gc();
	}
	
	private void execTrial()
	{
		try
		{
			rg = new Random(System.nanoTime());
			
			// get the list of predicates in canonical order
			predicates = new ArrayList<String>();
			for(String p : Constants.predicates)
				predicates.add(p);
			
			// swap the predicates increment number of times
			for(int i=0;i<increment;i++)
			{
				int index1 = rg.nextInt(predicates.size());
				String p1 = predicates.get(index1);
				
				int index2 = rg.nextInt(predicates.size());
				while(index2 == index1)
					index2 = rg.nextInt(predicates.size());
				String p2 = predicates.get(index2);
				
				predicates.set(index1, p2);
				predicates.set(index2, p1);
			}
			
			// do a run for each predicate
			accumulatedLearned = new ArrayList<Map<String,List<String>>>();
			for(String predicate : predicates)
			{
				// generate code
				String filename = getCode(predicate);
				
				// exec code
				List<Object> out = Cmd.execCode(filename);
				Map<String,List<String>> learnedPredicates = (Map<String,List<String>>) out.get(0);
				long runningTime = ((Long)out.get(1)).longValue();
				
				String definitionLearned = "";
				if(learnedPredicates.size() > 0)
				{
					for(List<String> ls : learnedPredicates.values())
						for(String s : ls)
							definitionLearned += s + "\n";
					
					accumulatedLearned.add(learnedPredicates);
				}
				
				// check definition
				int[] result = checkDefinition(predicate, learnedPredicates);
				
				// save result
				MySQL.saveResult(increment, 
						trialNum, 
						constants.size(), 
						predicate,
						trainingPercent > 0 ? trainingPercent : positiveTrainingSize,
						testPercent > 0 ? testPercent : testSize,
						definitionLearned,
						result,
						experimentID,
						runningTime);
			}
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
	
	public int[] checkDefinition(String predicate, Map<String,List<String>> learnedPredicates)
	{
		StringBuilder code = new StringBuilder();
		code.append(tree.getBKFile());
		code.append("\n");
		
		for(Map<String,List<String>> learn : accumulatedLearned)
		{
			for(List<String> def : learn.values())
			{
				for(String s : def)
				{
					code.append(s + "\n");
				}
				code.append("\n");
			}
		}

        //code.append("is_same(A,B):-A = B.\n");
        //code.append("is_not_same(A,B):-A \\= B.\n");
        code.append("\n");
		
        String filename = FileIO.writeTempFile("bk_def_"+predicate+"_", ".pl", code.toString());
        code = null;
        
        String checkFilename = Cmd.checkDefinition(predicate, filename);

        long start = System.currentTimeMillis();
		try {
			String output = new String(Files.readAllBytes(Paths.get(Constants.dirName+checkFilename))).trim();
			if(output.length() <= 2)
				output = "";
			else
			{
				StringBuilder ob = new StringBuilder();
				for(int i=0;i<output.length();i++)
				{
					if(i == 0 || i >= output.length() - 2)
						continue;
					else if(output.charAt(i) == '[')
						ob.append(predicate + "(");
					else if(output.charAt(i) == ']' && output.charAt(i + 1) == ','){
						ob.append("),\n");
						i++;
					}else
						ob.append(output.charAt(i));
				}
				ob.append("),");
				output = ob.toString();
			}
			new File(Constants.dirName+checkFilename).delete();
			
			String[] outputArray = output.split("\n");
			output = null;
			System.gc();
			
			Set<String> outputSet = new HashSet<String>();
			for(String o : outputArray)
				outputSet.add(o);
			outputArray = null;
			
			String posFile = tree.getPosFiles().get(predicate);

			String[] posArray = posFile.split("\n");
			posFile = null;
			
			Set<String> allPositivesSet = new HashSet<String>();
			for(String o : posArray)
				allPositivesSet.add(o);
			
			int totalPositives = 0;
			int totalNegatives = 0;
			int truePositives = 0;
			int trueNegatives = 0;
			int falsePositives = 0;
			int falseNegatives = 0;
			
			Set<String> testPositivesSet = new HashSet<String>();
			int posDups = 0;
			
			int len = posArray.length;
			if(testPercent < 0)
				testSize = (int) (tree.getMinPositives() - positiveTrainingSize);
			else
				testSize = (int)(allPositivesSet.size() * (testPercent/100.0f));
			for(int i=0;i<testSize;i++)
			{
				int index = rg.nextInt(len);
				if(!testPositivesSet.contains(posArray[index]))
					testPositivesSet.add(posArray[index]);
				else
					posDups++;
			}
			posArray = null;
			
			Set<String> testNegativesSet = new HashSet<String>();
			int negDups = 0;
			len = constants.size();
			
			if(testPercent > 0)
				testSize = (int)((len*len - allPositivesSet.size())*(testPercent/100.0f));
			
			for(int i=0;i<testSize;i++)
			{
				int index = rg.nextInt(len);
				String c1 = constants.get(index);
				String checkLine = predicate + "(" + c1;
				String c2 = constants.get(index);
				checkLine += "," + c2 + "),";
				
				if(allPositivesSet.contains(checkLine))
				{
					i--;
					continue;
				}
				else if(!testNegativesSet.contains(checkLine))
					testNegativesSet.add(checkLine);
				else
					negDups++;
			}
			allPositivesSet = null;
			
			/*
			StringBuilder test = new StringBuilder();
			test.append("testPositives:\n");
			test.append("\n");
			for(String p : testPositivesSet)
				test.append(p + "\n");
			test.append("\n");
			
			test.append("testNegatives:\n");
			test.append("\n");
			for(String p : testNegativesSet)
				test.append(p + "\n");
			test.append("\n");
			
			test.append("output:\n");
			test.append("\n");
			for(String p : outputSet)
				test.append(p + "\n");
			test.append("\n");
			
			FileIO.writeTempFile("test_"+predicate+"_", ".txt", test.toString());
			test = null;
			*/
			
			totalPositives = testPositivesSet.size();
			totalNegatives = testNegativesSet.size();
			
			for(String s : testPositivesSet)
			{
				if(outputSet.contains(s))
					truePositives++;
				else
					falseNegatives++;
			}
			testPositivesSet = null;
			
			for(String s : testNegativesSet)
			{
				if(outputSet.contains(s))
					falsePositives++;
				else
					trueNegatives++;
			}
			testNegativesSet = null;
			
			long end = (System.currentTimeMillis() - start);
			//if(end > 5000)
				//System.out.println("java long running checkDefinition: "+end);
			
			return new int[]{totalPositives, truePositives, falsePositives, totalNegatives, trueNegatives, falseNegatives};
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getCode(String predicate)
	{
		StringBuilder code = new StringBuilder();

        // assemble the prolog code to execute
        code.append(":- use_module('../metagol').\n");
        code.append(":- user:call(op(950,fx,'@')).\n");
        code.append("\n");
        code.append("%% background knowledge\n");
        code.append("\n");

        String bkString = tree.getBKFile();
        String[] bkStrings = bkString.split("\n");
        constants = new ArrayList<String>();
        for(String s : bkStrings)
        {
        	if(s.indexOf(",") > -1)
        		continue;
        	
        	constants.add(s.substring(s.indexOf("(") + 1, s.indexOf(")")));
        }
        
        code.append(bkString + "\n");
        code.append("\n");
    	code.append("%% pre-defined background knowledge\n");
        code.append("\n");
        //code.append("is_same(A,B):-A = B.\n");
        //code.append("is_not_same(A,B):-A \\= B.\n");
        code.append("\n");
        
        List<String> additionalPrimList = new ArrayList<String>();
		for(Map<String,List<String>> learn : accumulatedLearned)
		{
			for(Entry<String,List<String>> def : learn.entrySet())
			{
				additionalPrimList.add(def.getKey());
				
				for(String s : def.getValue())
				{
					code.append(s + "\n");
				}
				code.append("\n");
			}
		}
		
        code.append("\n");
        code.append("%% tell metagol to use the BK\n");
        code.append("\n");
        code.append("prim(male/1).\n");
        code.append("prim(female/1).\n");
        code.append("prim(mother/2).\n");
		code.append("prim(father/2).\n");
		//code.append("prim(is_same/2).\n");
		//code.append("prim(is_not_same/2).\n");
        for(String prim : additionalPrimList)
        	code.append("prim("+prim+").\n");
        code.append("\n");
        code.append("%% metarules\n");
        code.append("metarule([P,Q],([P,X]:-[[Q,X]])).\n");
        code.append("metarule([P,Q,R],([P,X]:-[[Q,X],[R,X]])).\n");
        code.append("metarule([P,Q,R],([P,X]:-[[Q,Z,X],[R,Z]])).\n");
        code.append("metarule([P,Q],([P,A,B]:-[[Q,B,A]])).\n");
        code.append("metarule([P,Q,R],([P,X,Y]:-[[Q,X],[R,Y]])).\n");
        code.append("metarule([P,Q,R],([P,A,B]:-[[Q,A,B],[R,A]])).\n");
        code.append("metarule([P,Q,R],([P,A,B]:-[[Q,A,B],[R,A,B]])).\n");
        code.append("metarule([P,Q],([P,A,B]:-[[Q,A,B]])).\n");
        code.append("metarule([P,Q,R],([P,A,B]:-[[Q,A,C],[R,C,B]])).\n");
        code.append("\n");
        code.append("%% learn from examples\n");
        code.append("\n");
        code.append("a:- T1 = [\n");
        
        Set<String> positiveSample = new HashSet<String>();
        String[] posString = tree.getPosFiles().get(predicate).split("\n");
        Set<String> allPositives = new HashSet<String>();
        for(String s : posString)
        	allPositives.add(s);
        
        if(trainingPercent > 0)
        	positiveTrainingSize = (int)Math.floor(posString.length * (trainingPercent/100.0f));
        else
        	positiveTrainingSize = (int)Math.floor(tree.getMinPositives() * 0.20f);
        
        for(int i=0;i<positiveTrainingSize;i++)
        {
        	int index = rg.nextInt(posString.length);
        	
        	if(!positiveSample.contains(posString[index]))
        	{
        		positiveSample.add(posString[index]);
        		code.append(posString[index] + "\n");
        	}
        }
        
        code.deleteCharAt(code.length() - 1);
        code.deleteCharAt(code.length() - 1);
        code.append("\n]/[\n");

        Set<String> negativeSample = new HashSet<String>();
        if(trainingPercent > 0)
        	negativeTrainingSize = (int)Math.floor((constants.size()*constants.size() - posString.length)*(trainingPercent/100.0f));
        else
        	negativeTrainingSize = positiveTrainingSize;
        
        posString = null;
        
        for(int i=0;i<negativeTrainingSize;i++)
        {
        	int index1 = rg.nextInt(constants.size());
        	String c1 = constants.get(index1);
        	int index2 = rg.nextInt(constants.size());
        	
        	String c2 = constants.get(index2);
        	String checkLine = predicate + "(" + c1 + "," + c2 + "),";
        	if(allPositives.contains(checkLine))
        	{
        		// not a valid negative. skip and try again.
        		i--;
        		continue;
        	}
        	else if(!negativeSample.contains(checkLine))
        	{
        		negativeSample.add(checkLine);
        	}
        	code.append(checkLine + "\n");
        }
        code.deleteCharAt(code.length() - 1);
        code.deleteCharAt(code.length() - 1);
        code.append("\n],\n");
        code.append("learn_seq([T1],Prog),\n");
        code.append("pprint(Prog).\n");
        
        return FileIO.writeTempFile("exec_"+predicate+"_", ".pl", code.toString());
	}
}
