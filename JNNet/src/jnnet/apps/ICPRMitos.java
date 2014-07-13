package jnnet.apps;

import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.lang.Double.parseDouble;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.readObject;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.writeObject;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import jnnet.BinDataset;
import jnnet.BinaryClassifier;
import jnnet.Dataset;
import jnnet.ReorderingDataset;
import jnnet.SimpleConfusionMatrix;
import jnnet.SimplifiedNeuralBinaryClassifier;
import jnnet.apps.MitosAtypiaImporter.VirtualImage40;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.ConsoleMonitor;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TaskManager;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2014-07-04)
 */
public final class ICPRMitos {
	
	private ICPRMitos() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String trainingFileName = arguments.get("training", "");
		final int trainingFolds = arguments.get("trainingFolds", 6)[0];
		final int trainingTestItems = arguments.get("trainingTestItems", 10000)[0];
		final int trainingShuffleChunkSize = arguments.get("trainingShuffleChunkSize", 4)[0];
		final int[] trainingParameters = arguments.get("trainingParameters", 10, 8, 6, 4, 2, 0);
		final double maximumCPULoad = parseDouble(arguments.get("maximumCPULoad", "0.25"));
		final String classifierFileName = arguments.get("classifier", "bestclassifier.jo");
		final String testRoot = arguments.get("test", "");
		final boolean restartTest = arguments.get("testRestart", 0)[0] != 0;
		
		if (!trainingFileName.isEmpty() && false) {
			train(trainingFileName, trainingShuffleChunkSize, trainingFolds, trainingTestItems
					, trainingParameters, classifierFileName, maximumCPULoad);
		}
		
		if (!testRoot.isEmpty()) {
			final BinaryClassifier classifier = readObject(classifierFileName);
			final Collection<String> imageBases = collectImageBases(testRoot);
			final int strideX = 4;
			final int strideY = strideX;
			
			debugPrint(imageBases);
			
			final TaskManager taskManager = new TaskManager(0.75);
			
			for (final String imageBase : imageBases) {
				taskManager.submit(new Runnable() {
					
					@Override
					public final void run() {
						try {
							process(imageBase, strideX, strideY, classifier, restartTest);
						} catch (final IOException exception) {
							throw unchecked(exception);
						}
					}
					
				});
			}
			
			taskManager.join();
		}
	}
	
	public static final int getWindowSize(final BinaryClassifier classifier, final int channelCount) {
		final int result = (int) sqrt(classifier.getInputDimension() / channelCount);
		
		if (result * result * channelCount != classifier.getInputDimension()) {
			throw new IllegalArgumentException("notSquare: " + classifier.getInputDimension() / channelCount);
		}
		
		return result;
	}
	
	public static final void process(final String imageBase, final int strideX, final int strideY
			, final BinaryClassifier classifier, final boolean restart) throws IOException {
		final TicToc timer = new TicToc();
		final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
		final int channelCount = 3;
		final int windowSize = getWindowSize(classifier, channelCount);
		final int windowHalfSize = windowSize / 2;
		final double[] window = new double[windowSize * windowSize * channelCount];
		final VirtualImage40 image = new VirtualImage40(imageBase);
		final String virtualImageName = new File(imageBase).getName();
		
		debugPrint(imageBase, image.getWidth(), image.getHeight());
		
		for (final String quad0 : array("A", "B", "C", "D")) {
			for (final String quad1 : array("a", "b", "c", "d")) {
				final String tileId = quad0 + quad1;
				final String tileFileId = virtualImageName + tileId;
				final String resultPath = new File(imageBase).getParent() + "/mitosis/" + tileFileId + "_mitosis.png";
				final File resultFile = new File(resultPath);
				
				if (!restart && resultFile.isFile()) {
					continue;
				}
				
				debugPrint("Processing tile", tileFileId, "started...", new Date(timer.tic()));
				
				final BufferedImage tile = image.getTile(tileId);
				final int tileWidth = tile.getWidth();
				final int tileHeight = tile.getHeight();
				final BufferedImage tileCopy = new BufferedImage(tileWidth, tileHeight, tile.getType());
				final Graphics2D g = tileCopy.createGraphics();
				
				tile.copyData(tileCopy.getRaster());
				g.setColor(Color.YELLOW);
				
				for (int y = 0; y < tileHeight; y += strideY) {
					for (int x = 0; x < tileWidth; x += strideX) {
						monitor.ping(tileFileId + " " + x + " " + y + "\r");
						
						getPixels(image, tileId, x - windowHalfSize, y - windowHalfSize, window, windowSize);
						
						if (classifier.accept(window)) {
//							debugPrint("Mitosis detected in", tileFileId, "at", x, y);
							g.fillRect(x - strideX / 2, y - strideY / 2, strideX, strideY);
						}
					}
				}
				
				monitor.pause();
				
				debugPrint("Processing tile", tileFileId, "done in", timer.toc(), "ms");
				
				resultFile.getParentFile().mkdirs();
				
				ImageIO.write(tileCopy, "png", resultFile);
			}
		}
	}
	
	public static final void getPixels(final VirtualImage40 source, final String tileId, final int x, final int y
			, final BufferedImage target) {
		for (int yy = 0; yy < target.getHeight(); ++yy) {
			for (int xx = 0; xx < target.getWidth(); ++xx) {
				target.setRGB(xx, yy, source.getRGB(tileId, x + xx, y + yy));
			}
		}
	}
	
	public static final void getPixels(final VirtualImage40 source, final String tileId, final int x, final int y
			, final double[] target, final int windowSize) {
		final int channelCount = 3;
		
		for (int yy = 0; yy < windowSize; ++yy) {
			for (int xx = 0; xx < windowSize; ++xx) {
				final int rgb = source.getRGB(tileId, x + xx, y + yy);
				final int pixelOffsetInTarget = (yy * windowSize + xx) * channelCount;
				target[pixelOffsetInTarget + 0] = red8(rgb);
				target[pixelOffsetInTarget + 1] = green8(rgb);
				target[pixelOffsetInTarget + 2] = blue8(rgb);
			}
		}
	}
	
	public static final Collection<String> collectImageBases(final String rootDirectory) {
		final Collection<String> result = new TreeSet<>();
		
		try {
			Files.walkFileTree(FileSystems.getDefault().getPath(rootDirectory), new SimpleFileVisitor<Path>() {
				
				@Override
				public final FileVisitResult visitFile(final Path file,
						final BasicFileAttributes attrs) throws IOException {
					final String filePath = file.toString();
					
					if (filePath.endsWith(".png") && file.getParent().endsWith("frames/x40")) {
						result.add(filePath.substring(0, filePath.length() - "Aa.png".length()));
					}
					
					return super.visitFile(file, attrs);
				}
				
			});
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		return result;
	}
	
	public static final void train(final String trainingFileName, final int shuffleChunkSize, final int crossValidationFolds
			, final int testItems, final int[] trainingParameters, final String classifierFileName, final double maximumCPULoad) {
		final TicToc timer = new TicToc();
		
		debugPrint("Loading full dataset started", new Date(timer.tic()));
		final BinDataset dataset = new BinDataset(trainingFileName);
		debugPrint("Loading full dataset done in", timer.toc(), "ms");
		
		debugPrint("Shuffling dataset started", new Date(timer.tic()));
		final ReorderingDataset all = new ReorderingDataset(dataset).shuffle(shuffleChunkSize);
		debugPrint("Shuffling dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading test dataset started", new Date(timer.tic()));
		final Dataset testData = all.subset(all.getItemCount() - testItems, testItems);
		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading full training dataset started", new Date(timer.tic()));
		final ReorderingDataset fullTrainingData = all.subset(0, all.getItemCount() - testItems);
		debugPrint("Loading full training dataset done in", timer.toc(), "ms");
		
		final int validationItems = crossValidationFolds == 1 ? 0
				: (all.getItemCount() - testItems) / crossValidationFolds;
		final List<Dataset[]> trainingValidationPairs = new ArrayList<>(crossValidationFolds);
		
		for (int fold = 1; fold <= crossValidationFolds; ++fold) {
			fullTrainingData.swapFolds(0, fold - 1, crossValidationFolds);
			
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading training dataset started", new Date(timer.tic()));
			final Dataset trainingData = fullTrainingData.subset(validationItems, fullTrainingData.getItemCount() - validationItems);
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading training dataset done in", timer.toc(), "ms");
			
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading validation dataset started", new Date(timer.tic()));
			final Dataset validationData = validationItems == 0 ? trainingData : fullTrainingData.subset(0, validationItems);
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading validation dataset done in", timer.toc(), "ms");
			
			trainingValidationPairs.add(array(trainingData, validationData));
			
			fullTrainingData.swapFolds(0, fold - 1, crossValidationFolds);
		}
		
		final int[] bestClassifierParameter = { 0 };
		final double[] bestSensitivity = { 0.0 };
		final TaskManager taskManager = new TaskManager(maximumCPULoad);
		
		for (final int classifierParameter : trainingParameters) {
			taskManager.submit(new Runnable() {
				
				@Override
				public final void run() {
					debugPrint("classifierParameter:", classifierParameter);
					
					final Statistics sensitivity = new Statistics();
					int fold = 1;
					
					for (final Dataset[] trainingValidationPair : trainingValidationPairs) {
						final Dataset trainingData = trainingValidationPair[0];
						final Dataset validationData = trainingValidationPair[1];
						
						debugPrint("classifierParameter:", classifierParameter);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Building classifier started", new Date(timer.tic()));
						final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(
								trainingData, 0.5, classifierParameter / 100.0, 100, true, true);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Building classifier done in", timer.toc(), "ms");
						
						debugPrint("classifierParameter:", classifierParameter);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on training set started", new Date(timer.tic()));
						debugPrint("fold:", fold + "/" + crossValidationFolds, "training:", classifier.evaluate(trainingData, null));
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on training set done in", timer.toc(), "ms");
						
						debugPrint("classifierParameter:", classifierParameter);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on validation set started", new Date(timer.tic()));
						final SimpleConfusionMatrix validationResult = classifier.evaluate(validationData, null);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "validation:", validationResult);
						debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on validation set done in", timer.toc(), "ms");
						
						sensitivity.addValue(validationResult.getSensitivity());
						++fold;
					}
					
					debugPrint("classifierParameter:", classifierParameter, "sensitivity:", sensitivity.getMinimum() + "<=" + sensitivity.getMean() + "(" + sqrt(sensitivity.getVariance()) + ")<=" + sensitivity.getMaximum());
					
					synchronized (bestSensitivity) {
						if (bestSensitivity[0] < sensitivity.getMean()) {
							bestSensitivity[0] = sensitivity.getMean();
							bestClassifierParameter[0] = classifierParameter;
						}
					}
				}
				
			});
		}
		
		taskManager.join();
		
		if (bestClassifierParameter[0] != 0) {
			debugPrint("bestClassifierParameter:", bestClassifierParameter[0]);
			
			{
				debugPrint("Building test classifier started", new Date(timer.tic()));
				final SimplifiedNeuralBinaryClassifier bestClassifier = new SimplifiedNeuralBinaryClassifier(
						fullTrainingData, 0.5, bestClassifierParameter[0] / 100.0, 100, true, true);
				debugPrint("Building test classifier done in", timer.toc(), "ms");
				
				debugPrint("Evaluating test classifier on training set started", new Date(timer.tic()));
				debugPrint("training:", bestClassifier.evaluate(fullTrainingData, null));
				debugPrint("Evaluating test classifier on training set done in", timer.toc(), "ms");
				
				debugPrint("Evaluating test classifier on test set started", new Date(timer.tic()));
				final SimpleConfusionMatrix testResult = bestClassifier.evaluate(testData, null);
				debugPrint("validation:", testResult);
				debugPrint("Evaluating test classifier on test set done in", timer.toc(), "ms");
			}
			
			{
				debugPrint("Building best classifier started", new Date(timer.tic()));
				final SimplifiedNeuralBinaryClassifier bestClassifier = new SimplifiedNeuralBinaryClassifier(
						all, 0.5, bestClassifierParameter[0] / 100.0, 100, true, true);
				debugPrint("Building best classifier done in", timer.toc(), "ms");
				
				debugPrint("Serializing best classifier started", new Date(timer.tic()));
				writeObject(bestClassifier, classifierFileName);
				debugPrint("Serializing best classifier done in", timer.toc(), "ms");
			}
		}
	}

}