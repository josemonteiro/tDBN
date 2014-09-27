package com.github.tDBN.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements an array that is also indexed by the values, which means that it
 * takes O(1) time to access an item both by its index and by its value.
 * Removing items is not possible. Adapted from
 * http://stackoverflow.com/a/7834138
 * 
 * @param <T>
 *            array type
 */
public class BidirectionalArray<T> {
	private List<T> indexToValueMap = new ArrayList<T>();
	private Map<T, Integer> valueToIndexMap = new HashMap<T, Integer>();

	/**
	 * @return false if the value is already present
	 */
	public boolean add(T value) {
		if (this.containsValue(value)) {
			return false;
		}
		int size = indexToValueMap.size();
		indexToValueMap.add(size, value);
		valueToIndexMap.put(value, size);
		return true;
	}

	public boolean containsValue(T value) {
		return valueToIndexMap.containsKey(value);
	}

	public int getIndex(T value) {
		return valueToIndexMap.get(value);
	}

	public T get(int index) {
		return indexToValueMap.get(index);
	}

	public int size() {
		return indexToValueMap.size();
	}

	@Override
	public String toString() {
		return "" + indexToValueMap;
	}
}