package jnnet;

import java.io.Serializable;
import java.time.Duration;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public abstract interface BinaryClassifier extends Serializable {
	
	public abstract int getInputDimension();
	
	public abstract boolean accept(double... item);
	
	public abstract SimpleConfusionMatrix evaluate(Dataset dataset, EvaluationMonitor monitor);
	
	/**
	 * {@value}.
	 */
	public static final long LOGGING_MILLISECONDS = 15000L;
	
	/**
	 * @author codistmonk (creation 2014-03-25)
	 */
	public static abstract interface EvaluationMonitor extends Serializable {
		
		public abstract void truePositive(int sampleId);
		
		public abstract void falsePositive(int sampleId);
		
		public abstract void trueNegative(int sampleId);
		
		public abstract void falseNegative(int sampleId);
		
	}
	
	/**
	 * @author codistmonk (creation 2014-03-25)
	 */
	public static final class Default {
		
		private Default() {
			throw new IllegalInstantiationException();
		}
		
		public static final SimpleConfusionMatrix defaultEvaluate(final BinaryClassifier classifier,
				final Dataset dataset, final EvaluationMonitor monitor) {
			final int itemCount = dataset.getItemCount();
			final SimpleConfusionMatrix result = new SimpleConfusionMatrix();
			final TicToc timer = new TicToc();
			
			timer.tic();
			
			for (int sampleId = 0; sampleId < itemCount; ++sampleId) {
				if (LOGGING_MILLISECONDS <= timer.toc()) {
					System.out.print(Thread.currentThread() + " evaluating: " + sampleId + "/" + itemCount
							+ " ETA: " + Duration.ofMillis((itemCount - sampleId) * timer.getTotalTime() / (sampleId | 1L))
							+ "\r");
					timer.tic();
				}
				
				final double expected = dataset.getItemLabel(sampleId);
				final double actual = classifier.accept(dataset.getItemWeights(sampleId)) ? 1.0 : 0.0;
				
				if (expected != actual) {
					if (expected == 1.0) {
						if (monitor != null) {
							monitor.falseNegative(sampleId);
						}
						result.getFalseNegativeCount().incrementAndGet();
					} else {
						if (monitor != null) {
							monitor.falsePositive(sampleId);
						}
						result.getFalsePositiveCount().incrementAndGet();
					}
				} else {
					if (expected == 1.0) {
						if (monitor != null) {
							monitor.truePositive(sampleId);
						}
						result.getTruePositiveCount().incrementAndGet();
					} else {
						if (monitor != null) {
							monitor.trueNegative(sampleId);
						}
						result.getTrueNegativeCount().incrementAndGet();
					}
				}
			}
			
			return result;
		}
		
	}
	
}
