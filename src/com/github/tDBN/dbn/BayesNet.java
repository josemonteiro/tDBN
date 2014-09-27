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

	private List<List<Integer>> parentNodes;

	private List<Edge> intraRelations;
	private List<Edge> interRelations;

	private List<Map<Configuration, List<Double>>> parameters;

	private List<Integer> topologicalOrder;

	/**
	 * maximum number of parents from t to t+1
	 */
	private int maxParents;

	// for random sampling
	private Random r = new Random();

	// prior network
	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, Random r) {
		this(attributes, intraRelations, (List<Edge>) null, r);
	}

	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations) {
		this(attributes, intraRelations, (List<Edge>) null);
	}

	// 2TBN
	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, List<Edge> interRelations, Random r) {
		this(attributes, intraRelations, interRelations);
		this.r = r;
	}

	public BayesNet(List<Attribute> attributes, List<Edge> intraRelations, List<Edge> interRelations) {

		this.attributes = attributes;
		this.intraRelations = intraRelations;
		this.interRelations = interRelations;
		int n = attributes.size();

		// for topological sorting of t+1 slice
		List<List<Integer>> childNodes = new ArrayList<List<Integer>>(n);
		for (int i = n; i-- > 0;) {
			childNodes.add(new ArrayList<Integer>(n));
		}

		parentNodes = new ArrayList<List<Integer>>(n);
		if (interRelations != null) {
			for (int i = n; i-- > 0;) {
				// n+1 instead of 2*n
				parentNodes.add(new ArrayList<Integer>(2 * n));
			}

			// edges crossing consecutive slices (t to t+1)
			for (Edge e : interRelations) {
				// tail refers to t, head to t+1
				List<Integer> headParentNodes = parentNodes.get(e.getHead());
				headParentNodes.add(e.getTail());
				if (maxParents < headParentNodes.size())
					maxParents = headParentNodes.size();
			}
		} else {
			for (int i = n; i-- > 0;) {
				// 1 instead of n
				parentNodes.add(new ArrayList<Integer>(n));
			}
		}

		// edges inside the same slice (t0 or t+1)
		for (Edge e : intraRelations) {
			// add n to tail so that it refers to t+1
			parentNodes.get(e.getHead()).add(e.getTail() + n);
			childNodes.get(e.getTail()).add(e.getHead());
		}

		// sort for when applying configuration mask
		for (int i = n; i-- > 0;) {
			Collections.sort(parentNodes.get(i));
		}

		// obtain nodes by topological order
		topologicalOrder = Utils.topologicalSort(childNodes);
	}

	public void generateParameters() {
		int n = attributes.size();
		parameters = new ArrayList<Map<Configuration, List<Double>>>(n);

		for (int i = 0; i < n; i++) {

			LocalConfiguration c = new LocalConfiguration(attributes, parentNodes.get(i), i);
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

	public void learnParameters(Observations o, int transition) {

		if (o.getAttributes() != this.attributes) {
			throw new IllegalArgumentException("Attributes of the observations don't"
					+ "match the attributes of the BN");
		}

		int n = attributes.size();
		parameters = new ArrayList<Map<Configuration, List<Double>>>(n);

		// for each node, generate its local CPT
		for (int i = 0; i < n; i++) {

			LocalConfiguration c = new LocalConfiguration(attributes, parentNodes.get(i), i);
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
					// count for all except one of possible child values
					for (int j = range - 1; j-- > 0;) {
						int Nijk = o.count(c, transition);
						probabilities.add(1.0 * Nijk / Nij);
						c.nextChild();
					}
					// important, configuration is index by parents only
					// child must be reset
					c.resetChild();
					parameters.get(i).put(new Configuration(c), probabilities);
				} while (c.nextParents());
			}

		}
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

	public int[] nextObservation(int[] previousObservation) {
		MutableConfiguration c = new MutableConfiguration(attributes, previousObservation);
		for (int node : topologicalOrder) {
			Configuration indexParameters = c.applyMask(parentNodes.get(node), node);
			List<Double> probabilities = parameters.get(node).get(indexParameters);

			// random sampling
			double sample = r.nextDouble();

			double accum = probabilities.get(0);
			int value = 0;

			while (sample > accum) {
				if (!(value < probabilities.size() - 1)) {
					++value;
					break;
				}
				accum += probabilities.get(++value);
			}

			c.update(node, value);
		}
		int n = attributes.size();
		return Arrays.copyOfRange(c.toArray(), n, 2 * n);
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
	 * @return [true positive, false positive, true negative, false negative]
	 */
	public static int[] compare(BayesNet original, BayesNet recovered, boolean verbose) {
		// intra edges only, assume graph is a tree

		assert (original.attributes == recovered.attributes);
		int n = original.attributes.size();
		// maxParents
		assert (original.maxParents == recovered.maxParents);

		List<Edge> intraTruePositive = new ArrayList<Edge>(original.intraRelations);
		intraTruePositive.retainAll(recovered.intraRelations);

		List<Edge> interTruePositive = new ArrayList<Edge>(original.interRelations);
		interTruePositive.retainAll(recovered.interRelations);

		int conditionPositive = (original.intraRelations.size() + original.interRelations.size());
		int testPositive = (recovered.intraRelations.size() + recovered.interRelations.size());
		int popTotal = n * (2 * n - 1);

		int truePositive = (intraTruePositive.size() + interTruePositive.size());
		int falsePositive = testPositive - truePositive;
		int trueNegative = popTotal - (conditionPositive + falsePositive);
		int falseNegative = conditionPositive - truePositive;

		// double precision = 1.0*truePositive/testPositive;
		// double recall = 1.0*truePositive/conditionPositive;
		// double accuracy = 1.0*(truePositive+trueNegative)/popTotal;

		if (verbose) {

			System.out.println("----- Intra-relational edges -----");

			System.out.println("Original network (" + original.intraRelations.size() + ")");
			for (Edge e : original.intraRelations)
				System.out.println(e);

			System.out.println("Learnt network (" + recovered.intraRelations.size() + ")");
			for (Edge e : recovered.intraRelations)
				System.out.println(e);

			System.out.println("In common (" + intraTruePositive.size() + ")");
			for (Edge e : intraTruePositive)
				System.out.println(e);

			System.out.println("----- Inter-relational edges -----");

			System.out.println("Original network (" + original.interRelations.size() + ")");
			for (Edge e : original.interRelations)
				System.out.println(e);

			System.out.println("Learnt network (" + recovered.interRelations.size() + ")");
			for (Edge e : recovered.interRelations)
				System.out.println(e);

			System.out.println("In common (" + interTruePositive.size() + ")");
			for (Edge e : interTruePositive)
				System.out.println(e);

			// System.out.println("Precision (tp/(tp+fp)) = "+precision);
			// System.out.println("Recall (tp/(tp+fn)) = "+recall);
			// System.out.println("Accuracy ((tp+tn)/(tp+fn+fp+tn)) = "+accuracy);
			// System.out.println("F1 = "+2*precision*recall/(precision+recall));
		}

		return new int[] { truePositive, falsePositive, trueNegative, falseNegative };

	}

	public List<Edge> getIntraRelations() {
		return intraRelations;
	}

	public List<Edge> getInterRelations() {
		return interRelations;
	}

	public String toString(int t) {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int tPlus1 = t + 1;
		if (interRelations != null)
			for (Edge e : interRelations)
				sb.append(e.getTail() + "[" + t + "] -> " + e.getHead() + "[" + tPlus1 + "]" + ls);
		for (Edge e : intraRelations)
			sb.append(e.getTail() + "[" + tPlus1 + "] -> " + e.getHead() + "[" + tPlus1 + "]" + ls);
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");

		// int i = 0;
		// for (List<Integer> iParents : parentNodes) {
		// sb.append(i + ":\t");
		// for (Integer parent : iParents) {
		// sb.append(parent + "\t");
		// }
		// sb.append(ls);
		// i++;
		// }

		sb.append("intra-slice:" + ls);
		for (Edge e : intraRelations) {
			sb.append(e + ls);
		}
		if (interRelations != null) {
			sb.append(ls);
			sb.append("inter-slice:" + ls);
			for (Edge e : interRelations) {
				sb.append(e + ls);
			}
		}

		return sb.toString();
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
