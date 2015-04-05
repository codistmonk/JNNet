package jnnet2.draft;

import static org.junit.Assert.*;

import java.util.Map;

import net.sourceforge.aprog.tools.MathTools.VectorStatistics;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class SimpleDatasetTest {
	
	@Test
	public final void test1() {
		final SimpleDataset dataset = new SimpleDataset();
		final Map<Double, VectorStatistics> labelStatistics = dataset.getLabelStatistics().getStatistics();
		
		assertEquals(0L, dataset.getItemSize());
		assertEquals(0L, dataset.getItemCount());
		assertEquals(0L, labelStatistics.size());
		
		dataset.add(10.0, 20.0, 0.0);
		dataset.add(30.0, 40.0, 1.0);
		
		assertEquals(3L, dataset.getItemSize());
		assertEquals(2L, dataset.getItemCount());
		assertEquals(2L, labelStatistics.size());
		assertArrayEquals(new double[] { 10.0, 20.0 }, labelStatistics.get(0.0).getMeans(), 1.0E-6);
		assertArrayEquals(new double[] { 30.0, 40.0 }, labelStatistics.get(1.0).getMeans(), 1.0E-6);
	}
	
}
