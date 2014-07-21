package jnnet.draft;

import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.ConsoleMonitor;
import jgencode.primitivelists.IntList;
import jnnet.Dataset;

/**
 * @author codistmonk (creation 2014-05-11)
 */
public final class ImageDataset implements Dataset {
	
	private final String imageId;
	
	private transient BufferedImage image;
	
	private final int windowHalfSize;
	
	private final int itemSize;
	
	private final BitSet pixels;
	
	private final BitSet labels;
	
	private final AtomicLong constructionTimestamp;
	
	private final AtomicLong usageTimestamp;
	
	private final IntList pixelAndLabels;
	
	private final DatasetStatistics statistics;
	
	private final List<TileTransformer> tileTransformers;
	
	public ImageDataset(final String imageId, final int windowHalfSize) {
		this(imageId, readImage(imageId), windowHalfSize);
	}
	
	public ImageDataset(final BufferedImage image, final int windowHalfSize) {
		this("", image, windowHalfSize);
	}
	
	public ImageDataset(final String imageId, final BufferedImage image, final int windowHalfSize) {
		this.imageId = imageId;
		this.image = image;
		this.windowHalfSize = windowHalfSize;
		final int tileSize = windowHalfSize * 2;
		this.itemSize = tileSize * tileSize * 3 + 1;
		this.pixels = new BitSet();
		this.labels = new BitSet();
		this.constructionTimestamp = new AtomicLong();
		this.usageTimestamp = new AtomicLong();
		this.pixelAndLabels = new IntList();
		this.statistics = new DatasetStatistics(this.itemSize - 1);
		this.tileTransformers = new ArrayList<>();
		
		this.tileTransformers.add(TileTransformer.Predefined.ID);
		this.tileTransformers.add(new TileRotation90(tileSize));
		this.tileTransformers.add(new TileRotation180(tileSize));
		this.tileTransformers.add(new TileRotation270(tileSize));
	}
	
	public final String getImageId() {
		return this.imageId;
	}
	
	public final boolean contains(final int x, final int y) {
		return this.contains(this.getPixel(x, y));
	}
	
	public final boolean contains(final int pixel) {
		return this.pixels.get(pixel);
	}
	
	public final int getPixelLabel(final int x, final int y) {
		return this.getPixelLabel(this.getPixel(x, y));
	}
	
	public final int getPixelLabel(final int pixel) {
		return this.labels.get(pixel) ? 1 : 0;
	}
	
	public final int getPixel(final int x, final int y) {
		return y * this.getImage().getWidth() + x;
	}
	
	public final int[] getPixelAndLabels() {
		final long timestamp = this.constructionTimestamp.get();
		
		if (this.usageTimestamp.getAndSet(timestamp) != timestamp) {
			final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
			
			this.pixelAndLabels.resize(this.pixels.cardinality());
			this.pixelAndLabels.clear();
			this.getStatistics().reset();
			
			final int w = this.getImage().getWidth();
			final int n = w * this.getImage().getHeight();
			
			for (int pixel = 0; pixel < n; ++pixel) {
				monitor.ping((pixel + 1) + " / " + n + "\r");
				
				if (this.contains(pixel)) {
					final int x = pixel % w;
					final int y = pixel / w;
					final int label = this.getPixelLabel(pixel);
					
					this.pixelAndLabels.addAll(pixel, label);
					
					for (final TileTransformer tileTransformer : this.tileTransformers) {
						this.getStatistics().addItem(item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize(), label));
					}
				}
			}
			
			monitor.pause();
			
			this.getStatistics().printTo(System.out);
		}
		
		return this.pixelAndLabels.toArray();
	}
	
	public final BufferedImage getImage() {
		if (this.image == null) {
			this.image = readImage(this.getImageId());
		}
		
		return this.image;
	}
	
	public final int getWindowHalfSize() {
		return this.windowHalfSize;
	}
	
	public final DatasetStatistics getStatistics() {
		return this.statistics;
	}
	
	public final ImageDataset reset() {
		this.pixelAndLabels.clear();
		this.getStatistics().reset();
		
		return this;
	}
	
	public final ImageDataset addPixelItems(final int x, final int y, final int label) {
		if (!this.canContain(x, y)) {
			return this;
		}
		
		return this.addPixelItems(this.getPixel(x, y), label);
	}
	
	public final ImageDataset addPixelItems(final int pixel, final int label) {
		this.constructionTimestamp.incrementAndGet();
		
		this.pixels.set(pixel);
		this.labels.set(pixel, label == 1);
		
		return this;
	}
	
	public final ImageDataset removePixelItems(final int x, final int y) {
		if (!this.canContain(x, y)) {
			return this;
		}
		
		return this.removePixelItems(this.getPixel(x, y));
	}
	
	public final boolean canContain(final int x, final int y) {
		return 0 <= x && x < this.getImage().getWidth() && 0 <= y && y < this.getImage().getHeight();
	}
	
	public final ImageDataset removePixelItems(final int pixel) {
		this.constructionTimestamp.incrementAndGet();
		
		this.pixels.clear(pixel);
		
		return this;
	}
	
	@Override
	public final int getItemCount() {
		return this.pixels.cardinality() * this.tileTransformers.size();
	}
	
	@Override
	public final int getItemSize() {
		return this.itemSize;
	}
	
	@Override
	public final double getItemValue(final int itemId, final int valueId) {
		final TileTransformer tileTransformer =
				this.tileTransformers.get(itemId % this.tileTransformers.size());
		final int untransformedItemId = itemId / this.tileTransformers.size();
		final int center = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
		final int imageWidth = this.getImage().getWidth();
		final int imageHeight = this.getImage().getHeight();
		final int tileSize = this.getWindowHalfSize() * 2;
		final int xInTile = (valueId / 3) % tileSize;
		final int yInTile = (valueId / 3) / tileSize;
		final int x = (center % imageWidth) - this.getWindowHalfSize()
				+ tileTransformer.transformXInTile(xInTile, yInTile);
		final int y = (center / imageWidth) - this.getWindowHalfSize()
				+ tileTransformer.transformYInTile(xInTile, yInTile);
		
		if (x < 0 || imageWidth <= x || y < 0 || imageHeight <= y) {
			return 0.0;
		}
		
		final int rgb = tileTransformer.transformRGB(this.getImage().getRGB(x, y));
		
		switch (valueId % 3) {
		case 0:
			return red8(rgb);
		case 1:
			return green8(rgb);
		case 2:
			return blue8(rgb);
		}
		
		throw new IllegalStateException();
	}
	
	@Override
	public final double[] getItem(final int itemId, final double[] result) {
		final TileTransformer tileTransformer =
				this.tileTransformers.get(itemId % this.tileTransformers.size());
		final int untransformedItemId = itemId / this.tileTransformers.size();
		final int pixel = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
		final int label = this.getPixelAndLabels()[untransformedItemId * 2 + 1];
		final int imageWidth = this.getImage().getWidth();
		final int x = pixel % imageWidth;
		final int y = pixel / imageWidth;
		
		return item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize(), label, result);
	}
	
	@Override
	public final double[] getItemWeights(final int itemId) {
		final TileTransformer tileTransformer =
				this.tileTransformers.get(itemId % this.tileTransformers.size());
		final int untransformedItemId = itemId / this.tileTransformers.size();
		final int pixel = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
		final int imageWidth = this.getImage().getWidth();
		final int x = pixel % imageWidth;
		final int y = pixel / imageWidth;
		
		return item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize()
				, new double[this.getItemSize() - 1]);
	}
	
	@Override
	public final double getItemLabel(final int itemId) {
		return this.getPixelAndLabels()[itemId / this.tileTransformers.size() * 2 + 1];
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -8970225174892151354L;
	
	public static final double[] item(final BufferedImage image, final TileTransformer tileTransformer
			, final int x, final int y, final int windowHalfSize, final double label) {
		final int order = windowHalfSize * windowHalfSize * 4 * 3 + 1;
		final double[] result = item(image, tileTransformer, x, y, windowHalfSize, new double[order]);
		
		result[order - 1] = label;
		
		return result;
	}
	
	public static final double[] item(final BufferedImage image, final TileTransformer tileTransformer
			, final int x, final int y, final int windowHalfSize, final double label, final double[] result) {
		item(image, tileTransformer, x, y, windowHalfSize, result);
		
		result[result.length - 1] = label;
		
		return result;
	}
	
	public static final double[] item(final BufferedImage image, final TileTransformer tileTransformer
			, final int x, final int y, final int windowHalfSize, final double[] result) {
		final int xStart = x - windowHalfSize;
		final int yStart = y - windowHalfSize;
		final int w = image.getWidth();
		final int h = image.getHeight();
		final int tileEnd = 2 * windowHalfSize;
		
		Arrays.fill(result, 0.0);
		
		for (int yInTile = 0, i = 0; yInTile < tileEnd; ++yInTile) {
			for (int xInTile = 0; xInTile < tileEnd; ++xInTile, i += 3) {
				final int xx = xStart + tileTransformer.transformXInTile(xInTile, yInTile);
				final int yy = yStart + tileTransformer.transformYInTile(xInTile, yInTile);
				
				if (0 <= xx && xx < w && 0 <= yy && yy < h) {
					final int rgb = tileTransformer.transformRGB(image.getRGB(xx, yy));
					
					result[i + 0] = red8(rgb);
					result[i + 1] = green8(rgb);
					result[i + 2] = blue8(rgb);
				}
			}
		}
		
		return result;
	}
	
	public static final BufferedImage readImage(final String imageId) {
		try {
			return ImageIO.read(getResourceAsStream(imageId));
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static abstract class TileRotation implements TileTransformer {
		
		private final int tileSize;
		
		protected TileRotation(final int tileSize) {
			this.tileSize = tileSize;
		}
		
		public final int getTileSize() {
			return this.tileSize;
		}
		
		@Override
		public final int transformRGB(final int rgb) {
			return rgb;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4151623294048501227L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class TileRotation90 extends TileRotation {
		
		public TileRotation90(final int tileSize) {
			super(tileSize);
		}
		
		@Override
		public final int transformXInTile(final int xInTile, final int yInTile) {
			return this.getTileSize() - yInTile;
		}
		
		@Override
		public final int transformYInTile(final int xInTile, final int yInTile) {
			return xInTile;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1450451672704459382L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class TileRotation180 extends TileRotation {
		
		public TileRotation180(final int tileSize) {
			super(tileSize);
		}
		
		@Override
		public final int transformXInTile(final int xInTile, final int yInTile) {
			return this.getTileSize() - xInTile;
		}
		
		@Override
		public final int transformYInTile(final int xInTile, final int yInTile) {
			return this.getTileSize() - yInTile;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6070034523633787629L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class TileRotation270 extends TileRotation {
		
		public TileRotation270(final int tileSize) {
			super(tileSize);
		}
		
		@Override
		public final int transformXInTile(final int xInTile, final int yInTile) {
			return yInTile;
		}
		
		@Override
		public final int transformYInTile(final int xInTile, final int yInTile) {
			return this.getTileSize() - xInTile;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 5962504140559334542L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static abstract interface TileTransformer extends Serializable {
		
		public abstract int transformXInTile(int xInTile, int yInTile);
		
		public abstract int transformYInTile(int xInTile, int yInTile);
		
		public abstract int transformRGB(int rgb);
		
		/**
		 * @author codistmonk (creation 2014-05-11)
		 */
		public static enum Predefined implements TileTransformer {
			
			ID {
				
				@Override
				public final int transformXInTile(final int xInTile, final int yInTile) {
					return xInTile;
				}
				
				@Override
				public final int transformYInTile(final int xInTile, final int yInTile) {
					return yInTile;
				}
				
				@Override
				public final int transformRGB(final int rgb) {
					return rgb;
				}
				
			};
			
		}
		
	}
	
}
