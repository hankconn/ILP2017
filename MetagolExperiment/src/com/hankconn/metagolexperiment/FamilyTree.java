package com.hankconn.metagolexperiment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class FamilyTree
{
	private int minIndividuals = 1500;
	private int maxIndividuals = 3000;
	private int numChildren = 8;
	private int numGenerations = 10;
	
	private Random rg;
	private List<String> maleNames;
	private List<String> femaleNames;
	private List<String> lastNames;
	private List<Family> families;
	private List<Person> spouses;
	private HashSet<String> individuals;
	private StringBuilder prologStr;
	private long minPositives;
	
	private HashMap<String,String> posFiles;
	
	public long getMinPositives()
	{
		return minPositives;
	}
	
	public HashMap<String,String> getPosFiles()
	{
		return posFiles;
	}
	
	public String getBKFile()
	{
		return prologStr.toString();
	}
	
	public Set<String> getIndividuals()
	{
		return individuals;
	}
	
	public FamilyTree(int minIndividuals, int maxIndividuals)
	{
		this.minIndividuals = minIndividuals;
		this.maxIndividuals = maxIndividuals;
	}
	
	private class Person
	{
		public String first;
		public String last;
		public boolean male;
		public int level;
		public Person(boolean male)
		{
			this(null, male);
		}
		public Person(String lastName, boolean male)
		{
			String last = lastName != null ? lastName : lastNames.remove(rg.nextInt(lastNames.size()));
			
			String first = null;
			String name = null;
			if(male)
			{
				first = maleNames.get(rg.nextInt(maleNames.size()));
				name = first.toLowerCase()+"_"+last.toLowerCase();
				while(individuals.contains(name))
				{
					first = maleNames.get(rg.nextInt(maleNames.size()));
					name = first.toLowerCase()+"_"+last.toLowerCase();
				}
				individuals.add(name);
			}
			else
			{
				first = femaleNames.get(rg.nextInt(femaleNames.size()));
				name = first.toLowerCase()+"_"+last.toLowerCase();
				while(individuals.contains(name))
				{
					first = femaleNames.get(rg.nextInt(femaleNames.size()));
					name = first.toLowerCase()+"_"+last.toLowerCase();
				}
				individuals.add(name);
			}
			prologStr.append((male ? "male(" : "female(") + name + ").\n"); 
			
			this.first = first;
			this.last = last;
			this.male = male;
		}
	}
	
	private class Family
	{
		public Person parent1;
		public Person parent2;
		public List<Person> children;
		public int level;

		public Family(Person parent1, Person parent2, List<Person> children, int level)
		{
			this.parent1 = parent1;
			this.parent2 = parent2;
			this.children = children;
			this.level = level;
		}
	}
	
	public int generateTree()
	{
		System.out.println("generating tree...");
		
		boolean done = false;
		int result = 0;
		while(!done)
		{
			for(int i=5;i<10 && !done;i++)
			{
				for(int j=5;j<10 && !done;j++)
				{
					for(int k=0;k<10 && !done;k++)
					{
						result = buildTree();
						
						System.gc();
						
						if(result < minIndividuals || result > maxIndividuals)
						{
							//System.out.println("bad tree: "+result);
							continue;
						}
						else
							done = true;
					}
				}
			}
		}
		
		System.out.println("found an acceptable tree with "+result+" individuals");

		// print BK file
		String bkFileName = writeBKToFile();

		// print pos files for each predicate
		posFiles = new HashMap<String,String>();
		minPositives = Long.MAX_VALUE;
		for(String predicate : Constants.predicates)
		{
			String posFileName = Cmd.generatePosFile(bkFileName,predicate);
			try {
				String posFile = new String(Files.readAllBytes(Paths.get(Constants.dirName+posFileName)));
				posFile = posFile.substring(1, posFile.length() - 2).replace("[", predicate+"(").replace("],", "),\n") + "),";
				long lineCount = posFile.chars().filter(ch -> ch =='\n').count();
				if(lineCount < minPositives)
					minPositives = lineCount;
				posFiles.put(predicate, posFile);
				FileIO.writeTempFile("pos_"+predicate+"_", ".txt", posFile);
				new File(Constants.dirName+posFileName).delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	public int buildTree()
	{
		rg = new Random(System.nanoTime());
		
		maleNames = new ArrayList<String>();
		for(String name : Constants.maleNames)
			maleNames.add(name);
		
		femaleNames = new ArrayList<String>();
		for(String name : Constants.femaleNames)
			femaleNames.add(name);
		
		lastNames = new ArrayList<String>();
		for(String name : Constants.lastNames)
			lastNames.add(name);
		
		families = new ArrayList<Family>();
		spouses = new ArrayList<Person>();
		individuals = new HashSet<String>();
		prologStr = new StringBuilder();
		
		Person adam = new Person(true);
		Person eve = new Person(false);

		Family f = generateAndAddFamily(adam, eve, 0);
		generateDescendants(f);
		for(Person spouse : spouses)
			generateAncestors(spouse);
		
		for(Family fam : families)
		{
			String parent1_name = fam.parent1.first + "_" + fam.parent1.last;
			String parent2_name = fam.parent2.first + "_" + fam.parent2.last;
			
			for(Person c : fam.children)
			{
				String child_name = c.first + "_" + c.last;
				prologStr.append((fam.parent1.male ? "father(" : "mother(")+parent1_name.toLowerCase()+","+child_name.toLowerCase()+").\n");
				prologStr.append((fam.parent2.male ? "father(" : "mother(")+parent2_name.toLowerCase()+","+child_name.toLowerCase()+").\n");
			}
		}
		
		prologStr.deleteCharAt(prologStr.length() - 1);
		
		return individuals.size();
	}
	
	private Family generateAndAddFamily(Person parent1, Person parent2, int level)
	{
		return generateAndAddFamily(parent1, parent2, level, null);
	}
	
	private Family generateAndAddFamily(Person parent1, Person parent2, int level, Person spouse)
	{
		//System.out.println(parent1.first + " " + parent1.last + " and " + parent2.first + " " + parent2.last + ": ");
		int numChildren = rg.nextInt(this.numChildren - (spouse == null ? 1 : 2)) + 1;
		List<Person> children = new ArrayList<Person>();
		for(int i=0;i<numChildren;i++)
		{
			boolean male = rg.nextBoolean();
			String last = parent1.male ? parent1.last : parent2.last;
			Person child = new Person(last, male);
			children.add(child);
			//System.out.println("\t"+child.first + " " + child.last);
		}
		if(spouse != null)
			children.add(spouse);
		
		Family f = new Family(parent1, parent2, children, level + 1);
		families.add(f);
		return f;
	}
	
	private void generateDescendants(Family f)
	{
		if(lastNames.size() == 0 || individuals.size() >= maxIndividuals / numGenerations)
			return;
		
		for(Person c : f.children)
		{
			boolean married = rg.nextBoolean();
			if(married)
			{
				Person spouse = new Person(!c.male);
				spouse.level = f.level;
				spouses.add(spouse);
				Family f2 = generateAndAddFamily(c, spouse, f.level + 1);
				
				if(f2.level < numGenerations)
					generateDescendants(f2);
			}
		}
	}
	
	public void generateAncestors(Person spouse)
	{
		if(lastNames.size() == 0 || individuals.size() >= maxIndividuals - numChildren)
			return;
		
		Person father = new Person(spouse.last, true);
		father.level = spouse.level - 1;
		
		Person mother = new Person(false);
		mother.level = spouse.level - 1;
		
		Family f = generateAndAddFamily(father, mother, spouse.level - 1, spouse);
		if(f.level > 0)
		{
			generateAncestors(father);
			generateAncestors(mother);
		}
	}
	
	public String writeBKToFile()
	{
		return FileIO.writeTempFile("tree_",".pl",getBKFile());
	}
	
	public static void main(String[] args)
	{
		FamilyTree tree = new FamilyTree(5000, 10000);
		int result = tree.generateTree();
	}
}
