package jnnet.apps;

import static java.lang.Math.sqrt;
import static jnnet.VectorStatistics.STATISTICS_FACTORY;
import static jnnet.apps.ICPRMitos.newEnglishScanner;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.TREE_MAP_FACTORY;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import imj.IMJTools;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageOfBufferedImage;
import imj.ImageOfBufferedImage.Feature;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jnnet.VectorStatistics;
import jnnet.apps.MitosAtypiaImporter.VirtualImage40;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-07-25)
 */
public final class ICPRMitosPostprocess {
	
	private ICPRMitosPostprocess() {
		throw new IllegalInstantiationException();
	}
	
	public static final DefaultFactory<AtomicInteger> ATOMIC_INTEGER_FACTORY = DefaultFactory.forClass(AtomicInteger.class);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String trainingRoot = arguments.get("trainingRoot", "");
		final String testRoot = arguments.get("test", "");
		final Pattern pattern = Pattern.compile("(.*)(frames.+x40)(.+)");
		final TicToc timer = new TicToc();
		final Map<String, Object> progress = ICPRMitos.getOrCreateProgress("postprocessingProgress.jo");
		final Map<Integer, AtomicInteger> sizeHistogram = (Map<Integer, AtomicInteger>) getOrCreate(
				progress, "sizeHistogram", (Factory) TREE_MAP_FACTORY);
		final Map<Integer, AtomicInteger> truePositiveSizeHistogram = (Map<Integer, AtomicInteger>) getOrCreate(
				progress, "truePositiveSizeHistogram", (Factory) TREE_MAP_FACTORY);
		final Statistics truePositivePatchSize = (Statistics) getOrCreate(
				progress, "truePositivePatchSize", (Factory) STATISTICS_FACTORY);
		
		debugPrint("Postprocessing...", new Date(timer.tic()));
		
		if (sizeHistogram.isEmpty()) {
			final Collection<String> imageBases = ICPRMitos.collectImageBases(trainingRoot);
			
			for (final String imageBase : imageBases) {
				final VirtualImage40 image = new VirtualImage40(imageBase);
				final Matcher matcher = pattern.matcher(imageBase);
				final Statistics patchSize = new Statistics();
				
				if (matcher.matches()) {
					final String rawResultBase = matcher.group(1) + matcher.group(2) + "/mitosis" + matcher.group(3);
					final String csvBase = matcher.group(1) + "mitosis" + matcher.group(3);
					final File maskFile = new File(rawResultBase + "_mask.png");
					final BufferedImage mask = getOrCreateMask(image, rawResultBase, maskFile);
					final Collection<Point> explicitPoints = collectDownscaledPositives(image, csvBase);
					final Image imjMask = new ImageOfBufferedImage(mask, Feature.MAX_RGB);
					
					debugPrint(imjMask.getColumnCount(), imjMask.getRowCount(), explicitPoints.size());
					
					final AtomicLong truePositives = new AtomicLong();
					
					IMJTools.forEachPixelInEachComponent4(imjMask, false, new PixelProcessor() {
						
						private final int w = imjMask.getColumnCount();
						
						private final Point point = new Point();
						
						private int patchPixelCount;
						
						private boolean truePositive;
						
						@Override
						public final void process(final int pixel) {
							this.point.x = pixel % this.w;
							this.point.y = pixel / this.w;
							
							if (explicitPoints.contains(this.point)) {
								truePositives.incrementAndGet();
								this.truePositive = true;
							}
							
							++this.patchPixelCount;
						}
						
						@Override
						public final void finishPatch() {
							patchSize.addValue(this.patchPixelCount);
							
							if (this.truePositive) {
								truePositivePatchSize.addValue(this.patchPixelCount);
								getOrCreate(truePositiveSizeHistogram, this.patchPixelCount, ATOMIC_INTEGER_FACTORY).incrementAndGet();
								this.truePositive = false;
							}
							
							getOrCreate(sizeHistogram, this.patchPixelCount, ATOMIC_INTEGER_FACTORY).incrementAndGet();
							
							this.patchPixelCount = 0;
						}
						
						/**
						 * {@value}.
						 */
						private static final long serialVersionUID = -7358350310744302005L;
						
					});
					
					debugPrint("patchCount:", patchSize.getCount(), "meanPatchSize:", patchSize.getMean()
							, "truePositives:", truePositives);
				}
			}
			
			ICPRMitos.writeSafely((Serializable) progress, "postprocessingProgress.jo");
		}
		
		debugPrint(truePositivePatchSize.getCount());
		debugPrint("truePositivePatchSize:", truePositivePatchSize.getMinimum()
				, "<=", truePositivePatchSize.getMean()
				, "#", sqrt(truePositivePatchSize.getVariance())
				, "<=", truePositivePatchSize.getMaximum());
		debugPrint("truePositiveSizeHistogram:", truePositiveSizeHistogram);
		debugPrint("sizeHistogram:", sizeHistogram);
		
		final Integer[] sizes = sizeHistogram.keySet().toArray(new Integer[sizeHistogram.size()]);
		final int sizeCount = sizes.length;
		final AtomicInteger zero = new AtomicInteger();
		final double[] bestScore = new double[5];
		final double alpha = 0.05;
		final double beta = 1.0 - alpha;
		
		for (int i = 0; i < sizeCount; ++i) {
			for (int j = i; j < sizeCount; ++j) {
				double truePositiveCount = 0.0;
				double count = 0.0;
				
				for (int k = i; k <= j; ++k) {
					truePositiveCount += truePositiveSizeHistogram.getOrDefault(sizes[k], zero).get();
					count += sizeHistogram.getOrDefault(sizes[k], zero).get();
				}
				
				final double score = alpha * truePositiveCount / truePositivePatchSize.getCount()
						+ beta * truePositiveCount / count;
				
				if (bestScore[0] < score) {
					bestScore[0] = score;
					bestScore[1] = sizes[i];
					bestScore[2] = sizes[j];
					bestScore[3] = truePositiveCount;
					bestScore[4] = count;
				}
			}
		}
		
		debugPrint(Arrays.toString(bestScore));
		
		{
			final int minimumSize = (int) bestScore[1];
			final int maximumSize = (int) bestScore[2];
			final Collection<String> imageBases = ICPRMitos.collectImageBases(testRoot);
			final Map<String, Collection<Point>> mitoses = new TreeMap<>();
			
			for (final String imageBase : imageBases) {
				final VirtualImage40 image = new VirtualImage40(imageBase);
				final int tileWidth = image.getWidth() / 4;
				final int tileHeight = image.getHeight() / 4;
				final Matcher matcher = pattern.matcher(imageBase);
				
				if (matcher.matches()) {
					final String rawResultBase = matcher.group(1) + matcher.group(2) + "/mitosis" + matcher.group(3);
					final File maskFile = new File(rawResultBase + "_mask.png");
					final BufferedImage mask = getOrCreateMask(image, rawResultBase, maskFile);
					final Image imjMask = new ImageOfBufferedImage(mask, Feature.MAX_RGB);
					
					debugPrint(imjMask.getColumnCount(), imjMask.getRowCount());
					
					for (char q0 = 'A'; q0 <= 'D'; ++q0) {
						for (char q1 = 'a'; q1 <= 'd'; ++q1) {
							mitoses.put(imageBase + q0 + q1, new HashSet<>());
						}
					}
					
					IMJTools.forEachPixelInEachComponent4(imjMask, false, new PixelProcessor() {
						
						private final int w = imjMask.getColumnCount();
						
						private int x;
						
						private int y;
						
						private int patchPixelCount;
						
						@Override
						public final void process(final int pixel) {
							this.x += pixel % this.w;
							this.y += pixel / this.w;
							++this.patchPixelCount;
						}
						
						@Override
						public final void finishPatch() {
							if (minimumSize <= this.patchPixelCount && this.patchPixelCount <= maximumSize) {
								this.x /= this.patchPixelCount;
								this.y /= this.patchPixelCount;
								// (00 00) (01 00) (10 00) (11 00)
								// (00 01) (01 01) (10 01) (11 01)
								// (00 10) (01 10) (10 10) (11 10)
								// (00 11) (01 11) (10 11) (11 11)
								final int qx = (this.x * 4) / tileWidth;
								final int qy = (this.y * 4) / tileHeight;
								final char q0 = (char) ('A' + ((qx & 2) >> 1) + ((qy & 2) >> 0));
								final char q1 = (char) ('a' + ((qx & 1) << 0) + ((qy & 1) << 1));
								
								mitoses.get(imageBase + q0 + q1).add(new Point(this.x, this.y));
							}
							
							this.x = 0;
							this.y = 0;
							this.patchPixelCount = 0;
						}
						
						/**
						 * {@value}.
						 */
						private static final long serialVersionUID = 2769568076132020491L;
						
					});
					
					for (char q0 = 'A'; q0 <= 'D'; ++q0) {
						for (char q1 = 'a'; q1 <= 'd'; ++q1) {
							final String key = imageBase + q0 + q1;
							
							debugPrint(key, mitoses.get(key).size());
						}
					}
				}
			}
		}
		
		debugPrint("Postprocessing done in", timer.toc(), "ms");
	}
	
	public static final Collection<Point> collectDownscaledPositives(
			final VirtualImage40 image, final String csvBase) {
		final int tileWidth = image.getWidth() / 4;
		final int tileHeight = image.getHeight() / 4;
		final Collection<Point> result = new ArrayList<>();
		
		for (char q0 = 'A'; q0 <= 'D'; ++q0) {
			for (char q1 = 'a'; q1 <= 'd'; ++q1) {
				final File mitosisFile = new File(csvBase + q0 + q1 + "_mitosis.csv");
				final File notMitosisFile = new File(csvBase + q0 + q1 + "_not_mitosis.csv");
				final int tileX = 2 * tileWidth * ((q0 - 'A') & 1) + tileWidth * ((q1 - 'a') & 1);
				final int tileY = 2 * tileHeight * ((q0 - 'A') >> 1) + tileHeight * ((q1 - 'a') >> 1);
				
				for (final File file : array(mitosisFile, notMitosisFile)) {
					debugPrint(file, file.exists(), image.getWidth(), image.getHeight());
					
					try (final Scanner scanner = newEnglishScanner(file)) {
						while (scanner.hasNext()) {
							try (final Scanner lineScanner = newEnglishScanner(scanner.nextLine())) {
								lineScanner.useDelimiter(",");
								
								final int x = lineScanner.nextInt();
								final int y = lineScanner.nextInt();
								final double label = lineScanner.nextDouble() < 0.5 ? 0.0 : 1.0;
								
								if (0.5 <= label) {
									result.add(new Point((tileX + x) / 4, (tileY + y) / 4));
								}
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public static final BufferedImage getOrCreateMask(final VirtualImage40 image,
			final String rawResultBase, final File maskFile) throws IOException {
		final int tileWidth = image.getWidth() / 4;
		final int tileHeight = image.getHeight() / 4;
		final BufferedImage result;
		
		if (!maskFile.exists()) {
			result = new BufferedImage(image.getWidth() / 4, image.getHeight() / 4, BufferedImage.TYPE_BYTE_BINARY);
			
			for (char q0 = 'A'; q0 <= 'D'; ++q0) {
				for (char q1 = 'a'; q1 <= 'd'; ++q1) {
					final File classifiedTileFile = new File(rawResultBase + q0 + q1 + "_mitosis.png");
					
					debugPrint(classifiedTileFile);
					
					final BufferedImage classifiedTile = ImageIO.read(classifiedTileFile);
					final int tileX = 2 * tileWidth * ((q0 - 'A') & 1) + tileWidth * ((q1 - 'a') & 1);
					final int tileY = 2 * tileHeight * ((q0 - 'A') >> 1) + tileHeight * ((q1 - 'a') >> 1);
					
					for (int y = 0; y < tileHeight; y += 4) {
						for (int x = 0; x < tileWidth; x += 4) {
							if (classifiedTile.getRGB(x, y) == 0xFFFFFF00) {
								result.setRGB((tileX + x) / 4
										, (tileY + y) / 4, 0xFFFFFFFF);
							}
						}
					}
				}
			}
			
			ImageIO.write(result, "png", maskFile);
		} else {
			debugPrint("Reading", maskFile);
			result = ImageIO.read(maskFile);
		}
		
		return result;
	}
	
}
