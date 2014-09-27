package com.github.tDBN.dbn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalConfiguration extends Configuration {

	private int[] parentIndices;

	private int childNode;

	/**
	 * If true, considers the child value when matching an observation with the
	 * current configuration. In this case, N_{ijk} is what is being counted.
	 */
	private boolean considerChild = true;

	/**
	 * Allocates the configuration array and sets all parents and the child to
	 * their first value. All input nodes must lie in the range
	 * [0,attributes.size()[.
	 * 
	 * @param attributes
	 *            list of attributes characterizing the nodes
	 * @param parentNodesPast
	 *            list of parent nodes in t
	 * @param parentNodesPresent
	 *            list of parent nodes in t+1
	 * @param childNode
	 *            child node in t+1
	 */
	public LocalConfiguration(List<Attribute> attributes, List<Integer> parentNodesPast,
			List<Integer> parentNodesPresent, int childNode) {
		super(attributes);
		this.reset();

		int n = attributes.size();
		int numParentsPast = (parentNodesPast != null) ? parentNodesPast.size() : 0;
		int numParentsPresent = (parentNodesPresent != null) ? parentNodesPresent.size() : 0;
		int numParents = numParentsPast + numParentsPresent;

		parentIndices = new int[numParents];
		for (int i = 0; i < numParentsPast; i++) {
			parentIndices[i] = parentNodesPast.get(i);
		}
		for (int i = 0; i < numParentsPresent; i++) {
			parentIndices[i + numParentsPast] = parentNodesPresent.get(i) + n;
		}
		resetParents();

		this.childNode = childNode;
		resetChild();
	}

	public LocalConfiguration(List<Attribute> attributes, List<Integer> parentNodesPast, Integer parentNodePresent,
			int childNode) {
		this(attributes, parentNodesPast, (parentNodePresent != null ? Arrays.asList(parentNodePresent) : null),
				childNode);
	}

	public LocalConfiguration(List<Attribute> attributes, List<Integer> parentNodes, int childNode) {
		this(attributes, parentNodes, (List<Integer>) null, childNode);
	}

	/**
	 * Sets whether the child value should be considered when matching an
	 * observation with the current configuration.
	 * 
	 * @param state
	 */
	public void setConsiderChild(boolean state) {
		considerChild = state;
	}

	public boolean matches(int[] observation) {

		int n = attributes.size();

		for (int i = 0; i < 2 * n; i++) {
			if (configuration[i] > -1) {
				if (observation[i] != configuration[i]) {
					if (considerChild || i != childNode + n) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Updates the configuration of parents' values by incrementing the current
	 * configuration in lexicographical order. If there isn't a new
	 * configuration, resets the parents' values and returns false.
	 * 
	 * @return true if a new parents' configuration is generated.
	 */
	public boolean nextParents() {

		int n = attributes.size();

		for (int i = 0; i < parentIndices.length; i++) {
			if (++configuration[parentIndices[i]] < attributes.get(parentIndices[i] % n).size()) {
				break;
			} else {
				configuration[parentIndices[i]] = 0;
				if (i == parentIndices.length - 1) {
					resetParents();
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Sets all parents to their first value.
	 */
	public void resetParents() {
		for (int i = 0; i < parentIndices.length; i++) {
			configuration[parentIndices[i]] = 0;
		}
	}

	/**
	 * Increments the child value maintaining the current configuration of
	 * parents' values. If there are no more child values, resets to the first
	 * value and returns false.
	 * 
	 * @return true if a new child value is generated.
	 */
	public boolean nextChild() {

		int n = attributes.size();

		if (++configuration[n + childNode] < attributes.get(childNode).size()) {
			return true;
		} else {
			resetChild();
			return false;
		}

	}

	/**
	 * Sets the child node to its first value.
	 */
	public void resetChild() {
		int n = attributes.size();
		configuration[n + childNode] = 0;
	}

	public int getParentsRange() {
		if (parentIndices.length == 0) {
			return 0;
		}
		int n = attributes.size();
		int result = 1;
		for (int i = 0; i < parentIndices.length; i++) {
			result *= attributes.get(parentIndices[i] % n).size();
		}
		return result;
	}

	public int getChildRange() {
		return attributes.get(childNode).size();
	}

	/**
	 * Calculates the number of parameters required to specify a distribution,
	 * according to the list of parents and the child.
	 * 
	 * @return the number of parameters to specify
	 */
	public int getNumParameters() {
		return getParentsRange() * (getChildRange() - 1);
	}

	public static void main(String[] args) {

		Observations o = new Observations(args[0]);
		List<Integer> parentNodesPast = new ArrayList<Integer>();
		parentNodesPast.add(1);
		parentNodesPast.add(2);
		Integer parentNodePresent = 3;
		int childNode = 1;
		LocalConfiguration c = new LocalConfiguration(o.getAttributes(), parentNodesPast, parentNodePresent, childNode);

		System.out.println(o);

		int count = 0;

		do {
			do {
				System.out.println(c);
				count++;
			} while (c.nextChild());
		} while (c.nextParents());

		System.out.println(count);
		System.out.println(c.getNumParameters());
	}
}
