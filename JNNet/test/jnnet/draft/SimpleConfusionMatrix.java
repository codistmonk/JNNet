package jnnet.draft;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class SimpleConfusionMatrix implements Serializable {
	
	private final AtomicLong truePositiveCount = new AtomicLong();
	
	private final AtomicLong falsePositiveCount = new AtomicLong();
	
	private final AtomicLong trueNegativeCount = new AtomicLong();
	
	private final AtomicLong falseNegativeCount = new AtomicLong();
	
	public final AtomicLong getTruePositiveCount() {
		return this.truePositiveCount;
	}
	
	public final AtomicLong getFalsePositiveCount() {
		return this.falsePositiveCount;
	}
	
	public final AtomicLong getTrueNegativeCount() {
		return this.trueNegativeCount;
	}
	
	public final AtomicLong getFalseNegativeCount() {
		return this.falseNegativeCount;
	}
	
	public final long getTotalErrorCount() {
		return this.getFalsePositiveCount().get() + this.getFalseNegativeCount().get();
	}
	
	public final long getTotalSampleCount() {
		return this.getTruePositiveCount().get() + this.getFalsePositiveCount().get() +
				this.getTrueNegativeCount().get() + this.getFalseNegativeCount().get();
	}
	
	public final long getPredictedPositiveCount() {
		return this.getTruePositiveCount().get() + this.getFalsePositiveCount().get();
	}
	
	public final long getPredictedNegativeCount() {
		return this.getTrueNegativeCount().get() + this.getFalseNegativeCount().get();
	}
	
	public final long getActualPositiveCount() {
		return this.getTruePositiveCount().get() + this.getFalseNegativeCount().get();
	}
	
	public final long getActualNegativeCount() {
		return this.getTrueNegativeCount().get() + this.getFalsePositiveCount().get();
	}
	
	public final double getSensitivity() {
		return this.getTruePositiveCount().doubleValue() / this.getActualPositiveCount();
	}
	
	public final double getSpecificity() {
		return this.getTrueNegativeCount().doubleValue() / this.getActualNegativeCount();
	}
	
	@Override
	public final String toString() {
		return "totalSamples: " + this.getTotalSampleCount() +
				" totalErrors: " + this.getTotalErrorCount() +
				" truePositives: " + this.getTruePositiveCount() +
				" falsePositives: " + this.getFalsePositiveCount() +
				" trueNegatives: " + this.getTrueNegativeCount() +
				" falseNegatives: " + this.getFalseNegativeCount() +
				" sensitivity: " + this.getSensitivity() +
				" specificity: " + this.getSpecificity() +
				" (se+sp)/2: " + (this.getSensitivity() + this.getSpecificity()) / 2.0;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -4501763144319557083L;
	
}
