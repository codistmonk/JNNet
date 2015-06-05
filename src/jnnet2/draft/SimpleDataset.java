package jnnet2.draft;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.List;

import jnnet2.core.Dataset;
import jnnet2.core.LabelStatistics;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class SimpleDataset implements Dataset {
	
	private final LabelStatistics labelStatistics = new LabelStatistics(); 
	
	private final List<double[]> items = new ArrayList<>();
	
	private int itemSize;
	
	public final SimpleDataset add(final double... item) {
		final int n = item.length;
		
		if (this.getItemSize() == 0) {
			this.itemSize = n;
		} else if (this.getItemSize() != n) {
			throw new IllegalArgumentException();
		}
		
		if (!isItemValid(item)) {
			return this;
		}
		
		this.items.add(item);
		this.labelStatistics.addItem(item);
		
		return this;
	}
	
	@Override
	public final long getItemCount() {
		return this.items.size();
	}
	
	@Override
	public final int getItemSize() {
		return this.itemSize;
	}
	
	@Override
	public final double[] getItem(final long itemId, final double[] result) {
		arraycopy(this.items.get((int) itemId), 0, result, 0, this.getItemSize());
		
		return result;
	}
	
	@Override
	public final LabelStatistics getLabelStatistics() {
		return this.labelStatistics;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2607731134739940184L;
	
	public static final boolean isItemValid(final double... item) {
		for (final double value : item) {
			if (isInfinite(value) || isNaN(value)) {
				return false;
			}
		}
		
		return true;
	}
	
}
