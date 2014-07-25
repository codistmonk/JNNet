package jnnet.apps;

import static java.lang.Math.sqrt;
import static jnnet.apps.ICPRMitos.newEnglishScanner;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageOfBufferedImage;
import imj.ImageOfBufferedImage.Feature;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jnnet.apps.MitosAtypiaImporter.VirtualImage40;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-07-25)
 */
public final class ICPRMitosPostprocess {
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String trainingRoot = arguments.get("trainingRoot", "");
		final String testRoot = arguments.get("test", "");
		final Pattern pattern = Pattern.compile("(.*)(frames.+x40)(.+)");
		final Collection<String> imageBases = ICPRMitos.collectImageBases(trainingRoot);
		final TicToc timer = new TicToc();
		
		debugPrint("Postprocessing...", new Date(timer.tic()));
		
		for (final String imageBase : imageBases) {
			final VirtualImage40 image = new VirtualImage40(imageBase);
			final Matcher matcher = pattern.matcher(imageBase);
			
			if (matcher.matches()) {
				final String rawResultBase = matcher.group(1) + matcher.group(2) + "/mitosis" + matcher.group(3);
				final String csvBase = matcher.group(1) + "mitosis" + matcher.group(3);
				final File maskFile = new File(rawResultBase + "_mask.png");
				final BufferedImage mask = getOrCreateMask(image, rawResultBase, maskFile);
				final Collection<Point> explicitPoints = collectDownscaledPositives(image, csvBase);
				final Image imjMask = new ImageOfBufferedImage(mask, Feature.MAX_RGB);
				
				debugPrint(imjMask.getColumnCount(), imjMask.getRowCount(), explicitPoints.size());
				
				final Statistics patchSize = new Statistics();
				final Statistics truePositivePatchSize = new Statistics();
				final AtomicLong truePositives = new AtomicLong();
				
				IMJTools.forEachPixelInEachComponent4(imjMask, false, new PixelProcessor() {
					
					private final int w = imjMask.getColumnCount();
					
					private final Point point = new Point();
					
					private long patchPixelCount;
					
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
						}
						
						this.patchPixelCount = 0L;
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = -7358350310744302005L;
					
				});
				
				debugPrint("patchCount:", patchSize.getCount(), "meanPatchSize:", patchSize.getMean()
						, "truePositives:", truePositives);
				debugPrint("truePositivePatchSize:", truePositivePatchSize.getMinimum()
						, "<=", truePositivePatchSize.getMean()
						, "#", sqrt(truePositivePatchSize.getVariance())
						, "<=", truePositivePatchSize.getMaximum());
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
