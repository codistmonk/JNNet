package jnnet4;

import static java.util.Arrays.copyOfRange;
import static jnnet4.JNNetTools.ATOMIC_INTEGER_FACTORY;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import jnnet.DoubleList;
import jnnet4.CSV2Bin.DataType;
import jnnet4.MitosAtypiaImporter.ConsoleMonitor;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class BinDataset implements Dataset {
	
	private final DatasetStatistics statistics;
	
	private final DoubleList data;
	
	private int itemSize;
	
	public BinDataset(final String resourcePath) {
		this(resourcePath, -1, 0, Integer.MAX_VALUE);
	}
	
	public BinDataset(final String resourcePath, final int labelIndex, final int offset, final int count) {
		try {
			final ConsoleMonitor monitor = new ConsoleMonitor(CSVDataset.LOGGING_MILLISECONDS);
			final DataInputStream input = new DataInputStream(new BufferedInputStream(getResourceAsStream(resourcePath)));
			
			debugPrint(input.available());
			
			try {
				final DataType dataType = DataType.values()[input.readByte()];
				this.itemSize = input.readInt();
				final int inputDimension = this.itemSize - 1;
				this.statistics = new DatasetStatistics(inputDimension);
				this.data = new DoubleList(count < Integer.MAX_VALUE ? count * this.itemSize : 16);
				final double[] buffer = new double[this.itemSize];
				int lineId = 0;
				
				while (0 < input.available() && lineId < offset + count) {
					monitor.ping(lineId + "\r");
					
					dataType.read(input, buffer, 0, this.itemSize);
					
					if (offset <= lineId) {
						this.data.addAll(buffer);
						this.statistics.addItem(buffer);
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
