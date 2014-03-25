package jnnet4;

import static java.util.Arrays.copyOfRange;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.io.Serializable;

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
	public static final long LOGGING_MILLISECONDS = 5000L;
	
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
			final TicToc timer = new TicToc();
			final SimpleConfusionMatrix result = new SimpleConfusionMatrix();
			final int step = dataset.getStep();
			final double[] data = dataset.getData();
			
			timer.tic();
			
			for (int i = 0; i < data.length; i += step) {
				if (LOGGING_MILLISECONDS <= timer.toc()) {
					debugPrint(i, "/", data.length);
					timer.tic();
				}
				
				final double expected = data[i + step - 1];
				final double actual = classifier.accept(copyOfRange(data, i, i + step - 1)) ? 1.0 : 0.0;
				final int sampleId = i / step;
				
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
