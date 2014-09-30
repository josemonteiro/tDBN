package com.github.tDBN.dbn;

import java.util.List;

/**
 * Provides network configurations that can be changed, for assisting the
 * process of sampling new observations.
 * 
 * @see BayesNet#nextObservation(int[])
 * 
 * @author zlm
 * 
 */
public class MutableConfiguration extends Configuration {

	public MutableConfiguration(List<Attribute> attributes, int markovLag, int[] extendedObservation) {
		super(attributes, markovLag);
		this.reset();
		if (extendedObservation != null)
			System.arraycopy(extendedObservation, 0, configuration, 0, extendedObservation.length);
	}

	/**
	 * 
	 * @param parentNodes
	 *            must be sorted
	 * @param childNode
	 *            must be in range [0,attributes.size()[
	 */
	public Configuration applyMask(List<Integer> parentNodes, int childNode) {
		int n = attributes.size();
		int size = configuration.length;
		int newConfiguration[] = new int[size];

		int numParents = parentNodes.size();
		int currentParent = 0;

		for (int i = 0; i < size; i++) {
			if (i == childNode + n * markovLag) {
				newConfiguration[i] = 0;
			} else if (currentParent < numParents && i == parentNodes.get(currentParent)) {
				newConfiguration[i] = configuration[i];
				currentParent++;
			} else {
				newConfiguration[i] = -1;
			}
		}

		return new Configuration(attributes, newConfiguration);
	}

	public void update(int node, int value) {
		// TODO: validate node and value bounds
		int n = attributes.size();
		configuration[node + n * markovLag] = value;
	}
}
