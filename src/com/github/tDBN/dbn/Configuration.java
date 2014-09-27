package com.github.tDBN.dbn;

import java.util.Arrays;
import java.util.List;

public class Configuration {

	protected List<Attribute> attributes;

	protected int[] configuration;

	public Configuration(Configuration c) {
		this.attributes = c.attributes;
		this.configuration = c.configuration.clone();
	}

	protected Configuration(List<Attribute> attributes) {
		this.attributes = attributes;
		this.configuration = new int[2 * attributes.size()];
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
		return Arrays.toString(configuration);
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
