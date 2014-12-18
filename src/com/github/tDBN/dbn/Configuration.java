package com.github.tDBN.dbn;

import java.util.Arrays;
import java.util.List;

public class Configuration {

	protected List<Attribute> attributes;

	protected int[] configuration;

	protected int childNode;

	/* markovLag = configuration.length/n -1 */
	protected int markovLag;

	public Configuration(Configuration c) {
		this.attributes = c.attributes;
		this.configuration = c.configuration.clone();
		this.markovLag = configuration.length / attributes.size() - 1;
		this.childNode = c.childNode;
	}

	protected Configuration(List<Attribute> attributes, int markovLag) {
		this.attributes = attributes;
		this.markovLag = markovLag;
		this.configuration = new int[(markovLag + 1) * attributes.size()];
	}

	protected Configuration(List<Attribute> attributes, int[] configuration) {
		this.attributes = attributes;
		this.configuration = configuration;
	}

	protected void reset() {
		Arrays.fill(configuration, -1);
	}

	public int[] toArray() {
		return configuration;
	}

	@Override
	public String toString() {
		// return Arrays.toString(configuration);
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int n = attributes.size();
		for (int i = 0; i < configuration.length; i++) {
			if (configuration[i] != -1 && i != n * markovLag + childNode) {
				int lag = i / n;
				int id = i % n;
				sb.append(attributes.get(id).getName() + "[" + lag + "]=" + attributes.get(id).get(configuration[i]));
				sb.append(", ");
			}
		}
		// Readable version
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(configuration);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Configuration))
			return false;
		Configuration other = (Configuration) obj;
		if (!Arrays.equals(configuration, other.configuration))
			return false;
		return true;
	}

}
