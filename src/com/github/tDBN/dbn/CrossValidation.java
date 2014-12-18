package com.github.tDBN.dbn;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.tDBN.utils.Utils;

public class CrossValidation {

	long randomSeed = new Random().nextLong();
	private Random r = new Random(randomSeed);

	private Observations o;

	private int[][] allData;

	private String[][] allPassiveData;

	// contains an extra column for fold identification
	private List<int[][]> stratifiedData;

	private List<String[][]> stratifiedPassiveData;

	private int numFolds;

	private class Pair {
		int a, b;

		private Pair(int a, int b) {
			this.a = a;
			this.b = b;
		}
	}

	public CrossValidation setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
		r.setSeed(randomSeed);
		return this;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	private Pair countInstancesOfFold(int fold) {
		int n = o.numAttributes();
		int m = o.getMarkovLag();
		int countFold = 0;
		int countNonFold = 0;
		for (int c = 0; c < stratifiedData.size(); c++) {
			for (int[] row : stratifiedData.get(c)) {
				if (row[(m + 1) * n] == fold)
					countFold++;
				else
					countNonFold++;
			}
		}

		return new Pair(countFold, countNonFold);
	}

	private List<Integer> calculateFoldIds(int numInstances, int numFolds) {
		List<Integer> foldIds = new ArrayList<Integer>(numInstances);

		int minFoldSize = numInstances / numFolds;
		int rest = numInstances % numFolds;

		for (int i = 0; i < numFolds; i++)
			for (int j = 0; j < minFoldSize; j++)
				foldIds.add(i);

		for (int i = 0; i < rest; i++)
			foldIds.add(i);

		Collections.shuffle(foldIds, r);

		return foldIds;
	}

	private int countInstancesOfClass(int classAttribute, int value) {
		int n = o.numAttributes();
		int m = o.getMarkovLag();
		int count = 0;
		for (int i = 0; i < allData.length; i++)
			if (allData[i][m * n + classAttribute] == value)
				count++;
		return count;
	}

	public CrossValidation(Observations o, int numFolds, Integer classAttribute) {

		this.o = o;

		// initialize allData
		int N = o.numObservations(-1);
		int n = o.numAttributes();
		int T = o.numTransitions();
		int m = o.getMarkovLag();
		int nPassive = o.numPassiveAttributes();

		allData = new int[N][(m + 1) * n];

		allPassiveData = new String[N][(m + 1) * nPassive];

		int[][][] usefulObservations = o.getObservationsMatrix();
		String[][][] passiveObservations = o.getPassiveObservationsMatrix();
		int i = 0;
		for (int t = 0; t < T; t++)
			for (int j = 0; j < o.numObservations(t); j++) {
				allData[i] = usefulObservations[t][j];
				allPassiveData[i] = passiveObservations[t][j];
				i++;
			}

		// stratify data
		if (classAttribute != null) {

			int classRange = o.getAttributes().get(classAttribute).size();

			stratifiedData = new ArrayList<int[][]>(classRange);

			stratifiedPassiveData = new ArrayList<String[][]>(classRange);

			for (int c = 0; c < classRange; c++) {
				stratifiedData.add(new int[countInstancesOfClass(classAttribute, c)][(m + 1) * n + 1]);
				stratifiedPassiveData.add(new String[countInstancesOfClass(classAttribute, c)][(m + 1) * nPassive]);
				int[][] classData = stratifiedData.get(c);
				String[][] classPassiveData = stratifiedPassiveData.get(c);
				i = 0;
				for (int j = 0; j < allData.length; j++) {
					int[] row = allData[j];
					if (row[m * n + classAttribute] == c) {
						classData[i] = Arrays.copyOf(row, (m + 1) * n + 1);
						classPassiveData[i] = Arrays.copyOf(allPassiveData[j], (m + 1) * nPassive);
						i++;
					}
				}

			}
		}

		else {
			stratifiedData = new ArrayList<int[][]>(1);
			stratifiedPassiveData = new ArrayList<String[][]>(1);

			stratifiedData.add(new int[N][(m + 1) * n + 1]);
			int[][] data = stratifiedData.get(0);

			stratifiedPassiveData.add(allPassiveData);

			for (int j = 0; j < allData.length; j++)
				data[j] = Arrays.copyOf(allData[j], (m + 1) * n + 1);
		}

		// determining folds
		this.numFolds = numFolds;
		if (numFolds > 0) {
			List<Integer> foldIds = calculateFoldIds(N, numFolds);

			i = 0;
			for (int[][] classData : stratifiedData)
				for (int j = 0; j < classData.length; j++)
					classData[j][(m + 1) * n] = foldIds.get(i++);
		}

	}

	private Observations evaluateFold(Observations train, Observations test, int numParents, ScoringFunction s,
			boolean dotOutput, String dotFileName, boolean mostProbable) {
		// System.out.println("initializing scores");
		Scores s1 = new Scores(train, numParents, true, true);
		// System.out.println("evaluating scores");
		s1.evaluate(s);

		// System.out.println("converting to DBN");
		DynamicBayesNet dbn1 = s1.toDBN();

		if (dotOutput) {
			try {
				Utils.writeToFile(dotFileName + ".dot", dbn1.toDot(false));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		// System.out.println("learning DBN parameters");
		@SuppressWarnings("unused")
		String params = dbn1.learnParameters(train, true);

		if (dotOutput) {
			// for (Attribute a : train.getAttributes()) {
			// System.out.print(a.getName() + ": ");
			// System.out.println(a);
			// }

			// System.out.println(params);
			return null;
		}

		else {
			// System.out.println("testing network");
			return dbn1.forecast(test, 1, true, mostProbable);
		}

	}

	public String evaluate(int numParents, ScoringFunction s, String outputFileName, List<Integer> forecastAttributes,
			boolean mostProbable) {

		int n = o.numAttributes();
		int nPassive = o.numPassiveAttributes();
		int m = o.getMarkovLag();
		int[][][] trainingData;
		int[][][] testData;
		String[][] testPassiveData;

		System.out.println("Random seed: " + randomSeed);
		System.out.println("Number of observations: " + o.numObservations(-1));

		StringBuilder output = new StringBuilder();
		String ls = System.getProperty("line.separator");

		output.append(randomSeed + ls);
		for (int predictor : forecastAttributes)
			output.append(o.getAttributes().get(predictor).getName() + "\t");
		output.append("\t" + "actual_value" + ls);

		for (int f = 0; f < numFolds; f++) {

			int fold = f + 1;
			System.out.println("Fold " + fold);

			Pair counts = countInstancesOfFold(f);
			int testSize = counts.a;
			int trainingSize = counts.b;

			trainingData = new int[1][trainingSize][(m + 1) * n];
			testData = new int[1][testSize][(m + 1) * n];
			testPassiveData = new String[testSize][(m + 1) * nPassive];

			System.out.println("Training size: " + trainingSize + "\t" + "Test size: " + testSize);

			int i = 0;
			int j = 0;
			for (int c = 0; c < stratifiedData.size(); c++) {
				int[][] classData = stratifiedData.get(c);
				String[][] classPassiveData = stratifiedPassiveData.get(c);
				for (int k = 0; k < classData.length; k++) {
					int[] row = classData[k];
					if (row[(m + 1) * n] == f) {
						testData[0][i] = Arrays.copyOf(row, (m + 1) * n);
						testPassiveData[i] = Arrays.copyOf(classPassiveData[k], (m + 1) * nPassive);
						i++;
					} else
						trainingData[0][j++] = Arrays.copyOf(row, (m + 1) * n);
				}
			}

			Observations train = new Observations(o, trainingData);
			Observations test = new Observations(o, testData);

			Observations forecast = evaluateFold(train, test, numParents, s, false, null, mostProbable);

			// output.append("---Fold-" + fold + "---" + ls);
			for (i = 0; i < testSize; i++) {
				int[][][] fMatrix = forecast.getObservationsMatrix();
				for (int predictor : forecastAttributes)
					output.append(o.getAttributes().get(predictor).get(fMatrix[0][i][m * n + predictor]) + "\t");

				output.append("\t");
				for (j = 0; j < nPassive; j++)
					output.append(testPassiveData[i][m * nPassive + j] + "\t");

				output.append(ls);
			}

		}

		// use all data for training and produce network graph

		System.out.println("---All-data---");
		Observations train = o;
		evaluateFold(train, null, numParents, s, true, outputFileName, mostProbable);
		output.append(ls);

		// output true values for baseline classifier
		for (int i = 0; i < allData.length; i++) {
			for (int j = 0; j < allPassiveData[0].length; j++)
				output.append(allPassiveData[i][j] + "\t");
			output.append(ls);
		}

		return output.toString();

	}

	public static void main(String[] args) {

		int p = 1;
		int m = 1;
		ScoringFunction s = new MDLScoringFunction();
		int folds = 0;
		Integer classAttribute = null;
		List<Integer> forecastAttributes = Arrays.asList(1, 2, 4, 5);

		Observations o = new Observations("trial5-horizontal.csv", "das-horizontal.csv", m);

		CrossValidation cv = new CrossValidation(o, folds, classAttribute);

		System.out.println(cv.evaluate(p, s, "dot", forecastAttributes, true));

	}

}
