package jnnet2.draft;

import static org.junit.Assert.*;

import jnnet2.core.Classifier;
import jnnet2.core.ConfusionMatrix;
import jnnet2.core.Dataset;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class PartitioningClassifierTest {
	
	@Test
	public final void test1() {
		final Classifier classifier = new PartitioningClassifier(DATASET1);
		final ConfusionMatrix confusionMatrix = new ConfusionMatrix().updateWith(classifier, DATASET1);
		
		assertEquals(1.0, confusionMatrix.getCount(0.0, 0.0), 0.0);
		assertEquals(0.0, confusionMatrix.getCount(0.0, 1.0), 0.0);
		assertEquals(1.0, confusionMatrix.getCount(1.0, 1.0), 0.0);
		assertEquals(0.0, confusionMatrix.getCount(1.0, 0.0), 0.0);
	}
	
	@Test
	public final void test2() {
		final Classifier classifier = new PartitioningClassifier(DATASET2);
		final ConfusionMatrix confusionMatrix = new ConfusionMatrix().updateWith(classifier, DATASET2);
		
		assertEquals(2.0, confusionMatrix.getCount(0.0, 0.0), 0.0);
		assertEquals(0.0, confusionMatrix.getCount(0.0, 1.0), 0.0);
		assertEquals(2.0, confusionMatrix.getCount(1.0, 1.0), 0.0);
		assertEquals(0.0, confusionMatrix.getCount(1.0, 0.0), 0.0);
	}
	
	public static final Dataset DATASET1 = new SimpleDataset()
			.add(0.0, 0.0)
			.add(1.0, 1.0);
	
	public static final Dataset DATASET2 = new SimpleDataset()
			.add(0.0, 0.0, 1.0)
			.add(1.0, 1.0, 1.0)
			.add(1.0, 0.0, 0.0)
			.add(0.0, 1.0, 0.0);
	
}
