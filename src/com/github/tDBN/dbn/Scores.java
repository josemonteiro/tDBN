package com.github.tDBN.dbn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.github.tDBN.utils.Edge;

public class Scores {

	private Observations observations;

	/**
	 * scoresMatrix[t][i][j] is the score of the arc
	 * Xj[t+markovLag]->Xi[t+markovLag].
	 */
	private double[][][] scoresMatrix;

	/**
	 * parentNodes.get(t).get(i) is the list of optimal parents in
	 * {X[t],...,X[t+markovLag-1]} of Xi[t+markovLag] when there is no arc from
	 * X[t+markovLag] to X[t+markovLag].
	 */
	private List<List<List<Integer>>> parentNodesPast;

	/**
	 * parentNodes.get(t).get(i).get(j) is the list of optimal parents in
	 * {X[t],...,X[t+markovLag-1]} of Xi[t+markovLag] when the arc
	 * Xj[t+markovLag]->Xi[t+markovLag] is present.
	 */
	private List<List<List<List<Integer>>>> parentNodes;

	/**
	 * Upper limit on the number of parents from previous time slices.
	 */
	private int maxParents;

	/**
	 * A list of all possible sets of parent nodes. Set cardinality lies within
	 * the range [1, maxParents].
	 */
	private List<List<Integer>> parentSets;

	/**
	 * If true, evaluates only one score matrix for all transitions.
	 */
	private boolean stationaryProcess;

	private boolean evaluated = false;

	public Scores(Observations observations, int maxParents) {
		this(observations, maxParents, false);
	}

	public Scores(Observations observations, int maxParents, boolean stationaryProcess) {
		this.observations = observations;
		this.maxParents = maxParents;
		this.stationaryProcess = stationaryProcess;

		int n = this.observations.numAttributes();
		int p = this.maxParents;
		int markovLag = observations.getMarkovLag();

		// calculate sum_i=1^k nCi
		int size = n * markovLag;
		for (int previous = n, i = 2; i <= p; i++) {
			int current = previous * (n - i + 1) / i;
			size += current;
			previous = current;
		}
		// TODO: check for size overflow

		// generate parents sets
		parentSets = new ArrayList<List<Integer>>(size);
		for (int i = 1; i <= p; i++) {
			generateCombinations(n * markovLag, i);
		}

		int numTransitions = stationaryProcess ? 1 : observations.numTransitions();
		parentNodesPast = new ArrayList<List<List<Integer>>>(numTransitions);
		parentNodes = new ArrayList<List<List<List<Integer>>>>(numTransitions);

		for (int t = 0; t < numTransitions; t++) {

			parentNodesPast.add(new ArrayList<List<Integer>>(n));
			// allocate parentNodesPast
			List<List<Integer>> parentNodesPastTransition = parentNodesPast.get(t);
			for (int i = 0; i < n; i++) {
				parentNodesPastTransition.add(new ArrayList<Integer>());
			}

			parentNodes.add(new ArrayList<List<List<Integer>>>(n));
			// allocate parentNodes
			List<List<List<Integer>>> parentNodesTransition = parentNodes.get(t);
			for (int i = 0; i < n; i++) {
				parentNodesTransition.add(new ArrayList<List<Integer>>(n));
				List<List<Integer>> parentNodesTransitionHead = parentNodesTransition.get(i);
				for (int j = 0; j < n; j++) {
					parentNodesTransitionHead.add(new ArrayList<Integer>());
				}
			}
		}

		// allocate scoresMatrix
		scoresMatrix = new double[numTransitions][n][n];

	}

	public Scores evaluate(ScoringFunction sf) {

		int n = observations.numAttributes();
		int numTransitions = scoresMatrix.length;

		for (int t = 0; t < numTransitions; t++) {
			// System.out.println("evaluating score in transition " + t + "/" +
			// numTransitions);
			for (int i = 0; i < n; i++) {
				// System.out.println("evaluating node " + i + "/" + n);
				double bestScore = Double.NEGATIVE_INFINITY;
				for (List<Integer> parentSet : parentSets) {
					double score = stationaryProcess ? sf.evaluate(observations, parentSet, i) : sf.evaluate(
							observations, t, parentSet, i);
					if (bestScore < score) {
						bestScore = score;
						parentNodesPast.get(t).set(i, parentSet);
					}
				}
				for (int j = 0; j < n; j++) {
					scoresMatrix[t][i][j] = -bestScore;
				}
			}

			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					// int currentEdge = i * n + j;
					// System.out.println("evaluating edge " + currentEdge + "/"
					// + n * n);
					if (i != j) {
						double bestScore = Double.NEGATIVE_INFINITY;
						for (List<Integer> parentSet : parentSets) {
							double score = stationaryProcess ? sf.evaluate(observations, parentSet, j, i) : sf
									.evaluate(observations, t, parentSet, j, i);
							if (bestScore < score) {
								bestScore = score;
								parentNodes.get(t).get(i).set(j, parentSet);
							}
						}

						scoresMatrix[t][i][j] += bestScore;

					}
				}
			}
		}

		evaluated = true;

		return this;

	}

	// adapted from http://stackoverflow.com/a/7631893
	private void generateCombinations(int n, int k) {

		int[] comb = new int[k];
		for (int i = 0; i < comb.length; i++) {
			comb[i] = i;
		}

		boolean done = false;
		while (!done) {

			List<Integer> intList = new ArrayList<Integer>(k);
			for (int i : comb) {
				intList.add(i);
			}
			this.parentSets.add(intList);

			int target = k - 1;
			comb[target]++;
			if (comb[target] > n - 1) {
				// carry the one
				while (comb[target] > ((n - 1 - (k - target)))) {
					target--;
					if (target < 0) {
						break;
					}
				}
				if (target < 0) {
					done = true;
				} else {
					comb[target]++;
					for (int i = target + 1; i < comb.length; i++) {
						comb[i] = comb[i - 1] + 1;
					}
				}
			}
		}
	}

	public double[][] getScoresMatrix(int transition) {
		return scoresMatrix[transition];
	}

	public DynamicBayesNet toDBN() {
		return toDBN(-1);
	}

	public DynamicBayesNet toDBN(int root) {

		if (!evaluated)
			throw new IllegalStateException("Scores must be evaluated before being converted to DBN");

		int n = observations.numAttributes();

		int numTransitions = scoresMatrix.length;

		List<BayesNet> transitionNets = new ArrayList<BayesNet>(numTransitions);

		for (int t = 0; t < numTransitions; t++) {

			List<Edge> intraRelations = OptimumBranching.evaluate(scoresMatrix[t], root);

			List<Edge> interRelations = new ArrayList<Edge>(n * maxParents);

			boolean[] hasParent = new boolean[n];

			for (Edge intra : intraRelations) {
				int tail = intra.getTail();
				int head = intra.getHead();
				List<List<List<Integer>>> parentNodesT = parentNodes.get(t);
				for (Integer nodePast : parentNodesT.get(head).get(tail)) {
					interRelations.add(new Edge(nodePast, head));
					hasParent[head] = true;
				}
			}

			for (int i = 0; i < n; i++)
				if (!hasParent[i]) {
					List<List<Integer>> parentNodesPastT = parentNodesPast.get(t);
					for (int nodePast : parentNodesPastT.get(i))
						interRelations.add(new Edge(nodePast, i));
				}

			BayesNet bt = new BayesNet(observations.getAttributes(), observations.getMarkovLag(), intraRelations,
					interRelations);

			transitionNets.add(bt);
		}

		return new DynamicBayesNet(observations.getAttributes(), transitionNets);

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int n = scoresMatrix[0].length;
		DecimalFormat df = new DecimalFormat("0.00");

		int numTransitions = scoresMatrix.length;

		for (int t = 0; t < numTransitions; t++) {
			sb.append("--- Transition " + t + " ---" + ls);
			sb.append("Maximum number of parents in t: " + maxParents + ls);

			sb.append(ls);

			sb.append("Scores matrix:" + ls);
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					sb.append(df.format(scoresMatrix[t][i][j]) + " ");
				}
				sb.append(ls);
			}

			sb.append(ls);

			sb.append("Parents only in t:" + ls);
			for (int i = 0; i < n; i++) {
				sb.append(i + ": " + parentNodesPast.get(t).get(i) + ls);
			}

			sb.append(ls);

			sb.append("Parents in t for each parent in t+1:" + ls);
			sb.append("t+1:	");
			for (int i = 0; i < n; i++) {
				sb.append(i + "	");
			}
			sb.append(ls);
			for (int i = 0; i < n; i++) {
				sb.append(i + ":	");
				for (int j = 0; j < n; j++) {
					sb.append(parentNodes.get(t).get(i).get(j) + "	");
				}
				sb.append(ls);
			}

			sb.append(ls);
		}

		return sb.toString();
	}

}
