package jnnet4;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.fill;
import static java.util.Collections.sort;
import static java.util.Collections.swap;
import static jnnet4.FeedforwardNeuralNetworkTest.intersection;
import static jnnet4.JNNetTools.RANDOM;
import static jnnet4.JNNetTools.uint8;
import static jnnet4.ProjectiveClassifier.preview;
import static jnnet4.VectorStatistics.add;
import static jnnet4.VectorStatistics.dot;
import static jnnet4.VectorStatistics.scaled;
import static jnnet4.VectorStatistics.subtract;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import jnnet.DoubleList;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class SimplifiedNeuralBinaryClassifierTest {
	
	@Test
	public final void test() {
		final boolean showClassifier = true;
		final boolean previewTrainingData = true;
		final boolean previewValidationData = true;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/gisette/gisette_train.data");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
//		final Dataset trainingData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 64), "Training data", false);
		}
		
		debugPrint("Building classifier started", new Date(timer.tic()));
		final BinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 2000, true, true);
		debugPrint("Building classifier done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
		final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData);
		debugPrint("training:", confusionMatrix);
		debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
		
//		debugPrint("Loading validation dataset started", new Date(timer.tic()));
//		final Dataset validationData = new Dataset("../Libraries/datasets/gisette/gisette_valid.data");
//		debugPrint("Loading validation dataset done in", timer.toc(), "ms");
//		
//		if (previewValidationData) {
//			SwingTools.show(preview(validationData, 8), "Validation data", false);
//		}
//		
//		debugPrint("Evaluating classifier on validation set started", new Date(timer.tic()));
//		debugPrint("test:", classifier.evaluate(validationData));
//		debugPrint("Evaluating classifier on validation set done in", timer.toc(), "ms");
		
//		debugPrint("Loading test dataset started", new Date(timer.tic()));
//		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
////		final Dataset testData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 5000000-500000, 500000);
//		debugPrint("Loading test dataset done in", timer.toc(), "ms");
//		
//		debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
//		debugPrint("test:", classifier.evaluate(testData));
//		debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
		
		if (showClassifier && classifier.getInputDimension() == 2) {
			show(classifier, 256, 16.0, trainingData.getData());
		}
		
		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
	public static final void show(final BinaryClassifier classifier, final int imageSize, final double scale, final double[] trainingData) {
		final TicToc timer = new TicToc();
		debugPrint("Allocating rendering buffer started", new Date(timer.tic()));
		final BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_3BYTE_BGR);
		debugPrint("Allocating rendering buffer done in", timer.toc(), "ms");
		
		debugPrint("Rendering started", new Date(timer.tic()));
		
		draw(classifier, image, scale);
		
		if (trainingData != null) {
			final Graphics2D g = image.createGraphics();
			final int inputDimension = classifier.getInputDimension();
			
			for (int i = 0; i < trainingData.length; i += inputDimension + 1) {
				final double label = trainingData[i + inputDimension];
				
				if (label < 0.5) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.GREEN);
				}
				
				g.drawOval(
						imageSize / 2 + (int) (trainingData[i + 0] * scale) - 2,
						imageSize / 2 - (int) (trainingData[i + 1] * scale) - 2,
						4, 4);
			}
			
			g.dispose();
		}
		
		debugPrint("Rendering done in", timer.toc(), "ms");
		
		SwingTools.show(image, getCallerClass().getName(), true);
	}
	
	public static final void draw(final BinaryClassifier classifier, final BufferedImage image, final double scale) {
		final int inputDimension = classifier.getInputDimension();
		
		if (inputDimension != 1 && inputDimension != 2) {
			throw new IllegalArgumentException();
		}
		
		final int w = image.getWidth();
		final int h = image.getHeight();
		final double[] input = new double[inputDimension];
		
		for (int y = 0; y < h; ++y) {
			if (1 < inputDimension) {
				input[1] = (h / 2.0 - y) / scale;
			}
			
			for (int x = 0; x < w; ++x) {
				input[0] = (x - w / 2.0) / scale;
				final double output = classifier.accept(input) ? 1.0 : 0.0;
				
				if (inputDimension == 1) {
					final int yy = (int) (h / 2 - scale * output);
					image.setRGB(x, yy, Color.WHITE.getRGB());
				} else if (inputDimension == 2) {
					image.setRGB(x, y, 0xFF000000 | (JNNetTools.uint8(output) * 0x00010101));
				}
			}
			
			if (inputDimension == 1) {
				break;
			}
		}
	}
	
}

/**
 * @author codistmonk (creation 2014-03-10)
 */
final class SimplifiedNeuralBinaryClassifier implements BinaryClassifier {
	
	private final int inputDimension;
	
	private final double[] hyperplanes;
	
	private final Collection<BitSet> clusters;
	
	private final boolean invertOutput;
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset) {
		this(trainingDataset, Integer.MAX_VALUE, true, true);
	}
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset, final int maximumHyperplaneCount,
			final boolean allowHyperplanePruning, final boolean allowOutputInversion) {
		debugPrint("Partitioning...");
		
		final DoubleList hyperplanes = new DoubleList();
		final int step = trainingDataset.getStep();
		
		generateHyperplanes(trainingDataset, new HyperplaneHandler() {

			@Override
			public final boolean hyperplane(final double bias, final double[] weights) {
				hyperplanes.add(bias);
				hyperplanes.addAll(weights);
				
				return hyperplanes.size() / step < maximumHyperplaneCount;
			}
			
			/**
			 * {@values}.
			 */
			private static final long serialVersionUID = 664820514870575702L;
			
		});
		
		final TicToc timer = new TicToc();
		final double[] data = trainingDataset.getData();
		final int dataLength = data.length;
		@SuppressWarnings("unchecked")
		final Collection<BitSet>[] codes = instances(2, HASH_SET_FACTORY);
		this.inputDimension = step - 1;
		int hyperplaneCount = hyperplanes.size() / step;
		
		debugPrint("hyperplaneCount:", hyperplaneCount);
		debugPrint("Clustering...");
		
		timer.tic();
		
		for (int i = 0; i < dataLength; i += step) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", dataLength);
				timer.tic();
			}
			
			final double[] item = copyOfRange(data, i, i + step - 1);
			final int label = (int) data[i + step - 1];
			final BitSet code = encode(item, hyperplanes.toArray());
			
			codes[label].add(code);
		}
		
		debugPrint("0-codes:", codes[0].size(), "1-codes:", codes[1].size());
		
		{
			final int ambiguities = intersection(codes[0], codes[1]).size();
			
			if (0 < ambiguities) {
				System.err.println(debug(Tools.DEBUG_STACK_OFFSET, "ambiguities:", ambiguities));
				
				codes[1].removeAll(codes[0]);
			}
		}
		
		if (allowHyperplanePruning) {
			debugPrint("Pruning...");
			
			final BitSet markedHyperplanes = new BitSet(hyperplaneCount);
			final int algo = 0;
			
			if (algo == 0) {
				final Collection<BitSet>[] newCodes = codes.clone();
				
				for (int bit = 0; bit < hyperplaneCount; ++bit) {
					@SuppressWarnings("unchecked")
					final Set<BitSet>[] simplifiedCodes = instances(2, HASH_SET_FACTORY);
					
					for (int i = 0; i < 2; ++i) {
						for (final BitSet code : newCodes[i]) {
							final BitSet simplifiedCode = (BitSet) code.clone();
							
							simplifiedCode.clear(bit);
							simplifiedCodes[i].add(simplifiedCode);
						}
					}
					
					final Set<BitSet> ambiguities = intersection(simplifiedCodes[0], simplifiedCodes[1]);
					
					if (ambiguities.isEmpty()) {
						markedHyperplanes.set(bit);
						System.arraycopy(simplifiedCodes, 0, newCodes, 0, 2);
					}
				}
				
				final int oldHyperplaneCount = hyperplaneCount;
				hyperplaneCount -= markedHyperplanes.cardinality();
				
				for (int i = 0; i < 2; ++i) {
					codes[i].clear();
					
					for (final BitSet newCode : newCodes[i]) {
						final BitSet code = new BitSet(hyperplaneCount);
						
						for (int oldBit = 0, newBit = 0; oldBit < oldHyperplaneCount; ++oldBit) {
							if (!markedHyperplanes.get(oldBit)) {
								code.set(newBit++, newCode.get(oldBit));
							}
						}
						
						codes[i].add(code);
					}
				}
			} else {
				final int codeSize = hyperplaneCount;
				final int codeCount = codes[0].size() + codes[1].size();
				final BitSet[] allCodes = new BitSet[codes[0].size() + codes[1].size()];
				final BitSet allLabels = new BitSet(codeCount);
				final BitSet bitVault = new BitSet(codeSize);
				final List<Integer> indices = range(0, codeCount - 1);
				
				{
					int j = 0;
					
					for (int i = 0; i < 2; ++i) {
						for (final BitSet code : codes[i]) {
							allCodes[j++] = code;
						}
					}
					
					allLabels.set(codes[0].size(), codeCount);
				}
				
				for (int bit = 0; bit < hyperplaneCount; ++bit) {
					for (int i = 0; i < codeCount; ++i) {
						final BitSet code = allCodes[i];
						bitVault.set(i, code.get(bit));
						code.clear(bit);
					}
					
					sort(indices, new Comparator<Integer>() {
						
						@Override
						public final int compare(final Integer i1, final Integer i2) {
							final BitSet code1 = allCodes[i1];
							final BitSet code2 = allCodes[i2];
							
							for (int i = 0; i < codeSize; ++i) {
								final boolean b1 = code1.get(i);
								final boolean b2 = code2.get(i);
								
								if (b1 && !b2) {
									return 1;
								}
								
								if (!b1 && b2) {
									return -1;
								}
							}
							
							return 0;
						}
						
					});
					
					boolean removeHyperplane = true;
					
					for (int i = 1; i < allCodes.length && removeHyperplane; ++i) {
						final int previous = indices.get(i - 1);
						final int current = indices.get(i);
						
						if (allCodes[previous].equals(allCodes[current]) && allLabels.get(previous) != allLabels.get(current)) {
							removeHyperplane = false;
						}
					}
					
					if (removeHyperplane) {
						markedHyperplanes.set(bit);
					} else {
						for (int i = 0; i < codeCount; ++i) {
							allCodes[i].set(bit, bitVault.get(i));
						}
					}
				}
				
				final int oldHyperplaneCount = hyperplaneCount;
				hyperplaneCount -= markedHyperplanes.cardinality();
				
				for (int i = 0; i < 2; ++i) {
					codes[i].clear();
				}
				
				for (int i = 0; i < codeCount; ++i) {
					final BitSet code = allCodes[i];
					final BitSet newCode = new BitSet(hyperplaneCount);
					
					for (int oldBit = 0, newBit = 0; oldBit < oldHyperplaneCount; ++oldBit) {
						if (!markedHyperplanes.get(oldBit)) {
							newCode.set(newBit++, code.get(oldBit));
						}
					}
					
					final int label = allLabels.get(i) ? 1 : 0;
					codes[label].add(newCode);
				}
			}
			
			removeHyperplanes(markedHyperplanes, hyperplanes, step);
			
			debugPrint("hyperplaneCount:", hyperplanes.size() / step);
			debugPrint("0-codes:", codes[0].size(), "1-codes:", codes[1].size());
		}
		
		this.hyperplanes = hyperplanes.toArray();
		this.invertOutput = allowOutputInversion && codes[0].size() < codes[1].size();
		this.clusters = this.invertOutput ? codes[0] : codes[1];
	}
	
	private static final void removeHyperplanes(final BitSet markedHyperplanes, final DoubleList hyperplanes, final int step) {
		final double[] data = hyperplanes.toArray();
		final int n = hyperplanes.size();
		
		for (int i = 0, j = 0, bit = 0; i < n; i += step, ++bit) {
			if (!markedHyperplanes.get(bit)) {
				System.arraycopy(data, i, data, j, step);
				j += step;
			}
		}
		
		hyperplanes.resize(n - markedHyperplanes.cardinality() * step);
	}
	
	@Override
	public final int getInputDimension() {
		return this.inputDimension;
	}
	
	public final double[] getHyperplanes() {
		return this.hyperplanes;
	}
	
	public final Collection<BitSet> getClusters() {
		return this.clusters;
	}
	
	public final BitSet encode(final double[] item) {
		return encode(item, this.getHyperplanes());
	}
	
	public static final BitSet encode(final double[] item, final double[] hyperplanes) {
		final int weightCount = hyperplanes.length;
		final int step = item.length + 1;
		final int hyperplaneCount = weightCount / step;
		final BitSet code = new BitSet(hyperplaneCount);
		
		for (int j = 0, bit = 0; j < weightCount; j += step, ++bit) {
			final double d = hyperplanes[j] + dot(item, copyOfRange(hyperplanes, j + 1, j + step));
			
			code.set(bit, 0.0 <= d);
		}
		
		return code;
	}
	
	@Override
	public final boolean accept(final double... item) {
		return this.getClusters().contains(this.encode(item)) ^ this.invertOutput;
	}
	
	@Override
	public final SimpleConfusionMatrix evaluate(final Dataset trainingData) {
		final TicToc timer = new TicToc();
		final SimpleConfusionMatrix result = new SimpleConfusionMatrix();
		final int step = trainingData.getStep();
		final double[] data = trainingData.getData();
		
		timer.tic();
		
		for (int i = 0; i < data.length; i += step) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", data.length);
				timer.tic();
			}
			
			final double expected = data[i + step - 1];
			final double actual = this.accept(copyOfRange(data, i, i + step - 1)) ? 1.0 : 0.0;
			
			if (expected != actual) {
				if (expected == 1.0) {
					result.getFalseNegativeCount().incrementAndGet();
				} else {
					result.getFalsePositiveCount().incrementAndGet();
				}
			} else {
				if (expected == 1.0) {
					result.getTruePositiveCount().incrementAndGet();
				} else {
					result.getTrueNegativeCount().incrementAndGet();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -6740686339638862795L;
	
	public static final Method CLONE_ELEMENTS = Functional.method(SimplifiedNeuralBinaryClassifier.class, "cloneElements", Collection.class);
	
	@SuppressWarnings("unchecked")
	public static final <T> Collection<T> cloneElements(final Collection<T> elements) {
		try {
			final Collection<T> result = elements.getClass().newInstance();
			
			for (final T element : elements) {
				result.add((T) Functional.CLONE.invoke(element));
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final static void generateHyperplanes(final Dataset trainingData, final HyperplaneHandler hyperplaneHandler) {
		final int step = trainingData.getStep();
		final int inputDimension = step - 1;
		final int itemCount = trainingData.getItemCount();
		final double[] data = trainingData.getData();
		final List<List<Integer>> todo = new ArrayList<List<Integer>>();
		final Factory<VectorStatistics> vectorStatisticsFactory = DefaultFactory.forClass(VectorStatistics.class, inputDimension);
		boolean continueProcessing = true;
		final TicToc timer = new TicToc();
		
		timer.tic();
		todo.add(range(0, itemCount - 1));
		
		while (!todo.isEmpty() && continueProcessing) {
			if (LOGGING_MILLISECONDS < timer.toc()) {
				debugPrint("remainingClusters:", todo.size());
				timer.tic();
			}
			
			final List<Integer> indices = todo.remove(0);
			
			final VectorStatistics[] statistics = instances(2, vectorStatisticsFactory);
			
			for (final int i : indices) {
				final int sampleOffset = i * step;
				final int labelOffset = sampleOffset + step - 1;
				statistics[(int) data[labelOffset]].addValues(copyOfRange(data, sampleOffset, labelOffset));
			}
			
			if (statistics[0].getCount() == 0 || statistics[1].getCount() == 0) {
				continue;
			}
			
			final double[] cluster0 = statistics[0].getMeans();
			final double[] cluster1 = statistics[1].getMeans();
			final double[] neuronWeights = subtract(cluster1, cluster0);
			
			if (Arrays.equals(cluster0, cluster1)) {
				for (int i = 0; i < inputDimension; ++i) {
					neuronWeights[i] = RANDOM.nextDouble();
				}
			}
			
			final double[] neuronLocation = scaled(add(cluster1, cluster0), 0.5);
			final double neuronBias = -dot(neuronWeights, neuronLocation);
			
			continueProcessing = hyperplaneHandler.hyperplane(neuronBias, neuronWeights);
			
			{
				final int m = indices.size();
				int j = 0;
				
				for (int i = 0; i < m; ++i) {
					final int sampleOffset = indices.get(i) * step;
					final double d = dot(neuronWeights, copyOfRange(data, sampleOffset, sampleOffset + inputDimension)) + neuronBias;
					
					if (0 <= d) {
						swap(indices, i, j++);
					}
				}
				
				if (0 < j && j < m) {
					todo.add(indices.subList(0, j));
					todo.add(indices.subList(j, m));
				}
			}
		}
	}
	
	public static final List<Integer> range(final int first, final int last) {
		final List<Integer> result = new ArrayList<Integer>(last - first + 1);
		
		for (int i = first; i <= last; ++i) {
			result.add(i);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-10)
	 */
	public static abstract interface HyperplaneHandler extends Serializable {
		
		public abstract boolean hyperplane(double bias, double[] weights);
		
	}
	
}

/**
 * @author codistmonk (creation 2014-03-10)
 */
final class Functional {
	
	private Functional() {
		throw new IllegalInstantiationException();
	}
	
	public static final Method CLONE = method(Object.class, "clone");
	
	@SuppressWarnings("unchecked")
	public static final <In, T> Collection<T>[] map(final In[] methodObjects, final Method method) {
		return map(Collection.class, methodObjects, method);
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, T> Collection<T>[] map(final Object methodObject, final Method method, final In[] singleArguments) {
		return map(Collection.class, methodObject, method, singleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> Collection<T>[] map(final Object methodObject, final Method method, final Object[][] multipleArguments) {
		return map(Collection.class, methodObject, method, multipleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out> Out[] map(final Class<Out> resultComponentType,
			final In[] methodObjects, final Method method) {
		try {
			final int n = methodObjects.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObjects[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out> Out[] map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final In[] singleArguments) {
		try {
			final int n = singleArguments.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObject, singleArguments[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <Out> Out[] map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final Object[][] multipleArguments) {
		try {
			final int n = multipleArguments.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObject, multipleArguments[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Method method(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
		Method result = null;
		
		try {
			result = cls.getMethod(methodName, parameterTypes);
		} catch (final Exception exception) {
			ignore(exception);
		}
		
		if (result == null) {
			try {
				result = cls.getDeclaredMethod(methodName, parameterTypes);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		result.setAccessible(true);
		
		return result;
	}
	
}
