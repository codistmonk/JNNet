package jnnet4;

import static net.sourceforge.aprog.tools.Tools.instances;

import java.io.Serializable;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class VectorStatistics implements Serializable {
	
	private final Statistics[] statistics;
	
	public VectorStatistics(final int dimension) {
		this.statistics = instances(dimension, STATISTICS_FACTORY);
	}
	
	public final Statistics[] getStatistics() {
		return this.statistics;
	}
	
	public final void addValues(final double... values) {
		final int n = this.getStatistics().length;
		
		for (int i = 0; i < n; ++i) {
			this.getStatistics()[i].addValue(values[i]);
		}
	}
	
	public final double getCount() {
		return this.getStatistics()[0].getCount();
	}
	
	public final double[] getMeans() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getMean();
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8371199963847573845L;
	
	public static final DefaultFactory<Statistics> STATISTICS_FACTORY = DefaultFactory.forClass(Statistics.class);
	
	public static final double[] subtract(final double[] v1, final double[] v2) {
		final int n = v1.length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = v1[i] - v2[i];
		}
		
		return result;
	}
	
	public static final double[] add(final double[] v1, final double[] v2) {
		final int n = v1.length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = v1[i] + v2[i];
		}
		
		return result;
	}
	
	public static final double dot(final double[] v1, final double[] v2) {
		final int n = v1.length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += v1[i] * v2[i];
		}
		
		return result;
	}
	
	public static final double[] scaled(final double[] v, final double scale) {
		final int n = v.length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = v[i] * scale;
		}
		
		return result;
	}
	
}
