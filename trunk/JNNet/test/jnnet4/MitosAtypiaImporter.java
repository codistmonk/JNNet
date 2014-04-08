package jnnet4;

import static java.lang.Double.parseDouble;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-04-07)
 */
public final class MitosAtypiaImporter {
	
	private MitosAtypiaImporter() {
		throw new IllegalInstantiationException();
	}
	
	private static final MosaicBuilder[] mitosisMosaicBuilder = instances(2, DefaultFactory.forClass(MosaicBuilder.class));
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final Operation operation = Operation.TEST_DATA_FILE;
		final String root = "F:/icpr2014_mitos_atypia/";
		final int windowHalfSize = 32;
		
		if (Operation.TEST_PNG.equals(operation)) {
			debugPrint(ImageIO.read(new File(root, "A18/frames/x40/A18_00Cc.png")));
		} else if (Operation.MAKE_DATA_FILE.equals(operation)) {
			final PrintStream out = new PrintStream(new File(root, "A.data"));
			
			try {
				for (final String imageSetId : array("A03", "A04", "A05", "A07", "A10", "A11", "A12", "A14", "A15", "A17", "A18")) {
					debugPrint(new Date());
					
					final String imageSetRoot = root + imageSetId;
					final File frames40 = new File(imageSetRoot + "/frames/x40/");
					final File mitosis = new File(imageSetRoot + "/mitosis/");
					final Map<String, VirtualImage40> images = new HashMap<String, VirtualImage40>();
					
					try {
						for (final String file : frames40.list()) {
							final String tileId = file.replaceAll("\\..+$", "");
							final String imageBasePath = new File(frames40, tileId.substring(0, tileId.length() - 2)).toString();
							
							debugPrint(tileId, imageBasePath);
							
							final VirtualImage40 image = getOrCreate(images, imageBasePath,
									new DefaultFactory<VirtualImage40>(VirtualImage40.class, imageBasePath));
							
							debugPrint(image.getWidth(), image.getHeight());
							
							convert(image, tileId, new File(mitosis, tileId + "_mitosis.csv"), windowHalfSize, out);
							convert(image, tileId, new File(mitosis, tileId + "_not_mitosis.csv"), windowHalfSize, out);
						}
					} finally {
						ImageIO.write(mitosisMosaicBuilder[0].generateMosaic(), "png", new File(mitosis, "not_mitosis_mosaic.png"));
						ImageIO.write(mitosisMosaicBuilder[1].generateMosaic(), "png", new File(mitosis, "mitosis_mosaic.png"));
					}
				}
			} finally {
				out.close();
				
				debugPrint(new Date());
			}
		} else if (Operation.TEST_DATA_FILE.equals(operation)) {
			debugPrint(new Date());
			
			final ConsoleMonitor monitor = new ConsoleMonitor(5000L);
//			final Scanner scanner = new Scanner(Tools.getResourceAsStream(root + "A.data"));
			final BufferedReader scanner = new BufferedReader(new FileReader(root + "A.data"));
			final Map<Integer, AtomicInteger> lineSizes = new TreeMap<Integer, AtomicInteger>();
			final DefaultFactory<AtomicInteger> atomicIntegerFactory = DefaultFactory.forClass(AtomicInteger.class);
			
			try {
				int lineCount = 0;
				
				String l = scanner.readLine();
				
				while (l != null) {
					monitor.ping();
					final String[] line = l.trim().split(" ");
					final int lineSize = line.length;
					getOrCreate(lineSizes, lineSize, atomicIntegerFactory).incrementAndGet();
					++lineCount;
					
					if (lineSize != (int) (square(windowHalfSize * 2.0) * 3.0 + 1.0)) {
						monitor.pause();
						System.err.println(debug(DEBUG_STACK_OFFSET, lineCount, lineSize, Arrays.toString(line)));
					}
					
					l = scanner.readLine();
				}
				
				monitor.pause();
				debugPrint(lineCount, lineSizes);
			} finally {
				scanner.close();
				
				monitor.pause();
				debugPrint(new Date());
			}
		}
	}
	
	public static final void convert(final VirtualImage40 image, final String tileId, final File csv, final int windowHalfSize, final PrintStream out) throws FileNotFoundException {
		final ConsoleMonitor monitor = new ConsoleMonitor(5000L);
		final Collection<Point> positivePoints = new ArrayList<Point>();
		final Scanner scanner = new Scanner(csv);
		
		try {
			while (scanner.hasNext()) {
				monitor.ping();
				
				final String[] line = scanner.nextLine().split(",");
				final int x0 = (int) parseDouble(line[0]);
				final int y0 = (int) parseDouble(line[1]);
				final double score = parseDouble(line[2]);
				final int label = score < 0.5 ? 0 : 1;
				
				mitosisMosaicBuilder[label].getImages().add(convert(image, tileId,
						windowHalfSize, x0, y0, label, out));
				
				if (label == 1) {
					positivePoints.add(new Point(x0, y0));
				}
			}
			
			if (!csv.getName().endsWith("_not_mitosis.csv")){
				final BufferedImage tile = image.getTile(tileId);
				final int w = tile.getWidth();
				final int h = tile.getHeight();
				final int xStep = w / 4;
				final int yStep = h / 4;
				
				for (int y = windowHalfSize; y + windowHalfSize <= h; y += yStep) {
					for (int x = windowHalfSize; x + windowHalfSize <= w; x += xStep) {
						monitor.ping();
						
						boolean ok = true;
						
						for (final Point point : positivePoints) {
							if (point.distance(x, y) <= 2.0 * windowHalfSize) {
								ok = false;
								break;
							}
						}
						
						if (ok) {
							convert(image, tileId, windowHalfSize, x, y, 0, out);
						}
					}
				}
			}
		} finally {
			scanner.close();
			monitor.pause();
		}
	}
	
	private static final BufferedImage convert(final VirtualImage40 image,
			final String tileId, final int windowHalfSize, final int x0,
			final int y0, final int label, final PrintStream out) {
		final BufferedImage result = new BufferedImage(2 * windowHalfSize, 2 * windowHalfSize, BufferedImage.TYPE_3BYTE_BGR);
		
		for (int y = y0 - windowHalfSize; y < y0 + windowHalfSize; ++y) {
			for (int x = x0 - windowHalfSize; x < x0 + windowHalfSize; ++x) {
				final int rgb = image.getRGB(tileId, x, y);
				
				result.setRGB(x - (x0 - windowHalfSize), y - (y0 - windowHalfSize), rgb);
			}
		}
		
		// 0°
		{
			for (int y = 0; y < 2 * windowHalfSize; ++y) {
				for (int x = 0; x < 2 * windowHalfSize; ++x) {
					final Color color = new Color(result.getRGB(x, y));
					
					out.print(color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " ");
				}
			}
			
			out.println(label);
		}
		
		// 90°
		{
			for (int x = 0; x < 2 * windowHalfSize; ++x) {
				for (int y = 2 * windowHalfSize - 1; 0 <= y; --y) {
					final Color color = new Color(result.getRGB(x, y));
					
					out.print(color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " ");
				}
			}
			
			out.println(label);
		}
		
		// 180°
		{
			for (int y = 2 * windowHalfSize - 1; 0 <= y; --y) {
				for (int x = 2 * windowHalfSize - 1; 0 <= x; --x) {
					final Color color = new Color(result.getRGB(x, y));
					
					out.print(color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " ");
				}
			}
			
			out.println(label);
		}
		
		// 270°
		{
			for (int x = 2 * windowHalfSize - 1; 0 <= x; --x) {
				for (int y = 0; y < 2 * windowHalfSize; ++y) {
					final Color color = new Color(result.getRGB(x, y));
					
					out.print(color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " ");
				}
			}
			
			out.println(label);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-30)
	 */
	public static final class VirtualImage40 implements Serializable {
		
		private final String basePath;
		
		private final BufferedImage[][] tiles;
		
		private final int width;
		
		private final int height;
		
		public VirtualImage40(final String basePath) {
			this.basePath = basePath;
			this.tiles = new BufferedImage[4][4];
			
			for (int quad0 = 0; quad0 <= 3; ++quad0) {
				for (int quad1 = 0; quad1 <= 3; ++quad1) {
					this.tiles[quad0][quad1] = readTile(quad0, quad1);
				}
			}
			
			this.width = this.tiles[0][0].getWidth() + this.tiles[0][1].getWidth() + this.tiles[1][0].getWidth() + this.tiles[1][1].getWidth();
			this.height = this.tiles[0][0].getHeight() + this.tiles[0][2].getHeight() + this.tiles[2][0].getHeight() + this.tiles[2][2].getHeight();
		}
		
		public final int getWidth() {
			return this.width;
		}
		
		public final int getHeight() {
			return this.height;
		}
		
		public final BufferedImage getTile(final String tileId) {
			final int quad0 = tileId.charAt(tileId.length() - 2) - 'A';
			final int quad1 = tileId.charAt(tileId.length() - 1) - 'a';
			
			return this.tiles[quad0][quad1];
		}
		
		public final int getRGB(final String tileId, final int x, final int y) {
			int quad0 = tileId.charAt(tileId.length() - 2) - 'A';
			int quad1 = tileId.charAt(tileId.length() - 1) - 'a';
			
			BufferedImage tile = this.tiles[quad0][quad1];
			int w = tile.getWidth();
			int h = tile.getHeight();
			
			try {
				if (x < 0) {
					if (quad1 == 1 || quad1 == 3) {
						--quad1;
					} else {
						++quad1;
						
						if (quad0 == 1 || quad0 == 3) {
							--quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				} else if (w <= x) {
					if (quad1 == 0 || quad1 == 2) {
						++quad1;
					} else {
						--quad1;
						
						if (quad0 == 0 || quad0 == 2) {
							++quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				}
				
				if (y < 0) {
					if (quad1 == 2 || quad1 == 3) {
						--quad1;
					} else {
						++quad1;
						
						if (quad0 == 2 || quad0 == 3) {
							--quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				} else if (h <= y) {
					if (quad1 == 0 || quad1 == 1) {
						++quad1;
					} else {
						--quad1;
						
						if (quad0 == 0 || quad0 == 1) {
							++quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				}
			} catch (final IllegalArgumentException exception) {
				ignore(exception);
				
				System.err.println(debug(DEBUG_STACK_OFFSET, tileId, x, y));
				
				return 0;
			}
			
			tile = this.tiles[quad0][quad1];
			w = tile.getWidth();
			h = tile.getHeight();
			
			return tile.getRGB((w + x) % w, (h + y) % h);
		}
		
		private final BufferedImage readTile(final int quad0, final int quad1) {
			final File file = new File(this.basePath + (char) ('A' + quad0) + "" + (char) ('a' + quad1) + ".png");
			
			try {
				return ImageIO.read(file);
			} catch (final IOException exception) {
				System.err.println(debug(DEBUG_STACK_OFFSET, "Error reading file", file));
				throw unchecked(exception);
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 5357895005657732226L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-07)
	 */
	public static final class ConsoleMonitor implements Serializable {
		
		private final TicToc timer;
		
		private final long periodMilliseconds;
		
		private final AtomicBoolean newLineNeeded;
		
		public ConsoleMonitor(final long periodMilliseconds) {
			this.timer = new TicToc();
			this.periodMilliseconds = periodMilliseconds;
			this.newLineNeeded = new AtomicBoolean();
			this.timer.tic();
		}
		
		public final void ping() {
			if (this.periodMilliseconds <= this.timer.toc()) {
				System.out.print('.');
				this.newLineNeeded.set(true);
				this.timer.tic();
			}
		}
		
		public final void pause() {
			if (this.newLineNeeded.getAndSet(false)) {
				System.out.println();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3669736743010335592L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-08)
	 */
	public static enum Operation {
		
		TEST_PNG, MAKE_DATA_FILE, TEST_DATA_FILE
		
	}
	
}
