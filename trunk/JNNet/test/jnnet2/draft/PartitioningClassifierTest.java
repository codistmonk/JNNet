package jnnet2.draft;

import static net.sourceforge.aprog.tools.Tools.debugError;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;

import jnnet.VectorStatistics;
import jnnet2.core.Classifier;
import jnnet2.core.ConfusionMatrix;
import jnnet2.core.Dataset;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class PartitioningClassifierTest {
	
	@Test
	public final void test1() {
		test(DATASET1);
	}
	
	@Test
	public final void test2() {
		test(DATASET2);
	}
	
	@Test
	public final void test3() {
		test(DATASET3);
	}
	
	@Test
	public final void test4() {
		test(DATASET4);
	}
	
	public static final Dataset DATASET1 = new SimpleDataset()
			.add(0.0, 0.0)
			.add(1.0, 1.0)
			;
	
	public static final Dataset DATASET2 = new SimpleDataset()
			.add(0.0, 0.0, 1.0)
			.add(1.0, 1.0, 1.0)
			.add(1.0, 0.0, 0.0)
			.add(0.0, 1.0, 0.0)
			;
	
	public static final Dataset DATASET3 = new SimpleDataset()
			.add(0.0, 0.0, 0.0)
			.add(2.0, 0.0, 0.0)
			.add(0.0, 2.0, 0.0)
			.add(2.0, 2.0, 0.0)
			.add(3.0, 1.0, 1.0)
			.add(5.0, 1.0, 1.0)
			.add(3.0, 3.0, 1.0)
			.add(5.0, 3.0, 1.0)
			.add(0.0, 3.0, 2.0)
			.add(2.0, 3.0, 2.0)
			.add(0.0, 5.0, 2.0)
			.add(2.0, 5.0, 2.0)
			;
	
	public static final Dataset DATASET4 = new SimpleDataset()
			.add(2.0, 2.0, 0.0)
			.add(3.0, 1.0, 1.0)
			.add(5.0, 1.0, 1.0)
			.add(3.0, 3.0, 1.0)
			.add(5.0, 3.0, 1.0)
	;
	
	public static final void test(final Dataset dataset) {
		final Classifier classifier = new PartitioningClassifier(dataset);
		final ConfusionMatrix confusionMatrix = new ConfusionMatrix().updateWith(classifier, dataset);
		
		assertNoErrors(confusionMatrix, dataset);
	}
	
	public static final void assertNoErrors(final ConfusionMatrix confusionMatrix, final Dataset dataset) {
		final Map<Double, VectorStatistics> statistics = dataset.getLabelStatistics().getStatistics();
		final Collection<Double> labels = statistics.keySet();
		
		for (final Double prediction : labels) {
			for (final Double reference : labels) {
				final double expectedCount = prediction.equals(reference) ?
						statistics.get(reference).getCount() : 0.0;
				try {
					assertEquals(expectedCount, confusionMatrix.getCount(prediction, reference), 0.0);
				} catch (final AssertionError error) {
					debugError(prediction, reference, expectedCount, confusionMatrix.getCount(prediction, reference));
					
					throw error;
				}
			}
		}
	}
	
}
