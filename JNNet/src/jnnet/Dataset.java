package jnnet;

import static jnnet.JNNetTools.ATOMIC_INTEGER_FACTORY;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.instances;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

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
	
	/**
	 * @author codistmonk (creation 2014-04-09)
	 */
	public static final class DatasetStatistics implements Serializable {
		
		private final Map<Integer, AtomicInteger> labelCounts;
		
		private final VectorStatistics[] statistics;
		
		public DatasetStatistics(final int inputDimension) {
			this.labelCounts = new TreeMap<Integer, AtomicInteger>();
			this.statistics = instances(3, DefaultFactory.forClass(VectorStatistics.class, inputDimension));
		}
		
		public final void reset() {
			for (final VectorStatistics statistics : this.getStatistics()) {
				statistics.reset();
			}
		}
		
		public final Map<Integer, AtomicInteger> getLabelCounts() {
			return this.labelCounts;
		}
		
		public final VectorStatistics[] getStatistics() {
			return this.statistics;
		}
		
		public final void addItem(final double... item) {
			final int labelOffset = this.getInputDimension();
			final int label = (int) item[labelOffset];
			
			this.getStatistics()[label].addValues(item);
			this.getStatistics()[2].addValues(item);
			
			getOrCreate(this.getLabelCounts(), label, ATOMIC_INTEGER_FACTORY).incrementAndGet();
		}
		
		public final void printTo(final PrintStream out) {
			final int inputDimension = this.getInputDimension();
			
			out.println("labelCounts: " + this.getLabelCounts());
			out.println("inputDimension: " + inputDimension);
			
			if (inputDimension <= 100) {
				for (int i = 0; i < 2; ++i) {
					out.println(i + "-statistics");
					out.println("means: " + Arrays.toString(this.getStatistics()[i].getMeans()));
					out.println("minima: " + Arrays.toString(this.getStatistics()[i].getMinima()));
					out.println("maxima: " + Arrays.toString(this.getStatistics()[i].getMaxima()));
					out.println("stddev: " + Arrays.toString(this.getStatistics()[i].getStandardDeviations()));
				}
			} else {
				out.println("High-dimensional statistics not shown");
			}
		}
		
		public final int getInputDimension() {
			return this.getStatistics()[0].getStatistics().length;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 597257108917904625L;
		
	}
	
}
