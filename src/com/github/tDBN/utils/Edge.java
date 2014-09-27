package com.github.tDBN.utils;

//import java.text.DecimalFormat;

public class Edge implements Comparable<Edge> {
	
	private int head;
	private int tail;
	private double weight;
	private double updatedWeight;
	
	public Edge(int tail, int head, double weight) {
		this.tail = tail;
		this.head = head;
		this.weight = this.updatedWeight = weight;
	}
	
	public Edge(int tail, int head) {
		this(tail, head, 0);
	}

	public int getTail() {
		return this.tail;
	}

	public int getHead() {
		return this.head;
	}
	
	public double getWeight() {
		return this.updatedWeight;
	}
	
	public double getOriginalWeight() {
		return this.weight;
	}
	
	public void setWeight(double weight){
		this.updatedWeight = weight;
	}
	
	public int compareTo(Edge anotherEdge){
		return -1 * Double.compare(this.updatedWeight, anotherEdge.updatedWeight);
	}

	@Override
	public String toString() {
//		DecimalFormat df = new DecimalFormat("0.00"); 
//		return "("+ tail + ", "+ head +") "+ df.format(updatedWeight);
		return "("+ tail + ", "+ head +")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + head;
		result = prime * result + tail;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Edge))
			return false;
		Edge other = (Edge) obj;
		if (head != other.head)
			return false;
		if (tail != other.tail)
			return false;
		return true;
	}

}
