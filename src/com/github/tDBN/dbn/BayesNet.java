package com.github.tDBN.dbn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.github.tDBN.utils.Edge;
import com.github.tDBN.utils.Utils;

public class BayesNet {

	private List<Attribute> attributes;

	// "processed" (unshifted) relations
	private List<List<List<Integer>>> parentNodesPerSlice;

	// "raw" (shifted) relations
	private List<List<Integer>> parentNodes;

	private List<Map<Configuration, List<Double>>> parameters;

	private List<Integer> topologicalOrder;

	private int markovLag;

	// for random sampling
	private Random r;

	// prior network
	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, Random r) {
		this(attributes, 0, intraRelations, (List<Edge>) null, r);
	}

	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations) {
		this(attributes, 0, intraRelations, (List<Edge>) null, null);
	}

	// transition network, standard Markov lag = 1
	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, List<Edge> interRelations, Random r) {
		this(attributes, 1, intraRelations, interRelations, r);
	}

	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, List<Edge> interRelations) {
		this(attributes, 1, intraRelations, interRelations, null);
	}

	// transition network, arbitrary Markov lag
	public BayesNet(List<Attribute> attributes, int markovLag, List<Edge> intraRelations, List<Edge> interRelations) {
		this(attributes, markovLag, intraRelations, interRelations, null);
	}

	// edge heads are already unshifted (i.e., in the interval [0, n[)
	// interRelations edge tails are shifted in Configuration style (i.e, [0,
	// markovLag*n[)
	// intraRelations edge tails are unshifted
	public BayesNet(List<Attribute> attributes, int markovLag, List<Edge> intraRelations, List<Edge> interRelations,
			Random r) {

		this.attributes = attributes;
		this.markovLag = markovLag;
		int n = attributes.size();

		this.r = (r != null) ? r : new Random();

		// for topological sorting of t+1 slice
		List<List<Integer>> childNodes = new ArrayList<List<Integer>>(n);
		for (int i = n; i-- > 0;) {
			childNodes.add(new ArrayList<Integer>(n));
		}

		parentNodesPerSlice = new ArrayList<List<List<Integer>>>(markovLag + 1);
		for (int slice = 0; slice < markovLag + 1; slice++) {
			parentNodesPerSlice.add(new ArrayList<List<Integer>>(n));
			for (int i = 0; i < n; i++) {
				parentNodesPerSlice.get(slice).add(new ArrayList<Integer>());
			}
		}

		parentNodes = new ArrayList<List<Integer>>(n);
		for (int i = 0; i < n; i++)
			parentNodes.add(new ArrayList<Integer>());

		if (interRelations != null) {
			for (Edge e : interRelations) {
				// tail is shifted and refers to a previous slice
				int tail = e.getTail();
				int slice = tail / n;
				int unshiftedTail = tail % n;
				// head refers to the foremost slice
				int head = e.getHead();

				parentNodesPerSlice.get(slice).get(head).add(unshiftedTail);
				parentNodes.get(head).add(tail);
			}
		}

		// edges inside the same slice
		for (Edge e : intraRelations) {
			// tail is unshifted
			int tail = e.getTail();
			int shiftedTail = tail + n * markovLag;
			int head = e.getHead();

			parentNodesPerSlice.get(markovLag).get(head).add(tail);
			parentNodes.get(head).add(shiftedTail);
			childNodes.get(tail).add(head);
		}

		// sort for when applying configuration mask
		for (int i = n; i-- > 0;)
			Collections.sort(parentNodes.get(i));

		// obtain nodes by topological order
		topologicalOrder = Utils.topologicalSort(childNodes);
	}

	public void generateParameters() {
		int n = attributes.size();
		parameters = new ArrayList<Map<Configuration, List<Double>>>(n);

		for (int i = 0; i < n; i++) {

			LocalConfiguration c = new LocalConfiguration(attributes, markovLag, parentNodes.get(i), i);
			int parentsRange = c.getParentsRange();
			if (parentsRange == 0) {
				parameters.add(new HashMap<Configuration, List<Double>>(2));
				int range = c.getChildRange();
				parameters.get(i).put(new Configuration(c), generateProbabilities(range));

			} else {
				parameters.add(new HashMap<Configuration, List<Double>>((int) Math.ceil(parentsRange / 0.75)));

				do {
					int range = c.getChildRange();
					parameters.get(i).put(new Configuration(c), generateProbabilities(range));
				} while (c.nextParents());
			}

		}
	}

	/**
	 * Learn from all transitions (stationary process).
	 */
	public void learnParameters(Observations o) {
		learnParameters(o, -1);
	}

	public String learnParameters(Observations o, int transition) {

		if (o.getAttributes() != this.attributes) {
			throw new IllegalArgumentException("Attributes of the observations don't"
					+ "match the attributes of the BN");
		}

		int n = attributes.size();
		parameters = new ArrayList<Map<Configuration, List<Double>>>(n);

		// for each node, generate its local CPT
		for (int i = 0; i < n; i++) {

			LocalConfiguration c = new LocalConfiguration(attributes, markovLag, parentNodes.get(i), i);
			int parentsRange = c.getParentsRange();

			// node i has no parents
			if (parentsRange == 0) {
				parameters.add(new HashMap<Configuration, List<Double>>(2));
				// specify its priors
				int range = c.getChildRange();
				List<Double> probabilities = new ArrayList<Double>(range - 1);
				// count for all except one of possible child values
				for (int j = range - 1; j-- > 0;) {
					int Nijk = o.count(c, transition);
					probabilities.add(1.0 * Nijk / o.numObservations(transition));
					c.nextChild();
				}
				// important, configuration is indexed by parents only
				// child must be reset
				c.resetChild();
				parameters.get(i).put(new Configuration(c), probabilities);

			} else {
				parameters.add(new HashMap<Configuration, List<Double>>((int) Math.ceil(parentsRange / 0.75)));

				do {
					c.setConsiderChild(false);
					int Nij = o.count(c, transition);
					c.setConsiderChild(true);

					int range = c.getChildRange();
					List<Double> probabilities = new ArrayList<Double>(range - 1);

					// no data found for given configuration
					if (Nij == 0) {
						for (int j = range - 1; j-- > 0;)
							// assume uniform distribution
							probabilities.add(1.0 / range);
					} else {
						// count for all except one of possible child values
						for (int j = range - 1; j-- > 0;) {
							int Nijk = o.count(c, transition);
							probabilities.add(1.0 * Nijk / Nij);
							c.nextChild();
						}
					}
					// important, configuration is index by parents only
					// child must be reset
					c.resetChild();
					parameters.get(i).put(new Configuration(c), probabilities);
				} while (c.nextParents());
			}

		}

		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		for (Map<Configuration, List<Double>> cpt : parameters)
			sb.append(Arrays.toString(cpt.entrySet().toArray()) + ls);

		return sb.toString();

	}

	private List<Double> generateProbabilities(int numValues) {
		List<Double> values = new ArrayList<Double>(numValues);
		List<Double> probabilities;

		// uniform sampling from [0,1[, more info at
		// http://cs.stackexchange.com/questions/3227/uniform-sampling-from-a-simplex
		// http://www.cs.cmu.edu/~nasmith/papers/smith+tromble.tr04.pdf
		// generate n-1 random values in [0,1[, sort them
		// use the distances between adjacent values as probabilities
		if (numValues > 2) {
			values.add(0.0);
			for (int j = numValues - 1; j-- > 0;) {
				values.add(r.nextDouble());
			}

			Collections.sort(values);

			probabilities = new ArrayList<Double>(numValues - 1);
			for (int j = 0; j < numValues - 1; j++) {
				probabilities.add(values.get(j + 1) - values.get(j));
			}
		} else {
			probabilities = Arrays.asList(r.nextDouble());
		}
		return probabilities;
	}

	public int[] nextObservation(int[] previousObservation, boolean mostProbable) {
		MutableConfiguration c = new MutableConfiguration(attributes, markovLag, previousObservation);
		for (int node : topologicalOrder) {
			Configuration indexParameters = c.applyMask(parentNodes.get(node), node);
			List<Double> probabilities = parameters.get(node).get(indexParameters);

			int size = probabilities.size();
			int value;

			if (mostProbable) {
				int maxIndex = -1;
				double max = 0;
				double sum = 0;
				for (int i = 0; i < size; i++) {
					double p = probabilities.get(i);
					sum += p;
					if (max < p) {
						max = p;
						maxIndex = i;
					}
				}
				if (max < 1 - sum)
					maxIndex = size;
				value = maxIndex;
			}

			// random sampling
			else {
				double sample = r.nextDouble();

				double accum = probabilities.get(0);
				value = 0;

				while (sample > accum) {
					if (!(value < size - 1)) {
						++value;
						break;
					}
					accum += probabilities.get(++value);
				}
			}

			c.update(node, value);
		}
		int n = attributes.size();
		return Arrays.copyOfRange(c.toArray(), markovLag * n, (markovLag + 1) * n);
	}

	public static int[] compare(BayesNet original, BayesNet recovered) {
		return compare(original, recovered, false);
	}

	/**
	 * Compares a network that was learned from observations (recovered) with
	 * the original network used to generate those observations. Returns counts
	 * of edges according to a binary classification scheme ((not) present in
	 * input vs. (not) present in output).
	 * 
	 * @param original
	 * @param recovered
	 * @param verbose
	 *            if set, prints net comparison
	 * @return [true positive, conditionPositive, testPositive]
	 */
	public static int[] compare(BayesNet original, BayesNet recovered, boolean verbose) {
		// intra edges only, assume graph is a tree

		assert (original.attributes == recovered.attributes);
		int n = original.attributes.size();
		// maxParents
		// assert (original.maxParents == recovered.maxParents);

		List<List<Integer>> parentNodesTruePositive = new ArrayList<List<Integer>>(n);
		for (int i = 0; i < n; i++) {
			parentNodesTruePositive.add(new ArrayList<Integer>(original.parentNodes.get(i)));
			parentNodesTruePositive.get(i).retainAll(recovered.parentNodes.get(i));
		}

		int truePositive = 0;
		int conditionPositive = 0;
		int testPositive = 0;
		for (int i = 0; i < n; i++) {
			truePositive += parentNodesTruePositive.get(i).size();
			conditionPositive += original.parentNodes.get(i).size();
			testPositive += recovered.parentNodes.get(i).size();
		}

		double precision = 1.0 * truePositive / testPositive;
		double recall = 1.0 * truePositive / conditionPositive;
		double f1 = 2 * precision * recall / (precision + recall);

		if (verbose) {

			System.out.println("Original network (" + conditionPositive + ")");
			for (int i = 0; i < n; i++) {
				System.out.print(i + ": ");
				System.out.println(original.parentNodes.get(i));
			}

			System.out.println("Learnt network (" + testPositive + ")");
			for (int i = 0; i < n; i++) {
				System.out.print(i + ": ");
				System.out.println(recovered.parentNodes.get(i));
			}

			System.out.println("In common (" + truePositive + ")");
			for (int i = 0; i < n; i++) {
				System.out.print(i + ": ");
				System.out.println(parentNodesTruePositive.get(i));
			}

			System.out.println("Precision = " + precision);
			System.out.println("Recall  = " + recall);
			System.out.println("F1 = " + f1);
		}

		return new int[] { truePositive, conditionPositive, testPositive };

	}

	public int getMarkovLag() {
		return markovLag;
	}

	public String toString(int t, boolean compactFormat) {

		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");

		int n = attributes.size();
		int presentSlice = t + markovLag;

		if (compactFormat)
			for (int head = 0; head < n; head++)
				for (Integer tail : parentNodesPerSlice.get(0).get(head))
					sb.append("X" + tail + " -> " + "X" + head + ls);

		else {
			for (int ts = 0; ts < markovLag + 1; ts++) {
				List<List<Integer>> parentNodesOneSlice = parentNodesPerSlice.get(ts);
				int slice = t + ts;
				for (int head = 0; head < n; head++)
					for (Integer tail : parentNodesOneSlice.get(head))
						sb.append("X" + tail + "_" + slice + " -> " + "X" + head + "_" + presentSlice + ls);
				sb.append(ls);
			}
		}

		return sb.toString();
	}

	public String toString() {
		return toString(0, false);
	}

	public static void main(String[] args) {

		int range = 5;
		List<Double> values = new ArrayList<Double>(range);
		List<Double> probabilities;

		// generating a random probabilities vector

		Random r1 = new Random();

		if (range > 2) {
			values.add(0.0);
			for (int j = range - 1; j-- > 0;) {
				values.add(r1.nextDouble());
			}

			Collections.sort(values);

			probabilities = new ArrayList<Double>(range - 1);
			for (int j = 0; j < range - 1; j++) {
				probabilities.add(values.get(j + 1) - values.get(j));
			}
		} else {
			probabilities = Arrays.asList(r1.nextDouble());
		}

		System.out.println(probabilities);

		double sum = 0;
		for (int i = probabilities.size(); i-- > 0;)
			sum += probabilities.get(i);
		System.out.println(sum);

		int size = probabilities.size();
		int maxIndex = -1;
		double max = 0;
		sum = 0;
		for (int i = 0; i < size; i++) {
			double p = probabilities.get(i);
			sum += p;
			if (max < p) {
				max = p;
				maxIndex = i;
			}
		}

		if (max < 1 - sum)
			maxIndex = size;

		System.out.println("Most probable: " + maxIndex);

		// sample from given probability vector

		int[] count = new int[5];
		List<Double> prob = Arrays.asList(0.1, 0.2, 0.4, 0.2);

		Random r2 = new Random();

		for (int i = 1000000; i-- > 0;) {

			double sample = r2.nextDouble();

			double accum = prob.get(0);
			int value = 0;

			while (sample > accum) {
				if (!(value < prob.size() - 1)) {
					++value;
					break;
				}
				accum += prob.get(++value);
			}

			count[value]++;
		}

		System.out.println(Arrays.toString(count));

	}

}
