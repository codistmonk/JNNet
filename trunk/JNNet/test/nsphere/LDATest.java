package nsphere;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static jnnet.draft.LinearConstraintSystem.Abstract.dot;
import static net.sourceforge.aprog.tools.MathTools.lcm;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import imj2.tools.VectorStatistics;

import java.util.Arrays;
import java.util.Date;

import jnnet.BinDataset;
import jnnet.Dataset;
import jnnet.Dataset.DatasetStatistics;
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
		final BinDataset dataset = generateDiscretePlaneBounds(
				(int) expectedDirection[0], (int) expectedDirection[1], (int) expectedDirection[2]);
		final int dimension = dataset.getItemSize() - 1;
		final VectorStatistics[] datasetStatistics = dataset.getStatistics().getStatistics();
		
		dataset.getStatistics().printTo(System.out);
		
		final double[] mu01 = LinearConstraintSystem.Abstract.add(-1.0, datasetStatistics[0].getMeans(), 0
				, 1.0, datasetStatistics[1].getMeans(), 0, new double[dimension], 0, dimension);
		
		debugPrint("Projection on means direction:", Arrays.toString(mu01));
		
		computeProjectedStatistics(dataset, mu01).printTo(System.out);
		
		{
			final TicToc timer = new TicToc();
			
			debugPrint("LDA...", new Date(timer.tic()));
			
			final double[][] covariances = computeCovariances(dataset);
			final BasicMatrix s0 = matrix(dimension, covariances[0]);
			final BasicMatrix s1 = matrix(dimension, covariances[1]);
			final BasicMatrix s = s0.add(s1);
			final double[] bestDirection = toArray(s.solve(columnVector(mu01)));
			
			debugPrint("LDA done in", timer.toc(), "ms");
			debugPrint("Projection on best direction:", Arrays.toString(bestDirection));
			
			final DatasetStatistics bestProjection = computeProjectedStatistics(dataset, bestDirection);
			
			bestProjection.printTo(System.out);
			
			assertTrue(bestProjection.getStatistics()[0].getMaxima()[0] < bestProjection.getStatistics()[1].getMinima()[0]);
			assertEquals(0.0, colinearity(expectedDirection, bestDirection), 1E-4);
		}
	}
	
	public static final double[][] computeCovariances(final Dataset dataset) {
		final int dimension = dataset.getItemSize() - 1;
		final double[][] result = { new double[dimension * dimension], new double[dimension * dimension] };
		final int n = dataset.getItemCount();
		
		// Set upper half
		for (int i = 0; i < n; ++i) {
			final double[] item = dataset.getItemWeights(i);
			final int label = (int) dataset.getItemLabel(i);
			final double[] matrix = result[label];
			
			for (int j = 0; j < dimension; ++j) {
				final double jth = item[j];
				
				for (int k = j; k < dimension; ++k) {
					final double kth = item[k];
					
					matrix[j * dimension + k] += jth * kth;
				}
			}
		}
		
		// Copy upper half to lower half
		for (final double[] matrix : result) {
			for (int j = 0; j < dimension; ++j) {
				for (int k = j + 1; k < dimension; ++k) {
					matrix[k * dimension + j] = matrix[j * dimension + k];
				}
			}
		}
		
		return result;
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
	
	public static final BasicMatrix matrix(final int columnCount, final double... values) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(values.length / columnCount, columnCount);
		
		for (int i = 0; i < values.length; ++i) {
			builder.set(i, values[i]);
		}
		
		return builder.build();
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
	
	public static final BinDataset generateDiscretePlaneBounds(final int a, final int b, final int c) {
		final BinDataset result = new BinDataset(4);
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
	
}
