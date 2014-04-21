package jnnet;

import static java.lang.Math.exp;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.invoke;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-12-21)
 */
public final class JNNetTools {
	
	private JNNetTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final Random RANDOM = new Random(0L);
	
	public static final DefaultFactory<AtomicInteger> ATOMIC_INTEGER_FACTORY = DefaultFactory.forClass(AtomicInteger.class);
	
	public static final double[] doubles(final double... values) {
		return values;
	}
	
	public static final double[] inputs(final double... values) {
		return values;
	}
	
	public static final double[] outputs(final double... values) {
		return values;
	}
	
	public static final byte[] pack(final byte[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final int[] pack(final int[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final double[] pack(final double[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final byte[] reserve(final byte[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
	public static final int[] reserve(final int[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
	public static final double[] reserve(final double[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
	public static final int uint8(final double valueBetween0And1) {
		return (int) (255.0 * valueBetween0And1);
	}
	
	public static final int rgb(final double valueBetween0And1) {
		return 0xFF000000 | (0x00010101 * uint8(valueBetween0And1));
	}
	
	public static final int rgb(final double redBetween0And1, final double greenBetween0And1, final double blueBetween0And1) {
		return 0xFF000000 | (uint8(redBetween0And1) << 16) | (uint8(greenBetween0And1) << 8) | (uint8(blueBetween0And1) << 0);
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
	
	public static final int[] irange(final int n) {
		final int[] result = new int[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = i;
		}
		
		return result;
	}
	
	public static final void swap(final int[] array, final int i, final int j) {
		final int tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}
	
	public static final <T, C extends Collection<T>> C intersection(final C s1, final C s2) {
		final C result = invoke(s1, "clone");
		
		result.retainAll(s2);
		
		return result;
	}
	
}
