package jnnet.draft;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public abstract interface ValueSource extends Serializable {
	
	public abstract void update();
	
	public abstract double getValue();
	
}
	