package jnnet2.core;

import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools.VectorStatistics;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class LabelStatistics implements Serializable {
	
	private final Map<Double, VectorStatistics> statistics = new TreeMap<>();
	
	private final Map<Double, Integer> labelIds = new HashMap<>();
	
	public final LabelStatistics addItem(final double... item) {
		final int n = item.length;
		final int labelIndex = n - 1;
		final int inputSize = labelIndex;
		final double label = item[labelIndex];
		
		getOrCreate(this.getStatistics(), label
				, DefaultFactory.forClass(VectorStatistics.class, inputSize)).addValues(item);
		
		final Integer labelId = this.labelIds.get(label);
		
		if (labelId == null) {
			this.labelIds.put(label, this.labelIds.size());
		}
		
		return this;
	}
	
	public final int getLabelId(final double label) {
		return this.labelIds.get(label);
	}
	
	public final int getLabelCount() {
		return this.getStatistics().size();
	}
	
	public final Map<Double, VectorStatistics> getStatistics() {
		return this.statistics;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 55136198129781124L;
	
}