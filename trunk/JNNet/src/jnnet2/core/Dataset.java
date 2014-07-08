package jnnet2.core;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public abstract interface Dataset extends Serializable, Iterable<double[]> {
	
	public abstract long getItemCount();
	
	public abstract int getItemSize();
	
	public abstract double[] getItem(long itemId, double[] result);
	
	public abstract LabelStatistics getLabelStatistics();
	
	public default double getLabel(final long itemId) {
		final int n = this.getItemSize();
		
		return this.getItem(itemId, new double[n])[n - 1];
	}
	
	@Override
	public default Iterator<double[]> iterator() {
		return new Iterator<double[]>() {
			
			private final double[] item = new double[Dataset.this.getItemSize()];
			
			private long itemId;
			
			@Override
			public final boolean hasNext() {
				return this.itemId < Dataset.this.getItemCount();
			}
			
			@Override
			public final double[] next() {
				return Dataset.this.getItem(this.itemId++, this.item);
			}
			
		};
	}
	
}
