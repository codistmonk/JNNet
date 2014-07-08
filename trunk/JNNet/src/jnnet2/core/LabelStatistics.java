package jnnet2.core;

import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import jnnet.VectorStatistics;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class LabelStatistics implements Serializable {
	
	private final Map<Double, VectorStatistics> statistics = new TreeMap<>();
	
	public final LabelStatistics addItem(final double... item) {
		final int n = item.length;
		final int labelIndex = n - 1;
		final int inputSize = labelIndex;
		
		getOrCreate(this.getStatistics(), item[labelIndex]
				, DefaultFactory.forClass(VectorStatistics.class, inputSize)).addValues(item);
		
		return this;
	}
	
	public final Map<Double, VectorStatistics> getStatistics() {
		return this.statistics;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 55136198129781124L;
	
}