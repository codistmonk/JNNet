package jnnet4;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author codistmonk (creation 2014-03-28)
 */
public final class MosaicBuilder implements Serializable {
	
	private final List<BufferedImage> images;
	
	private final int preferredRowCount;
	
	public MosaicBuilder() {
		this(0);
	}
	
	public MosaicBuilder(final int preferredRowCount) {
		this.images = new ArrayList<BufferedImage>();
		this.preferredRowCount = preferredRowCount;
	}
	
	public final List<BufferedImage> getImages() {
		return this.images;
	}
	
	public final int getPreferredRowCount() {
		return this.preferredRowCount;
	}
	
	public final BufferedImage generateMosaic() {
		if (this.getImages().isEmpty()) {
			return new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
		}
		final int rowCount = 0 < this.getPreferredRowCount() ? this.getPreferredRowCount() :
			(int) ceil(sqrt(this.getImages().size()));
		final int columnCount = (int) ceil((double) this.getImages().size() / rowCount);
		
		debugPrint(this.getImages().size(), rowCount, columnCount);
		
		final int imageWidth = this.getImages().get(0).getWidth();
		final int imageHeight = this.getImages().get(0).getHeight();
		final int mosaicWidth = columnCount * imageWidth;
		final int mosaicHeight = columnCount * imageHeight;
		final BufferedImage result = new BufferedImage(mosaicWidth, mosaicHeight, BufferedImage.TYPE_3BYTE_BGR);
		
		{
			final Graphics2D g = result.createGraphics();
			int i = 0;
			
			for (final BufferedImage image : this.getImages()) {
				final int tileX = (i % columnCount) * imageWidth;
				final int tileY = (i / columnCount) * imageHeight;
				
				g.drawImage(image, tileX, tileY, null);
				++i;
			}
			
			g.dispose();
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -3610143890164242310L;
	
}
