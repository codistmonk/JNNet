package jnnet2.draft;

import static multij.tools.Tools.getResourceAsStream;
import static multij.tools.Tools.getThisPackagePath;
import static org.junit.Assert.*;
import multij.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-07-17)
 */
public final class VirtualARFFDatasetTest {
	
	@Test
	public final void test1() {
		final VirtualARFFDataset dataset = new VirtualARFFDataset(getResourceAsStream(getThisPackagePath() + "iris.arff"));
		final int itemSize = dataset.getItemSize();
		
		assertEquals(5L, itemSize);
		assertEquals(150L, dataset.getItemCount());
		assertArrayEquals(new double[] { 5.1, 3.5, 1.4, 0.2, 0.0 }, dataset.getItem(0L, new double[itemSize]), 0.0);
		assertArrayEquals(new double[] { 7.0, 3.2, 4.7, 1.4, 1.0 }, dataset.getItem(50L, new double[itemSize]), 0.0);
		assertArrayEquals(new double[] { 6.3, 3.3, 6.0, 2.5, 2.0 }, dataset.getItem(100L, new double[itemSize]), 0.0);
		assertEquals(3L, dataset.getLabelStatistics().getLabelCount());
		assertEquals(50.0, dataset.getLabelStatistics().getStatistics().get(0.0).getCount(), 0.0);
		assertEquals(50.0, dataset.getLabelStatistics().getStatistics().get(1.0).getCount(), 0.0);
		assertEquals(50.0, dataset.getLabelStatistics().getStatistics().get(2.0).getCount(), 0.0);
	}
	
}
