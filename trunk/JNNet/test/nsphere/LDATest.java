package nsphere;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.copyOfRange;
import static jnnet.draft.LinearConstraintSystem.Abstract.dot;
import static net.sourceforge.aprog.tools.MathTools.lcm;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import jgencode.primitivelists.DoubleList;
import jnnet.Dataset;
import jnnet.Dataset.DatasetStatistics;
import jnnet.VectorStatistics;
import jnnet.draft.LinearConstraintSystem;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;

/**
 * @author codistmonk (creation 2014-05-07)
 */
public final class LDATest {
	
	@Test
	public final void test() {
		final double[] expectedDirection = { -1.0, 2.0, 3.0 };
		final SimpleDataset dataset = generateDiscretePlaneBounds(
				(int) expectedDirection[0], (int) expectedDirection[1], (int) expectedDirection[2]);
		final int dimension = dataset.getItemSize() - 1;
		final VectorStatistics[] datasetStatistics = dataset.getStatistics().getStatistics();
		
		debugPrint(dataset);
		dataset.getStatistics().printTo(System.out);
		
		final double[] mu01 = LinearConstraintSystem.Abstract.add(-1.0, datasetStatistics[0].getMeans(), 0
				, 1.0, datasetStatistics[1].getMeans(), 0, new double[dimension], 0, dimension);
		
		debugPrint("Projection on means direction:", Arrays.toString(mu01));
		
		computeProjectedStatistics(dataset, mu01).printTo(System.out);
		
		{
			final TicToc timer = new TicToc();
			
			debugPrint("LDA...", new Date(timer.tic()));
			
			final MatrixBuilder<?>[] builders = { PrimitiveMatrix.getBuilder(dimension, (int) datasetStatistics[0].getCount())
					, PrimitiveMatrix.getBuilder(dimension, (int) datasetStatistics[1].getCount()) };
			final int[] counts = new int[builders.length];
			final int n = dataset.getItemCount();
			
			for (int i = 0; i < n; ++i) {
				final int label = (int) dataset.getItemLabel(i);
				
				setColumn(builders[label], counts[label]++, dataset.getItemWeights(i));
			}
			
			final BasicMatrix x0 = builders[0].build();
			final BasicMatrix x1 = builders[1].build();
			final BasicMatrix s0 = x0.multiplyRight(x0.transpose());
			final BasicMatrix s1 = x1.multiplyRight(x1.transpose());
			final BasicMatrix s = s0.add(s1);
			final double[] bestDirection = toArray(s.invert().multiplyRight(columnVector(mu01)));
			
			debugPrint("LDA done in", timer.toc(), "ms");
			debugPrint("Projection on best direction:", Arrays.toString(bestDirection));
			
			final DatasetStatistics bestProjection = computeProjectedStatistics(dataset, bestDirection);
			
			bestProjection.printTo(System.out);
			
			assertTrue(bestProjection.getStatistics()[0].getMaxima()[0] < bestProjection.getStatistics()[1].getMinima()[0]);
			assertEquals(0.0, colinearity(expectedDirection, bestDirection), 1E-4);
		}
	}
	
	public static final double colinearity(final double[] v1, final double[] v2) {
		final int n = min(v1.length, v2.length) - 1;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += det(v1[i], v1[i + 1], v2[i], v2[i + 1]);
		}
		
		return result;
	}
	
	public static final double det(final double a, final double b, final double c, final double d) {
		return a * d - b * c;
	}
	
	public static final double[] toArray(final BasicMatrix columnVector) {
		if (columnVector.getColDim() != 1) {
			throw new IllegalArgumentException();
		}
		
		final int n = columnVector.getRowDim();
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = columnVector.doubleValue(i);
		}
		
		return result;
	}
	
	public static final BasicMatrix columnVector(final double... values) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(values.length);
		
		setColumn(builder, 0, values);
		
		return builder.build();
	}
	
	public static final void setColumn(final MatrixBuilder<?> builder, final int columnIndex, final double... values) {
		final int n = min(builder.getRowDim(), values.length);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, columnIndex, values[i]);
		}
	}
	
	public DatasetStatistics computeProjectedStatistics(final Dataset dataset, final double... axis) {
		final DatasetStatistics result = new DatasetStatistics(1);
		final int n = dataset.getItemCount();
		
		for (int i = 0; i < n; ++i) {
			result.addItem(dot(dataset.getItemWeights(i), axis), dataset.getItemLabel(i));
		}
		
		return result;
	}
	
	public static final SimpleDataset generateDiscretePlaneBounds(final int a, final int b, final int c) {
		final SimpleDataset result = new SimpleDataset(4);
		final int m = (int) max(lcm(abs(a), abs(c)), lcm(abs(b), abs(c)));
		
		for (int y = -m; y <= m; ++y) {
			for (int x = - m; x <= m; ++x) {
				// a x + b y + c z = 0
				// <- c z = - a x - b y
				// <- floor(- (a x + b y) / c) <= z < floor(- (a x + b y) / c) + 1
				final double z = floor(- (double) (a * x + b * y) / c);
				
				result.addItem(x, y, z, 0.0);
				result.addItem(x, y, z + 1.0, 1.0);
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-05-07)
	 */
	public static final class SimpleDataset implements Dataset {
		
		private final int order;
		
		private final DoubleList data;
		
		private final DatasetStatistics statistics;
		
		public SimpleDataset(final int order) {
			this.order = order;
			this.data = new DoubleList();
			this.statistics = new DatasetStatistics(order - 1);
		}
		
		public final DoubleList getData() {
			return this.data;
		}
		
		public final DatasetStatistics getStatistics() {
			return this.statistics;
		}
		
		public final SimpleDataset addItem(final double... item) {
			this.getData().addAll(item);
			this.getStatistics().addItem(item);
			
			return this;
		}
		
		@Override
		public final int getItemCount() {
			return this.getData().size() / this.getItemSize();
		}
		
		@Override
		public final int getItemSize() {
			return this.order;
		}
		
		@Override
		public final double getItemValue(final int itemId, final int valueId) {
			return this.getData().get(itemId * this.getItemSize() + valueId);
		}
		
		@Override
		public final double[] getItem(final int itemId) {
			final int n = this.getItemSize();
			final int offset = itemId * n;
			
			return copyOfRange(this.getData().toArray(), offset, offset + n);
		}
		
		@Override
		public final double[] getItemWeights(final int itemId) {
			final int n = this.getItemSize();
			final int offset = itemId * n;
			
			return copyOfRange(this.getData().toArray(), offset, offset + n - 1);
		}
		
		@Override
		public final double getItemLabel(final int itemId) {
			return this.getItemValue(itemId, this.getItemSize() - 1);
		}
		
		@Override
		public final String toString() {
			return this.getData().toString();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 1448280373109937349L;
		
	}
	
}