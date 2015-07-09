package jnnet3.dct.draft;

import static java.lang.Math.*;
import static multij.tools.Tools.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;
import multij.tools.Tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.data.general.DefaultHeatMapDataset;
import org.jfree.data.general.HeatMapDataset;
import org.jfree.data.general.HeatMapUtilities;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;

/**
 * @author codistmonk (creation 2015)
 */
public final class DWTDemo {
	
	private DWTDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		if (false) {
//			SwingTools.show(new ChartPanel(plot(new DWT.Wave(DWT::step, 1.0, 0.0, 6),  drange(-0.5, 0.01, 1.5))), "plot", false);
//			SwingTools.show(new ChartPanel(plot(new DWT.Wave(DWT::sigmoid, 4.0, 0.0, 6),  drange(-0.5, 0.01, 1.5))), "plot", false);
//			SwingTools.show(new ChartPanel(plot(new DWT.SinmoidWave(6),  drange(-0.5, 0.01, 1.5))), "plot", false);
			SwingTools.show(new ChartPanel(plot(new DWT.StepWave(6),  drange(-0.5, 0.01, 1.5))), "plot", false);
//			SwingTools.show(new ChartPanel(plot(new DWT.ReLUWave(6),  drange(-0.5, 0.01, 1.5))), "plot", false);
		}
		
		if (true) {
			final int[] ns = { 2, 2 };
			final double[] values = { 0.0, 1.0, 1.0, 0.0 };
//			final double[] values = Arrays.stream(intRange(product(ns))).mapToDouble(x -> x).toArray();
//			values[values.length / 2] *= -1.0;
			final BasicMatrix f = newColumn(values);
			
			debugPrint(Arrays.toString(ns));
			
			final IntFunction<DoubleUnaryOperator> waveFactory = DWT.StepWave::new;
			final BasicMatrix m = DWT.newIDWTMatrix(waveFactory, ns);
			
			if (f.size() < 50) {
				debugPrint(m);
			}
			
			final int rank = m.getRank();
			
			if (rank != f.size()) {
				debugError(rank);
			}
			
			final BasicMatrix g = m.solve(f);
			
			debugPrint(f);
			debugPrint(g);
			
			{
				final double[] normalizedXs = new double[ns.length];
				
				for (final int[] xs : cartesian(bounds(ns))) {
					normalize(xs, ns, normalizedXs);
					
					debugPrint(Arrays.toString(xs), g.transpose().multiplyRight(
							DWT.newWaveColumn(waveFactory, ns, normalizedXs)).doubleValue(0));
				}
			}
			
			final DoubleUnaryOperator wave1 = waveFactory.apply(1);
			final DoubleUnaryOperator wave2 = waveFactory.apply(2);
			final DoubleUnaryOperator wave3 = waveFactory.apply(3);
			final DoubleBinaryOperator reconstructed;
			
			if (wave1 instanceof DWT.StepWave) {
				reconstructed = (x0, x1) ->
					wave1.applyAsDouble(0)
					+ wave2.applyAsDouble(x0 / 2.0)
					- wave3.applyAsDouble((x0 - x1) / 3.0) - wave3.applyAsDouble((x0 + x1) / 3.0);
			} else {
				reconstructed = (x0, x1) ->
					-2.0 * wave1.applyAsDouble(0)
					+ 2.0 * (wave2.applyAsDouble(-x1 / 2.0) + wave2.applyAsDouble(x1 / 2.0))
					+ 6.0 * wave2.applyAsDouble(x0 / 2.0)
					- 4.0 * (wave3.applyAsDouble((x0 - x1) / 3.0) + wave3.applyAsDouble((x0 + x1) / 3.0));
			}
			
			for (double x0 = 0.0; x0 <= 1.0; ++x0) {
				for (double x1 = 0.0; x1 <= 1.0; ++x1) {
					debugPrint(x0, x1, reconstructed.applyAsDouble(x0 / 2.0, x1 / 2.0));
				}
			}
			
			{
				final Statistics x0Statistics = new Statistics();
				final Statistics x1Statistics = new Statistics();
				
				x0Statistics.addValue(-0.5);
				x0Statistics.addValue(1.5);
				x1Statistics.addAll(x0Statistics);
				
				final int x0n = 200;
				final int x1n = x0n;
				final DefaultHeatMapDataset heatMapDataset = new DefaultHeatMapDataset(x0n, x1n,
						x0Statistics.getMinimum(), x0Statistics.getMaximum(),
						x1Statistics.getMinimum(), x1Statistics.getMaximum());
				
				for (int x0 = 0; x0 < x0n; ++x0) {
					for (int x1 = 0; x1 < x1n; ++x1) {
						heatMapDataset.setZValue(x0, x1, reconstructed.applyAsDouble(
								x0Statistics.getDenormalizedValue((double) x0 / x0n),
								x0Statistics.getDenormalizedValue((double) x1 / x1n)));
					}
				}
				
				final BufferedImage image = HeatMapUtilities.createHeatMapImage(heatMapDataset, new GrayPaintScale());
				
				{
					final Graphics2D graphics = image.createGraphics();
					
					final int left = (int) (x0Statistics.getNormalizedValue(0.0) * x0n);
					final int right = (int) (x0Statistics.getNormalizedValue(1.0) * x0n);
					final int bottom = (int) (x1Statistics.getNormalizedValue(0.0) * x0n);
					final int top = (int) (x1Statistics.getNormalizedValue(1.0) * x0n);
					final int r = 2;
					final int d = r * 2;
					final int h = x1n - 1;
					
					graphics.setColor(Color.RED);
					graphics.drawOval(left - r, h - bottom - r, d, d);
					graphics.drawOval(right - r, h - top - r, d, d);
					graphics.setColor(Color.GREEN);
					graphics.drawOval(left - r, h - top - r, d, d);
					graphics.drawOval(right - r, h - bottom - r, d, d);
					
					graphics.dispose();
				}
				
				SwingTools.show(image, "XOR/" + wave1.getClass().getSimpleName(), false);
			}
		}
		
		if (false) {
			demo1D();
			demo2D();
			demoND();
		}
	}
	
	public static final JFreeChart plot(final DoubleUnaryOperator f, final double... xs) {
		final double[] ys = Arrays.stream(xs).map(f::applyAsDouble).toArray();
		final Statistics xStatistics = new Statistics();
		final Statistics yStatistics = new Statistics();
		
		Arrays.stream(xs).forEach(xStatistics::addValue);
		Arrays.stream(ys).forEach(yStatistics::addValue);
		
		final DefaultXYDataset dataset = new DefaultXYDataset();
		
		dataset.addSeries("xy", array(xs, ys));
		
		return ChartFactory.createXYLineChart("plot", "x", "y", dataset);
	}
	
	public static final double[] drange(final double first, final double step, final double last) {
		final List<Double> values = new ArrayList<>();
		
		for (double x = first; x <= last; x += step) {
			values.add(x);
		}
		
		return values.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
	public static final void demo1D() {
		final BasicMatrix f = newColumn(1.0, 2.0, -3.0);
		final int n = f.size();
		final BasicMatrix m = newIDWTMatrix(n);
		final BasicMatrix g = m.solve(f);
		
		debugPrint(m);
		debugPrint(f);
		debugPrint(g);
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 0.0 / n)).doubleValue(0));
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 1.0 / n)).doubleValue(0));
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 2.0 / n)).doubleValue(0));
	}
	
	public static final void demo2D() {
		final int n1 = 5;
		final double[] values = Arrays.stream(intRange(n1 * n1)).mapToDouble(x -> x).toArray();
		values[n1 * n1 / 2] *= -1.0;
		final BasicMatrix f = newColumn(values);
		final int n2 = f.size() / n1;
		
		debugPrint(n1, n2);
		
		final BasicMatrix m = newIDWTMatrix(n1, n2);
		
		if (f.size() < 50) {
			debugPrint(m);
		}
		
		final int rank = m.getRank();
		
		if (rank != f.size()) {
			debugError(rank);
		}
		
		final BasicMatrix g = m.solve(f);
		
		debugPrint(f);
		debugPrint(g);
		
		for (int x1 = 0; x1 < n1; ++x1) {
			for (int x2 = 0; x2 < n2; ++x2) {
				debugPrint(x1, x2, g.transpose().multiplyRight(newWaveColumn(n1, n2, (double) x1 / n1, (double) x2 / n2)).doubleValue(0));
			}
		}
	}
	
	public static final void demoND() {
		final int[] ns = { 3, 3, 3 };
		final double[] values = Arrays.stream(intRange(product(ns))).mapToDouble(x -> x).toArray();
		values[values.length / 2] *= -1.0;
		final BasicMatrix f = newColumn(values);
		
		debugPrint(Arrays.toString(ns));
		
		final BasicMatrix m = newIDWTMatrix(ns);
		
		if (f.size() < 50) {
			debugPrint(m);
		}
		
		final int rank = m.getRank();
		
		if (rank != f.size()) {
			debugError(rank);
		}
		
		final BasicMatrix g = m.solve(f);
		
		debugPrint(f);
		debugPrint(g);
		
		{
			final double[] normalizedXs = new double[ns.length];
			
			for (final int[] xs : cartesian(bounds(ns))) {
				normalize(xs, ns, normalizedXs);
				
				debugPrint(Arrays.toString(xs), g.transpose().multiplyRight(newWaveColumn(ns, normalizedXs)).doubleValue(0));
			}
		}
	}
	
	public static final BasicMatrix newColumn(final double... values) {
		final int n = values.length;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, values[i]);
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix newIDWTMatrix(final int n) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		
		for (int x = 0; x < n; ++x) {
			for (int k = 0; k < n; ++k) {
				builder.set(x, k, waveCell((double) x / n, k));
			}
		}
		
		return builder.build();
	}
	
	public static final double waveCell(final double x, final int k) {
		return wave(1 + k, k * x / (1 + k));
	}
	
	public static final BasicMatrix newWaveColumn(final int n, final double x) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int k = 0; k < n; ++k) {
			builder.set(k, waveCell(x, k));
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix newIDWTMatrix(final int[] ns) {
		final int n = product(ns);
		final double[] normalizedXs = new double[ns.length];
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		final Iterable<int[]> tuples = cartesian(bounds(ns));
		int i = -1;
		
		for (final int[] xs : tuples) {
			++i;
			int j = -1;
			
			normalize(xs, ns, normalizedXs);
			
			for (final int[] ks : tuples) {
				++j;
				
				builder.set(i, j, waveCell(normalizedXs, ks));
			}
		}
		
		return builder.build();
	}
	
	public static final void normalize(final int[] input, final int[] ns, final double[] output) {
		final int n = input.length;
		
		for (int i = 0; i < n; ++i) {
			output[i] = (double) input[i] / ns[i];
		}
	}
	
	public static final int[] bounds(final int... sizes) {
		final int n = sizes.length;
		final int[] result = new int[2 * n];
		
		for (int i = 0; i < n; ++i) {
			result[2 * i + 1] = sizes[i] - 1;
		}
		
		return result;
	}
	
	public static final int sum(final int... values) {
		return Arrays.stream(values).sum();
	}
	
	public static final int product(final int... values) {
		return Arrays.stream(values).reduce(1, (x, y) -> x * y);
	}
	
	public static final double waveCell(final double[] xs, final int[] ks) {
		final int m = xs.length;
		final int n = 1 + sum(ks);
		final int count = 1 << (m - 1);
		double result = 0.0;
		
		for (int i = 0; i < count; ++i) {
			double argument = ks[0] * xs[0] / n;
			
			for (int j = 1; j < m; ++j) {
				final int signum = ((i >> (j - 1)) & 1) * 2 - 1;
				argument += signum * ks[j] * xs[j] / n;
			}
			
			result += wave(n, argument);
		}
		
		return result;
	}
	
	public static final BasicMatrix newWaveColumn(final int[] ns, final double[] xs) {
		final int n = product(ns);
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		int i = -1;
		
		for (final int[] ks : cartesian(bounds(ns))) {
			builder.set(++i, 0, waveCell(xs, ks));
		}
		
		return builder.build();
		
	}
	
	public static final BasicMatrix newIDWTMatrix(final int n1, final int n2) {
		final int n = n1 * n2;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		
		for (int x1 = 0, i = 0; x1 < n1; ++x1) {
			for (int x2 = 0; x2 < n2; ++x2, ++i) {
				for (int k1 = 0, j = 0; k1 < n1; ++k1) {
					for (int k2 = 0; k2 < n2; ++k2, ++j) {
						builder.set(i, j, waveCell((double) x1 / n1, k1, (double) x2 / n2, k2));
					}
				}
			}
		}
		
		return builder.build();
	}
	
	public static final double waveCell(final double x1, final int k1, final double x2, final int k2) {
		final int n = 1 + k1 + k2;
		final double xx1 = k1 * x1 / n;
		final double xx2 = k2 * x2 / n;
		
		return wave(n, xx1 + xx2) + wave(n, xx1 - xx2);
	}
	
	public static final BasicMatrix newWaveColumn(final int n1, final int n2, final double x1, final double x2) {
		final int n = n1 * n2;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int k1 = 0, i = 0; k1 < n1; ++k1) {
			for (int k2 = 0; k2 < n2; ++k2, ++i) {
				builder.set(i, waveCell(x1, k1, x2, k2));
			}
		}
		
		return builder.build();
	}
	
	public static final double wave(final int n, final double x) {
		double result = 1.0 - relu(n * x);
		
		for (int k = 1, s = 2; k < n; ++k, s = -s) {
			result += s * relu(n * x - k);
		}
		
		return result;
	}
	
	public static final double relu(final double x) {
		return max(0.0, x);
	}
	
}
