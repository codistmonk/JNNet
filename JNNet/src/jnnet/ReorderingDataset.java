package jnnet;

import static java.util.Arrays.copyOfRange;
import static jnnet.draft.JNNetTools.irange;
import static jnnet.draft.JNNetTools.swap;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.util.Random;

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
		
		this.source = source;
		this.indices = indices;
		this.statistics = new DatasetStatistics(source.getItemSize() - 1);
		
		for (final int itemId : indices) {
			this.statistics.addItem(source.getItem(itemId));
		}
		
		this.statistics.printTo(System.out);
	}
	
	public final ReorderingDataset shuffle() {
		final int n = this.getItemCount();
		
		for (int i = 0; i < n; ++i) {
			swap(this.indices, i, RANDOM.nextInt(n));
		}
		
		return this;
	}
	
	public final ReorderingDataset subset(final int start, final int count) {
		return new ReorderingDataset(this.source, copyOfRange(this.indices, start, start + count));
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
	public final double[] getItem(final int itemId) {
		return this.source.getItem(this.getSourceItemId(itemId));
	}
	
	@Override
	public final double[] getItemWeights(final int itemId) {
		return this.source.getItemWeights(this.getSourceItemId(itemId));
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
