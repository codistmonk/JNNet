package jnnet4;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static jnnet4.JNNetTools.rgb;
import static jnnet4.LinearConstraintSystemTest.LinearConstraintSystem20140325.unscale;
import static net.sourceforge.aprog.tools.SystemProperties.getAvailableProcessorCount;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.readObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import jnnet4.LinearConstraintSystemTest.LinearConstraintSystem;
import jnnet4.LinearConstraintSystemTest.LinearConstraintSystem20140414;
import jnnet4.LinearConstraintSystemTest.OjAlgoLinearConstraintSystem;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-04-13)
 */
public final class InvertClassifier {
	
	private InvertClassifier() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final SimplifiedNeuralBinaryClassifier classifier = readObject(arguments.get("classifier", ""));
		final String outPath = arguments.get("out", "mosaic.png");
		final int channelCount = arguments.get("channels", 3)[0];
		final int maximumClusterCount = arguments.get("clusters", 9)[0];
		final BufferedImage mosaic = invert(classifier, channelCount, maximumClusterCount).generateMosaic();
		
		ImageIO.write(mosaic, "png", new File(outPath));
	}
	
	public static final MosaicBuilder invert(final SimplifiedNeuralBinaryClassifier classifier, final int channelCount, final int maximumClusterCount) {
		final TicToc timer = new TicToc();
		
		debugPrint("Inverting classifier started", new Date(timer.tic()));
		
		final MosaicBuilder result = new MosaicBuilder();
		final int inputDimension = classifier.getInputDimension();
		final int w = (int) sqrt(inputDimension / channelCount);
		final int h = w;
		final double[] hyperplanes = classifier.getHyperplanes();
		final ExecutorService executor = Executors.newFixedThreadPool(min(6, getAvailableProcessorCount()));
		
		try {
			int remaining = maximumClusterCount;
			
			final AtomicInteger i = new AtomicInteger();
			
			for (final BitSet code : classifier.getClusters()) {
				if (--remaining < 0) {
					break;
				}
				
				executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						debugPrint(Thread.currentThread(), i.getAndIncrement());
						
						final double[] example;
						
						try {
							example = invert(hyperplanes, inputDimension, code);
						} catch (final Throwable exception) {
							exception.printStackTrace();
							return;
						}
						
						final BufferedImage image;
						
						if (channelCount == 1) {
							image = newImage(example, 1, w, h);
						} else if (channelCount == 3) {
							image = newImageRGB(example, 1, w, h);
						} else {
							throw new IllegalArgumentException();
						}
						
						synchronized (result) {
							result.getImages().add(image);
						}
					}
					
				});
			}
			
			executor.shutdown();
			
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException exception) {
			exception.printStackTrace();
		} finally {
			executor.shutdown();
		}
		
		debugPrint("Inverting classifier done in", timer.toc(), "ms");
		
		return result;
	}
	
	public static final double[] invert(final double[] hyperplanes, final int inputDimension, final BitSet code) {
		final int step = inputDimension + 1;
		final int n = hyperplanes.length;
		
		final LinearConstraintSystem system = new LinearConstraintSystem20140414(step);
//		final LinearConstraintSystem system = new OjAlgoLinearConstraintSystem(step);
		
		{
			final double[] constraint = new double[step];
			
			if (!(system instanceof OjAlgoLinearConstraintSystem)) {
				system.allocate(step + step - 1 + n / step);
				
				for (int i = 0; i < step; ++i) {
					constraint[i] = 1.0;
					system.addConstraint(constraint);
					constraint[i] = 0.0;
				}
			}
			
			constraint[0] = 255.0;
			
			for (int i = 1; i < step; ++i) {
				constraint[i] = -1.0;
				system.addConstraint(constraint);
				constraint[i] = 0.0;
			}
		}
		
		for (int i = 0, bit = 0; i < n; i += step, ++bit) {
			final double scale = code.get(bit) ? 1.0 : -1.0;
			final double[] constraint = new double[step];
			
			for (int j = i; j < i + step; ++j) {
				constraint[j - i] = scale * hyperplanes[j];
			}
			
			system.addConstraint(constraint);
		}
		
		final double[] example = unscale(system.solve());
		
		debugPrint(system.accept(example));
		
		return example;
	}
	
	public static final BufferedImage newImage(final double[] example, final int offset, final int w, final int h) {
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		
		for (int y = 0, p = offset; y < h; ++y) {
			for (int x = 0; x < w; ++x, ++p) {
				image.setRGB(x, y, rgb(max(0.0, min(example[p] / example[0] / 255.0, 1.0))));
			}
		}
		
		return image;
	}
	
	public static final BufferedImage newImageRGB(final double[] example, final int offset, final int w, final int h) {
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		
		for (int y = 0, p = offset; y < h; ++y) {
			for (int x = 0; x < w; ++x, p += 3) {
				image.setRGB(x, y, rgb(
						unscale255To01(example, p + 0),
						unscale255To01(example, p + 1),
						unscale255To01(example, p + 2)));
			}
		}
		
		return image;
	}
	
	public static final double unscale255To01(final double[] values, final int index) {
		return values[index] / values[0] / 255.0;
	}
	
}
