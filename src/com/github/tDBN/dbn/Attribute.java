package com.github.tDBN.dbn;

/**
 * 
 * An attribute is a representation of a random variable in a DBN. It can be
 * numeric (discrete valued) or nominal and takes a finite number of different
 * values. Values are indexed by sequential integers, which are used for
 * representing them.
 * 
 * @author zlm
 * 
 */
public interface Attribute {

	public boolean isNumeric();

	public boolean isNominal();

	public int size();

	public String get(int index);

	public int getIndex(String value);

	public boolean add(String value);

	public String toString();

	public void setName(String name);

	public String getName();

}
