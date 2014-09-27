package com.github.tDBN.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Utils {

	public static List<String> readFile(String pathName) throws IOException {

		File file = new File(pathName);
		List<String> fileContents = new ArrayList<String>();
		Scanner scanner = new Scanner(file);

		try {
			while (scanner.hasNextLine()) {
				fileContents.add(scanner.nextLine());
			}
		} finally {
			scanner.close();
		}
		return fileContents;
	}

	public static void writeToFile(String fileName, String contents) throws FileNotFoundException {
		File f = new File(fileName);
		if (f.isFile()) {
			System.err.println("Warning: overwriting to " + fileName + ".");
		}
		PrintWriter w = new PrintWriter(f);
		w.println(contents);
		w.close();

	}

	public static boolean isNumeric(String str) {
		// TODO: change to non-exception method
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	private static void dfs(List<List<Integer>> graph, boolean[] used, List<Integer> res, int u) {
		used[u] = true;
		for (int v : graph.get(u))
			if (!used[v])
				dfs(graph, used, res, v);
		res.add(u);
	}

	// adapted from
	// https://sites.google.com/site/indy256/algo/topological_sorting
	public static List<Integer> topologicalSort(List<List<Integer>> graph) {
		int n = graph.size();
		boolean[] used = new boolean[n];
		List<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < n; i++)
			if (!used[i])
				dfs(graph, used, res, i);
		Collections.reverse(res);
		return res;
	}

	public static void main(String[] args) throws IOException {

		int n = 8;
		List<List<Integer>> g = new ArrayList<List<Integer>>(n);
		for (int i = 0; i < n; i++) {
			g.add(new ArrayList<Integer>());
		}
		g.get(2).add(0);
		g.get(2).add(1);
		g.get(0).add(1);
		g.get(3).add(1);
		g.get(3).add(2);
		g.get(1).add(4);
		g.get(5).add(6);

		List<Integer> res = topologicalSort(g);
		System.out.println(res);

	}
}
