package com.github.tDBN.dbn;

import java.util.List;

public class MutableConfiguration extends Configuration {
	
	public MutableConfiguration(List<Attribute> attributes, int[] observation){
		super(attributes);
		if (observation != null){
			int n = attributes.size();
			assert (observation.length == n);
			for (int i=0; i<n; i++){
				configuration[i] = observation[i];
				configuration[i+n] = -1;
			}
		}
		else{
			this.reset();
		}
	}
	
	/**
	 * 
	 * @param parentNodes must be sorted
	 * @param childNode must be in range [0,attributes.size()[
	 */
	public Configuration applyMask(List<Integer> parentNodes, int childNode){
		int n = attributes.size();
		int newConfiguration[] = new int[2*n];
		
		int numParents = parentNodes.size();
		int currentParent = 0;
		
		for (int i=0; i<2*n; i++){
			if (i == childNode+n){
				newConfiguration[i]=0;
			}
			else if (currentParent < numParents && i == parentNodes.get(currentParent)){
				newConfiguration[i]=configuration[i];
				currentParent++;
			}
			else{
				newConfiguration[i]=-1;
			}
		}
		
		return new Configuration(attributes, newConfiguration);
	}
	
	public void update(int node, int value){
		//TODO: validate node and value bounds
		int n = attributes.size();
		configuration[node+n] = value;
	}
}
