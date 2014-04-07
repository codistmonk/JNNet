package jnnet4;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-04-07)
 */
public abstract interface Dataset extends Serializable {
	
	public abstract int getItemCount();
	
	public abstract int getItemSize();
	
	public abstract double getItemValue(int itemId, int valueId);
	
	public abstract double[] getItem(int itemId);
	
	public abstract double[] getItemWeights(int itemId);
	
	public abstract double getItemLabel(int itemId);
	
}
