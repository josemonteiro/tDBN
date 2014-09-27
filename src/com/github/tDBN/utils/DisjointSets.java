package com.github.tDBN.utils;

/**
 * A disjoint sets ADT implemented with a Union-Find data structure. Performs
 * union-by-rank and path compression. Implemented using arrays.
 * 
 * Elements are represented by ints, numbered from zero.
 * 
 * Each disjoint set has one element designated as its root. Negative values
 * indicate the element is the root of a set. The absolute value of a negative
 * value is the number of elements in the set. Positive values are an index to
 * where the root was last known to be. If the set has been unioned with
 * another, the last known root will point to a more recent root.
 * 
 * @author Mark Allen Weiss revised 7/21/00 by Matt Fleming
 */

public class DisjointSets {
	private int[] array;

	/**
	 * Constructs a disjoint sets object.
	 * 
	 * @param numElements
	 *            the initial number of elements and also the initial number of
	 *            disjoint sets, since every element is initially in its own
	 *            set.
	 */
	public DisjointSets(int numElements) {
		array = new int[numElements];
		for (int i = 0; i < array.length; i++) {
			array[i] = -1;
		}
	}

	// /**
	// * Unites two disjoint sets into a single set. A union-by-rank
	// * heuristic is used to choose the new root.
	// *
	// * @param a the root element of the first set.
	// * @param b the root element of the second set.
	// */
	// public void union(int root1, int root2) {
	// if (array[root2] < array[root1]) // root2 is deeper
	// array[root1] = root2; // make root2 new root
	// else {
	// if (array[root1] == array[root2])
	// array[root1]--; // update height if same
	// array[root2] = root1; // make root1 new root
	// }
	// }

	/**
	 * Unites two disjoint sets into a single set, maintaining the root of the
	 * first set.
	 * 
	 * @param root1
	 * @param root2
	 */
	public void union(int root1, int root2) {
		array[root2] = root1;
	}

	/**
	 * Finds the (int) name of the set containing a given element. Performs path
	 * compression along the way.
	 * 
	 * @param x
	 *            the element sought.
	 * @return the set containing x.
	 */
	public int find(int x) {
		if (array[x] < 0) {
			return x; // x is the root of the tree; return it
		} else {
			// Find out who the root is; compress path by making the root
			// x's parent.
			array[x] = find(array[x]);
			return array[x]; // Return the root
		}
	}

	// Test main; all finds on same output line should be identical
	public static void main(String[] args) {
		int NumElements = 128;
		int NumInSameSet = 16;

		DisjointSets ds = new DisjointSets(NumElements);
		int set1, set2;

		for (int k = 1; k < NumInSameSet; k *= 2) {
			for (int j = 0; j + k < NumElements; j += 2 * k) {
				set1 = ds.find(j);
				set2 = ds.find(j + k);
				ds.union(set1, set2);
			}
		}

		for (int i = 0; i < NumElements; i++) {
			System.out.print(ds.find(i) + "*");
			if (i % NumInSameSet == NumInSameSet - 1)
				System.out.println();
		}
		System.out.println();
	}
}
