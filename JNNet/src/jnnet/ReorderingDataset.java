package jnnet;

import static java.util.Arrays.copyOfRange;
import static jnnet.JNNetTools.irange;
import static jnnet.JNNetTools.swap;

import java.util.Random;

import net.sourceforge.aprog.tools.ConsoleMonitor;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class ReorderingDataset implements Dataset {
	
	private final Dataset source;
	
	private final int[] indices;
	
	private final DatasetStatistics statistics;
	
	public ReorderingDataset(final Dataset source) {
		this(source, irange(source.getItemCount()));
	}
	
	public ReorderingDataset(final Dataset source, final int[] indices) {
		System.out.println(this.getClass().getName());
		
		final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
		
		this.source = source;
		this.indices = indices;
		this.statistics = new DatasetStatistics(source.getItemSize() - 1);
		
		for (int i = 0; i < indices.length; ++i) {
			monitor.ping(i + "/" + indices.length + "\r");
			
			this.statistics.addItem(source.getItem(indices[i]));
		}
		
		monitor.pause();
		
		this.statistics.printTo(System.out);
	}
	
	public final DatasetStatistics getStatistics() {
		return this.statistics;
	}
	
	public final ReorderingDataset shuffle(final int chunkSize) {
		final int n = this.getItemCount();
		
		for (int i = 0; i < n; i += chunkSize) {
			final int j = chunkSize * RANDOM.nextInt(n / chunkSize);
			
			for (int k = 0; k < chunkSize; ++k) {
				swap(this.indices, i + k, j + k);
			}
		}
		
		return this;
	}
	
	public final ReorderingDataset shuffle() {
		return this.shuffle(1);
	}
	
	public final ReorderingDataset subset(final int start, final int count) {
		return new ReorderingDataset(this.source, copyOfRange(this.indices, start, start + count));
	}
	
	public final ReorderingDataset swapFolds(final int fold1, final int fold2, final int foldCount) {
		final int n = this.getItemCount() / foldCount;
		final int i1 = fold1 * n;
		final int i2 = fold2 * n;
		
		for (int i = 0; i < n; ++i) {
			swap(this.indices, i1 + i, i2 + i);
		}
		
		return this;
	}
	
	@Override
	public final int getItemCount() {
		return this.indices.length;
	}
	
	@Override
	public final int getItemSize() {
		return this.source.getItemSize();
	}
	
	@Override
	public final double getItemValue(final int itemId, final int valueId) {
		return this.source.getItemValue(this.getSourceItemId(itemId), valueId);
	}

	@Override
	public final double[] getItem(final int itemId, final double[] result) {
		return this.source.getItem(this.getSourceItemId(itemId), result);
	}
	
	@Override
	public final double[] getItemWeights(final int itemId, final double[] result) {
		return this.source.getItemWeights(this.getSourceItemId(itemId), result);
	}
	
	@Override
	public final double getItemLabel(final int itemId) {
		return this.source.getItemLabel(this.getSourceItemId(itemId));
	}
	
	private final int getSourceItemId(final int itemId) {
		return this.indices[itemId];
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 7192159261343544777L;
	
	public static final Random RANDOM = new Random(0L);
	
}
