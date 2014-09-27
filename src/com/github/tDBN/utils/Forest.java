package com.github.tDBN.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Forest<T> {
	
	private Map<T,TreeNode<T>> roots = new HashMap<T,TreeNode<T>>();
	
	public TreeNode<T> add(T nodeData, List<T> childrenData){
		TreeNode<T> node = new TreeNode<T>(nodeData);
		for (T childData : childrenData){
			//check if child is already in the forest as root
			if (roots.containsKey(childData)){
				TreeNode<T> childNode = roots.remove(childData);
				node.addChild(childNode);
			}
			else{
				node.addChild(childData);
			}			
		}
		roots.put(nodeData, node);
		return node;
	}
	
	public TreeNode<T> getRoot(){
		return roots.values().iterator().next();
	}
	
//	/**
//	 * Deletes the nodes belonging to the path between source and target.
//	 * Source must be a root of the forest and target must be a leaf and
//	 * a descendant from source. All orphaned children (descendants of path
//	 * nodes) become roots of the forest.
//	 * @param source Root source node.
//	 * @param target Leaf target node.
//	 */
//	public void deletePath(TreeNode<T> source, TreeNode<T> target){
//		
//		T sourceData = source.getData();
//		TreeNode<T> sourceRoot = roots.remove(sourceData);
//		assert source == sourceRoot;
//		
//		try{
//			List<TreeNode<T>> newRoots = sourceRoot.deleteDown(target);
//			if (newRoots == null){
//				// path not found, puts the root back
//				roots.put(sourceData, sourceRoot);
//				throw new IllegalArgumentException("There is no path between source and target.");
//			}
//			else{
//				for (TreeNode<T> newRoot : newRoots){
//					roots.put(newRoot.getData(), newRoot);
//				}
//			}
//		}
//		catch (IllegalArgumentException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}		
//	}
	
	/**
	 * Deletes the nodes belonging to the path between leaf and up to the root.
	 * All orphaned children (descendants of path nodes) become roots of the forest.
	 * @param leaf node with no children 
	 */
	public void deleteUp(TreeNode<T> leaf){
		List<TreeNode<T>> newRoots = null;
//		System.out.println("deleting up "+leaf.getData());
		try{
			newRoots = leaf.deleteUp(roots);
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		}
		for (TreeNode<T> newRoot : newRoots){
//			System.out.println("putting new root "+newRoot.getData());			
			roots.put(newRoot.getData(), newRoot);
		}
	}
	
	public boolean isEmpty() {
		return roots.isEmpty();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		sb.append("Forest contains "+roots.size()+" trees."+ls);
		for(TreeNode<T> treeRoot : roots.values()){
			sb.append(treeRoot+ls);
		}
		return sb.toString();
	}
	
	public static void main(String[] args){
		Forest<Integer> f = new Forest<Integer>();
		
		TreeNode<Integer> target = f.add(1, Collections.<Integer>emptyList());
		f.add(0, Arrays.asList(1, 5, 6));
		f.add(7, Arrays.asList(8, 9));
		f.add(10, Arrays.asList(0, 7));
		f.add(100, Arrays.asList(101, 102));
		
		System.out.println(f);
		
		System.out.println("Removing path from "+target.getData()+" up to the root");
		
		f.deleteUp(target);
		
		System.out.println(f);
		
	}

}
