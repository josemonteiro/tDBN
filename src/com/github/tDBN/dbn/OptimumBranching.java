package com.github.tDBN.dbn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.github.tDBN.utils.DisjointSets;
import com.github.tDBN.utils.Edge;
import com.github.tDBN.utils.Forest;
import com.github.tDBN.utils.TreeNode;

public class OptimumBranching {

	public static List<Edge> evaluate(double[][] scoresMatrix) {
		return evaluate(scoresMatrix, -1);
	}

	public static List<Edge> evaluate(double[][] scoresMatrix, int finalRoot) {

		// INIT phase

		int n = scoresMatrix.length;

		// set of strongly-connected graph components
		DisjointSets scc = new DisjointSets(n);

		// set of weakly-connected graph components
		DisjointSets wcc = new DisjointSets(n);

		// maintains track of edges hierarchy to build final tree
		Forest<Edge> forest = new Forest<Edge>();

		List<List<Edge>> incidentEdges = new ArrayList<List<Edge>>(n);

		List<List<Edge>> cycleEdges = new ArrayList<List<Edge>>(n);

		List<Edge> enteringEdge = new ArrayList<Edge>(n);

		List<TreeNode<Edge>> forestLeaf = new ArrayList<TreeNode<Edge>>(n);

		int[] min = new int[n];

		List<Edge> branchingEdges = new LinkedList<Edge>();

		Deque<Integer> vertices = new ArrayDeque<Integer>(n);

		// stupid initialization
		int root = finalRoot;

		for (int i = 0; i < n; i++) {

			incidentEdges.add(new LinkedList<Edge>());

			cycleEdges.add(new ArrayList<Edge>(n));

			enteringEdge.add(null);
			forestLeaf.add(null);

			// initial root of the strongly connected component of i
			min[i] = i;

			vertices.add(i);
		}

		// remove supplied final root node
		vertices.remove(finalRoot);

		// fill incident edges, already sorted by source
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				// skip self edges
				if (i != j) {
					incidentEdges.get(i).add(new Edge(j, i, scoresMatrix[i][j]));
				}
			}
		}

		// BRANCH phase

		while (!vertices.isEmpty()) {

			int r = vertices.pop();
			List<Edge> inEdges = incidentEdges.get(r);

			// input graph assumed strongly connected
			// if there is no edge incident on r, then r is a super-node
			// containing all vertices
			if (inEdges.isEmpty()) {
				// root of the final MWDST
				root = min[r];
			}

			else {

				// get heaviest edge (i,j) incident on r
				int maxIndex = 0;
				for (int i = 1; i < inEdges.size(); i++)
					if (inEdges.get(i).getWeight() > inEdges.get(maxIndex).getWeight())
						maxIndex = i;
				// edge is deleted from I[r]
				Edge heaviest = inEdges.remove(maxIndex);

				int i = heaviest.getTail();
				int j = heaviest.getHead();
				int iWeakComponentRoot = wcc.find(i);
				int jWeakComponentRoot = wcc.find(j);

				// add heaviest edge to forest of edges
				TreeNode<Edge> tn = forest.add(heaviest, cycleEdges.get(r));
				if (cycleEdges.get(r).isEmpty()) {
					forestLeaf.set(j, tn); // points leaf edge in F
				}

				// no cycle is created by heaviest edge
				if (iWeakComponentRoot != jWeakComponentRoot) {
					// join i and j in the same weakly-connected set
					wcc.union(iWeakComponentRoot, jWeakComponentRoot);
					// heaviest is the only chosen edge incident on r
					enteringEdge.set(r, heaviest);
				}

				// heaviest edge introduces a cycle
				else {
					// reset cycle edges
					cycleEdges.get(r).clear();

					Edge lightest = heaviest;
					// find cycle edges and obtain the lightest one
					for (Edge cycleEdge = heaviest; cycleEdge != null; cycleEdge = enteringEdge.get(scc.find(cycleEdge
							.getTail()))) {

						if (cycleEdge.getWeight() < lightest.getWeight())
							lightest = cycleEdge;

						// add (x,y) to the list of cycle edges
						cycleEdges.get(r).add(cycleEdge);
					}

					// update incident edges on r
					for (Edge e : inEdges) {
						e.setWeight(e.getWeight() + lightest.getWeight() - heaviest.getWeight());
					}

					// keep track of root for the spanning tree
					min[r] = min[scc.find(lightest.getHead())];

					// loop over cycle edges excluding heaviest
					for (Edge cycleEdge = enteringEdge.get(scc.find(i)); cycleEdge != null; cycleEdge = enteringEdge
							.get(scc.find(cycleEdge.getTail()))) {

						int headStrongComponentRoot = scc.find(cycleEdge.getHead());

						// update incident edges on other nodes of the cycle
						for (Edge e : incidentEdges.get(headStrongComponentRoot)) {
							e.setWeight(e.getWeight() + lightest.getWeight() - cycleEdge.getWeight());
						}

						// join vertices of the cycle into one scc
						scc.union(r, headStrongComponentRoot);

						// join incident edges lists;
						incidentEdges.set(r,
								merge(incidentEdges.get(r), incidentEdges.get(headStrongComponentRoot), scc, r));
					}

					vertices.push(r);
				}
			}
		}

		// LEAF phase

		// System.out.println(forest);

		TreeNode<Edge> rootLeaf = forestLeaf.get(root);
		if (rootLeaf != null) {
			forest.deleteUp(rootLeaf);
		}

		while (!forest.isEmpty()) {
			TreeNode<Edge> forestRoot = forest.getRoot();
			Edge e = forestRoot.getData();
			branchingEdges.add(e);
			TreeNode<Edge> forestRootLeaf = forestLeaf.get(e.getHead());
			forest.deleteUp(forestRootLeaf);
		}

		return branchingEdges;
	}

	/**
	 * Merges two sorted list of edges, eliminating those that are inside the
	 * strongly-connected component passed as argument. Incoming lists must be
	 * sorted by tail/source. If there is more than one edge with the same
	 * source, keeps only the heaviest.
	 * 
	 * @param l1
	 *            first sorted list
	 * @param l2
	 *            second sorted list
	 * @param scc
	 *            strongly-connect components
	 * @param component
	 *            id of the relevant component
	 * @return merged list
	 */
	private static List<Edge> merge(List<Edge> l1, List<Edge> l2, DisjointSets scc, int component) {
		List<Edge> merged = new ArrayList<Edge>(l1.size() + l2.size());
		ListIterator<Edge> i1 = l1.listIterator();
		ListIterator<Edge> i2 = l2.listIterator();

		while (i1.hasNext() && i2.hasNext()) {
			// skip edges inside the strongly-connected component
			while (i1.hasNext()) {
				if (scc.find(i1.next().getTail()) != component) {
					i1.previous();
					break;
				}
			}
			while (i2.hasNext()) {
				if (scc.find(i2.next().getTail()) != component) {
					i2.previous();
					break;
				}
			}

			if (!i1.hasNext() && !i2.hasNext())
				break;

			if (!i1.hasNext())
				merged.add(i2.next());

			else if (!i2.hasNext())
				merged.add(i1.next());

			// i1.hasNext() && i2.hasNext()
			else {
				Edge e1 = i1.next();
				Edge e2 = i2.next();

				if (e1.getTail() < e2.getTail()) {
					merged.add(e1);
					i2.previous();
				}

				else if (e1.getTail() > e2.getTail()) {
					merged.add(e2);
					i1.previous();
				}

				// if both have the same source, keep the heaviest
				else {
					if (e1.getWeight() > e2.getWeight())
						merged.add(e1);
					else
						merged.add(e2);
				}
			}
		}
		return merged;
	}

	public static void main(String[] args) {

		double matrix[][] = { { 0, 0, 100, 0, 0 }, { 0, 0, 0, 0, 99 }, { 0, 0, 0, 0, 34 }, { 0, 42, 0, 0, 0 },
				{ 0, 0, 82, 87, 0 } };

		List<Edge> branchingEdges = OptimumBranching.evaluate(matrix);

		for (Edge e : branchingEdges) {
			System.out.println(e);
		}
	}
}
