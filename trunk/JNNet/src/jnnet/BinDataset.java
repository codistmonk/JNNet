package jnnet;

import static java.util.Arrays.copyOfRange;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.sourceforge.aprog.tools.ConsoleMonitor;

import jgencode.primitivelists.DoubleList;

import jnnet.draft.CSV2Bin.DataType;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class BinDataset implements Dataset {
	
	private final int itemSize;
	
	private final DoubleList data;
	
	private final DatasetStatistics statistics;
	
	public BinDataset(final int itemSize) {
		this.itemSize = itemSize;
		final int inputDimension = itemSize - 1;
		this.statistics = new DatasetStatistics(inputDimension);
		this.data = new DoubleList();
	}
	
	public BinDataset(final String resourcePath) {
		this(resourcePath, 0, Integer.MAX_VALUE);
	}
	
	public BinDataset(final String resourcePath, final int offset, final int count) {
		System.out.println(this.getClass().getName());
		
		try {
			final ConsoleMonitor monitor = new ConsoleMonitor(CSVDataset.LOGGING_MILLISECONDS);
			final DataInputStream input = new DataInputStream(new BufferedInputStream(getResourceAsStream(resourcePath)));
			
			debugPrint(input.available());
			
			try {
				final DataType dataType = DataType.values()[input.readByte()];
				this.itemSize = input.readInt();
				final int inputDimension = this.itemSize - 1;
				this.statistics = new DatasetStatistics(inputDimension);
				this.data = new DoubleList(count < Integer.MAX_VALUE ? count * this.itemSize : input.available() / dataType.getByteCount());
				final double[] item = new double[this.itemSize];
				int lineId = 0;
				
				while (0 < input.available() && lineId < offset + count) {
					monitor.ping(lineId + "\r");
					
					dataType.read(input, item, 0, this.itemSize);
					
					if (offset <= lineId) {
						this.addItem(item);
					}
					
					++lineId;
				}
				
				this.statistics.printTo(System.out);
			} finally {
				input.close();
			}
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	public final DoubleList getData() {
		return this.data;
	}
	
	public final DatasetStatistics getStatistics() {
		return this.statistics;
	}
	
	public final BinDataset addItem(final double... item) {
		this.getData().addAll(item);
		this.getStatistics().addItem(item);
		
		return this;
	}
	
	@Override
	public final int getItemCount() {
		return this.data.size() / this.getItemSize();
	}
	
	@Override
	public final int getItemSize() {
		return this.itemSize;
	}
	
	@Override
	public final double getItemValue(final int itemId, final int valueId) {
		return this.data.get(itemId * this.getItemSize() + valueId);
	}
	
	@Override
	public final double[] getItem(final int itemId) {
		return copyOfRange(this.data.toArray(), itemId * this.getItemSize(), (itemId + 1) * this.getItemSize());
	}
	
	@Override
	public final double[] getItemWeights(final int itemId) {
		return copyOfRange(this.data.toArray(), itemId * this.getItemSize(), (itemId + 1) * this.getItemSize() - 1);
	}
	
	@Override
	public final double getItemLabel(final int itemId) {
		return this.getItemValue(itemId, this.getItemSize() - 1);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 4919461372047609500L;
	
}
