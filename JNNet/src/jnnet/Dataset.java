package jnnet;

import static jnnet.JNNetTools.ATOMIC_INTEGER_FACTORY;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import jnnet.draft.CSV2Bin.DataType;

import net.sourceforge.aprog.tools.ConsoleMonitor;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
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
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class IO {
		
		private IO() {
			throw new IllegalInstantiationException();
		}
		
		public static final void writeARFF(final Dataset dataset, final PrintStream out) {
			final TicToc timer = new TicToc();
			final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
			
			System.out.println("Exporting dataset as ARFF... " + new Date(timer.tic()));
			
			out.println("@relation relation");
			
			final int dimension = dataset.getItemSize() - 1;
			
			for (int i = 0; i < dimension; ++i) {
				monitor.ping("Writing ARFF header " + (i + 1) + " / " + dimension + "\r");
				out.println("@attribute x" + i + " numeric");
			}
			
			out.println("@attribute class { 0.0, 1.0 }");
			
			out.println("@data");
			
			final int n = dataset.getItemCount();
			
			for (int i = 0; i < n; ++i) {
				monitor.ping("Writing ARFF data " + (i + 1) + " / " + n + "\r");
				out.println(join(",", dataset.getItem(i)));
			}
			
			monitor.pause();
			
			System.out.println("Exporting dataset as ARFF done in " + timer.toc() + " ms");
			
			out.close();
		}
		
		public static final void writeBin(final Dataset dataset
				, final DataType dataType, final DataOutputStream out) {
			final TicToc timer = new TicToc();
			final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
			
			System.out.println("Exporting dataset as Bin... " + new Date(timer.tic()));
			
			try {
				out.writeByte(dataType.ordinal());
				out.writeInt(dataset.getItemSize());
				
				final int n = dataset.getItemCount();
				
				for (int i = 0; i < n; ++i) {
					monitor.ping("Writing Bin data " + (i + 1) + " / " + n + "\r");
					
					for (final double value : dataset.getItem(i)) {
						dataType.write(value, out);
					}
				}
				
				monitor.pause();
				
				System.out.println("Exporting dataset as Bin done in " + timer.toc() + " ms");
				
				out.close();
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
		
		/**
		 * Creates a new string by concatenating the representations of the <code>objects</code>
		 * and using the specified <code>separator</code>.
		 * 
		 * @param separator
		 * <br>Not null
		 * @param elements
		 * <br>Not null
		 * @return
		 * <br>Not null
		 * <br>New
		 */
		public static final String join(final String separator, final double... elements) {
			final StringBuilder resultBuilder = new StringBuilder();
			final int n = elements.length;
			
			if (0 < n) {
				resultBuilder.append(elements[0]);
				
				for (int i = 1; i < n; ++i) {
					resultBuilder.append(separator).append(elements[i]);
				}
			}
			
			return resultBuilder.toString();
		}
		
	}
	
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
