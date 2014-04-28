package nsphere;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import jgencode.primitivelists.DoubleList;
import jnnet.draft.LinearConstraintSystem;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2014-04-23)
 */
public final class NSphereDemo {
	
	private NSphereDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final int width = 512;
		final int height = width;
		final int centerX = width / 2;
		final int centerY = height / 2;
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final Random random = new Random(0L);
		final int sx = 100;
		final int sy = 20;
		final Graphics2D g = image.createGraphics();
		final double[] expectedAngle = { PI / 3.0 };
		final DoubleList samples = new DoubleList();
		
		for (int i = 0; i < 1000; ++i) {
			final double randX = random.nextGaussian();
			final double randY = random.nextGaussian();
			final double dx = sx * randX * cos(expectedAngle[0]) - sy * randY * sin(expectedAngle[0]);
			final double dy = sy * randY * sin(expectedAngle[0]) + sx * randX * cos(expectedAngle[0]);
			final int x = (int) (centerX + dx);
			final int y = (int) (centerY + dy);
			
			g.fillOval(x - 2, height - 1 - y - 2, 4, 4);
			samples.addAll(dx, dy);
		}
		
		{
			final double[] actualAngle = { 0.0 };
			final double[] bestAngle = actualAngle.clone();
			double bestVariance = 0.0;
			final int n = 1000;
			
			for (int i = 0; i < n; ++i) {
				actualAngle[0] = i * 2.0 * PI / n;
				final double variance = varianceOfProjection(samples.toArray(), nDirection(actualAngle));
				
				if (bestVariance < variance) {
					bestVariance = variance;
					System.arraycopy(actualAngle, 0, bestAngle, 0, actualAngle.length);
				}
			}
			
			debugPrint("expected:", Arrays.toString(expectedAngle), "actual:", Arrays.toString(bestAngle));
			
			g.setColor(Color.RED);
			g.drawLine(centerX, centerY, (int) (centerX + sx * cos(bestAngle[0])), (int) (height - 1 - centerY - sx * sin(bestAngle[0])));
		}
		
		g.dispose();
		
		show(image, NSphereDemo.class.getSimpleName(), false);
	}
	
	public static final double nextGaussian(final Random random) {
		final double signum = random.nextDouble() < 0.5 ? -1.0 : 1.0;
		final double y = random.nextDouble();
		
		return signum * sqrt(-log(y));
	}
	
	public static final double varianceOfProjection(final double[] samples, final double[] axis) {
		final int n = samples.length;
		final int dimension = axis.length;
		final Statistics statistics = new Statistics();
		
		for (int i = 0; i < n; i += dimension) {
			statistics.addValue(LinearConstraintSystem.Abstract.dot(samples, i, axis, 0, dimension));
		}
		
		return statistics.getVariance();
	}
	
	public static final double[] nDirection(final double... angles) {
		final int dimension = angles.length + 1;
		final double[] result = new double[dimension];
		final int last = dimension - 1;
		double sins = 1.0;
		
		for (int i = 0; i < last; ++i) {
			result[i] = sins * cos(angles[i]);
			sins *= sin(angles[i]);
		}
		
		result[last] = sins;
		
		return result;
	}
	
}
