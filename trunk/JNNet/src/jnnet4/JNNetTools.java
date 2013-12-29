package jnnet4;

import static java.lang.Math.exp;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-12-21)
 */
public final class JNNetTools {
	
	private JNNetTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final double[] doubles(final double... values) {
		return values;
	}
	
	public static final double[] inputs(final double... values) {
		return values;
	}
	
	public static final double[] outputs(final double... values) {
		return values;
	}
	
//	public static final void draw(final Training training, final BufferedImage image) {
//		if (0 == training.getItems().length) {
//			return;
//		}
//		
//		final int inputCount = training.getItems()[0].getInputs().length;
//		final int outputCount = training.getItems()[0].getOutputs().length;
//		
//		if (2 == inputCount) {
//			for (final Item item : training.getItems()) {
//				final int w = image.getWidth();
//				final int h = image.getHeight();
//				final int x = (int) item.getInputs()[0];
//				final int y = h - 1 - (int) item.getInputs()[1];
//				final int rgb;
//				
//				if (1 == outputCount) {
//					rgb = 0x00010101 * uint8(item.getOutputs()[0]);
//				} else if (3 == outputCount) {
//					final int red = uint8(item.getOutputs()[0]);
//					final int green = uint8(item.getOutputs()[1]);
//					final int blue = uint8(item.getOutputs()[2]);
//					rgb = (red << 16) | (green << 8) | blue;
//				} else {
//					throw new IllegalArgumentException();
//				}
//				
//				final int reverseRGB = ~rgb;
//				
//				image.setRGB(x, y, 0xFF000000 | rgb);
//				
//				if (0 < y) {
//					image.setRGB(x, y - 1, 0xFF000000 | reverseRGB);
//				}
//				
//				if (0 < x) {
//					image.setRGB(x - 1, y, 0xFF000000 | reverseRGB);
//				}
//				
//				if (x + 1 < w) {
//					image.setRGB(x + 1, y, 0xFF000000 | reverseRGB);
//				}
//				
//				if (y+ 1 < h) {
//					image.setRGB(x, y + 1, 0xFF000000 | reverseRGB);
//				}
//			}
//		} else {
//			throw new IllegalArgumentException();
//		}
//	}
	
//	public static final void draw(final ArtificialNeuralNetwork ann, final BufferedImage image) {
//		final int inputCount = ann.getInputCount();
//		
//		if (2 == inputCount) {
//			final int w = image.getWidth();
//			final int h = image.getHeight();
//			final int outputCount = ann.getOutputNeurons().length;
//			
//			if (1 == outputCount) {
//				for (int y = 0; y < h; ++y) {
//					for (int x = 0; x < w; ++x) {
//						ann.evaluate(x, h - 1 - y);
//						image.setRGB(x, y, 0xFF000000 | (0x00010101 * uint8(ann.getOutputValue(0))));
//					}
//				}
//			} else if (3 == outputCount) {
//				for (int y = 0; y < h; ++y) {
//					for (int x = 0; x < w; ++x) {
//						ann.evaluate(x, h - 1 - y);
//						final int red = uint8(ann.getOutputValue(0));
//						final int green = uint8(ann.getOutputValue(1));
//						final int blue = uint8(ann.getOutputValue(2));
//						image.setRGB(x, y, 0xFF000000 | (red << 16) | (green << 8) | blue);
//					}
//				}
//			} else {
//				throw new IllegalArgumentException();
//			}
//		} else {
//			throw new IllegalArgumentException();
//		}
//	}
	
	public static final int uint8(final double valueBetween0And1) {
		return (int) (255.0 * valueBetween0And1);
	}
	
	public static final Field getDeclaredField(final Class<?> cls, final String name) {
		try {
			return cls.getDeclaredField(name);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void add(final Object object, final Field array, final double value) {
		try {
			array.setAccessible(true);
			
			final double[] oldArray = (double[]) array.get(object);
			final int n = oldArray.length;
			final double[] newArray = copyOf(oldArray, n + 1);
			newArray[n] = value;
			array.set(object, newArray);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void add(final Object object, final Field array, final int value) {
		try {
			array.setAccessible(true);
			
			final int[] oldArray = (int[]) array.get(object);
			final int n = oldArray.length;
			final int[] newArray = copyOf(oldArray, n + 1);
			newArray[n] = value;
			array.set(object, newArray);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final double sigmoid(final double x) {
		return 1.0 / (1.0 + exp(-x));
	}
	
	public static final double dSigmoid(final double x) {
		final double s = sigmoid(x);
		
		return s * (1.0 - s);
	}
	
//	public static final ArtificialNeuralNetwork newNetwork(final int inputCount, final int... layers) {
//		final ArtificialNeuralNetwork result = new ArtificialNeuralNetwork(inputCount);
//		final Random random = new Random(inputCount + Arrays.hashCode(layers));
//		
//		final int layerCount = layers.length;
//		int sourceOffset = 1;
//		int sourceSize = inputCount;
//		
//		for (int i = 0, layerSize = layers[i]; i < layerCount; sourceOffset += sourceSize, sourceSize = layerSize, ++i) {
//			layerSize = layers[i];
//			
//			for (int j = 0; j < layerSize; ++j) {
//				final double[] weights = newValues(random, 1 + sourceSize);
//				
//				if (i < layerCount - 1) {
//					result.addNeuron(sourceOffset, weights);
//				} else {
//					result.addOutputNeuron(sourceOffset, weights);
//				}
//			}
//		}
//		
//		return result;
//	}
	
	public static final double[] newValues(final Random random, final int n) {
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = (random.nextDouble() - 0.5) * 2.0;
		}
		
		return result;
	}
	
}
