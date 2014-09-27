package com.github.tDBN.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TreeNode<T> {
	
	private T data;
	
	private List<TreeNode<T>> children;
	
	private TreeNode<T> parent;
	
	public TreeNode(T data) {
		this.data = data;
		this.children = new LinkedList<TreeNode<T>>();
	}

	public TreeNode<T> addChild(T childData) {
		TreeNode<T> childNode = new TreeNode<T>(childData);
		childNode.parent = this;
		this.children.add(childNode);
		return childNode;
	}
	
	public void addChild(TreeNode<T> childNode) {
		childNode.parent = this;
		this.children.add(childNode);
	}

	
//	/* target must be a leaf */
//	public List<TreeNode<T>> deleteDown(TreeNode<T> target) throws IllegalArgumentException {
//		if (this == target){
//			//assert this.isLeaf();
//			if (!this.isLeaf()){
//				throw new IllegalArgumentException("Target is not a leaf node.");
//			}
//			return new LinkedList<TreeNode<T>>();
//		}
//		
//		List<TreeNode<T>> orphanChildren = null;
//		for (Iterator<TreeNode<T>> iter = this.children.iterator(); iter.hasNext(); ) {
//			TreeNode<T> childNode = iter.next();
//			orphanChildren = childNode.deleteDown(target);
//			if (orphanChildren != null){
//				iter.remove();
//				break;
//			}
//		}
//		
//		if (orphanChildren == null){
//			return null;
//		}
//		
//		for (TreeNode<T> childNode : this.children){
//			childNode.parent = null;
//			orphanChildren.add(childNode);
//		}
//		
//		return orphanChildren;
//	}
	
	/* target must be a leaf */
	public List<TreeNode<T>> deleteUp(Map<T,TreeNode<T>> roots)
			throws IllegalArgumentException {
		
		if (!this.isLeaf()){
			throw new IllegalArgumentException(this+" is not a leaf node.");
		}
		List<TreeNode<T>> orphanChildren = new LinkedList<TreeNode<T>>();
		TreeNode<T> parent = this.parent;
		TreeNode<T> child = this;
		while (parent != null){
			for (Iterator<TreeNode<T>> iter = parent.children.iterator(); iter.hasNext(); ) {
				TreeNode<T> otherChild = iter.next();
				if (otherChild != child){
					otherChild.parent = null;
					orphanChildren.add(otherChild);
				}
			}
			child = parent;
			parent = parent.parent;
		}
		roots.remove(child.getData());
		return orphanChildren;
	}
	
	public T getData(){
		return this.data;
	}
	
	public List<TreeNode<T>> getChildren() {
		return children;
	}
	
	public boolean isRoot() {
		return parent == null;
	}

	public boolean isLeaf() {
		return children.size() == 0;
	}
	
	public int getLevel() {
		return this.isRoot()? 0 : parent.getLevel() + 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int level = getLevel();
		
		for(int i=level; i-->0;){
			sb.append("	");
		}
		sb.append("-- "+getData()+ls);
		
		for (TreeNode<T> childNode : this.children){
			sb.append(childNode);
		}
		
		return sb.toString();
	}

}
