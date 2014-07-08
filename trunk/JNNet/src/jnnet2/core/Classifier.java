package jnnet2.core;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public abstract interface Classifier extends Serializable {
	
	public abstract int getInputSize();
	
	public abstract double getLabelFor(double... input);
	
}
