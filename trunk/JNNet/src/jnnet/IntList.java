package jnnet;

import static java.lang.Math.min;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;
import java.util.Arrays;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class IntList implements Serializable {
	
	private int[] values;
	
	private int first;
	
	private int end;
	
	private boolean beingTraversed;
	
	public IntList() {
		this(16);
	}
	
	public IntList(final int initialCapacity) {
		this.values = new int[initialCapacity];
	}
	
	public final IntList clear() {
		this.first = 0;
		this.end = 0;
		
		return this;
	}
	
	public final int size() {
		return this.end - this.first;
	}
	
	public final IntList add(final int value) {
		if (this.values.length <= this.end) {
			if (0 < this.first) {
				System.arraycopy(this.values, this.first, this.values, 0, this.size());
				this.end -= this.first;
				this.first = 0;
			} else {
				final int newBufferSize = (int) min(Integer.MAX_VALUE, 2L * this.size());
				
				try {
					try {
						this.values = copyOf(this.values, newBufferSize);
					} catch (final OutOfMemoryError error) {
						ignore(error);
						
						gc(10L);
						
						this.values = copyOf(this.values, newBufferSize);
					}
				} catch (final OutOfMemoryError error) {
					System.err.println(debug(DEBUG_STACK_OFFSET, "Failed to allocate", newBufferSize, this.values.getClass().getComponentType().getSimpleName() + "s"));
					
					throw error;
				}
			}
		}
		
		this.values[this.end++] = value;
		
		return this;
	}
	
	public final IntList addAll(final int... values) {
		for (final int value : values) {
			this.add(value);
		}
		
		return this;
	}
	
	public final int get(final int index) {
		return this.values[this.first + index];
	}
	
	public final IntList set(final int index, final int value) {
		this.values[this.first + index] = value;
		
		return this;
	}
	
	public final boolean isBeingTraversed() {
		return this.beingTraversed;
	}
	
	public final IntList resize(final int newSize) {
		if (this.values.length < newSize) {
			final int[] newValues = new int[newSize];
			System.arraycopy(this.values, this.first, newValues, 0, this.size());
			this.values = newValues;
			this.first = 0;
		} else {
			if (0 < this.first) {
				System.arraycopy(this.values, this.first, this.values, 0, this.size());
				this.first = 0;
			}
		}
		
		this.end = this.first + newSize;
		
		return this;
	}
	
	public final IntList pack() {
		if (this.values.length != this.size()) {
			this.values = copyOfRange(this.values, this.first, this.end);
			this.first = 0;
			this.end = this.values.length;
		}
		
		return this;
	}
	
	public final int remove(final int index) {
		if (index == 0) {
			return this.values[this.first++];
		}
		
		final int result = this.get(index);
		
		System.arraycopy(this.values, this.first + index + 1, this.values, this.first + index, this.size() - 1 - index);
		--this.end;
		
		return result;
	}
	
	public final boolean isEmpty() {
		return this.size() <= 0;
	}
	
	public final IntList sort() {
		Arrays.sort(this.values, this.first, this.end);
		
		return this;
	}
	
	public final int[] toArray() {
		return this.pack().values;
	}
	
	public final IntList forEach(final Processor processor) {
		this.beingTraversed = true;
		
		try {
			for (int first = this.first, i = first; i < this.end; i += 1 + this.first - first, first = this.first) {
				if (!processor.process(this.values[i])) {
					break;
				}
			}
		} finally {
			this.beingTraversed = false;
		}
		
		return this;
	}
	
	@Override
	public final String toString() {
		final StringBuilder resultBuilder = new StringBuilder();
		
		resultBuilder.append('[');
		
		if (!this.isEmpty()) {
			resultBuilder.append(this.get(0));
			
			final int n = this.size();
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(' ').append(this.get(i));
			}
		}
		
		resultBuilder.append(']');
		
		return resultBuilder.toString();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 582817874368132826L;
	
	public static final DefaultFactory<IntList> FACTORY = DefaultFactory.forClass(IntList.class);
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	public static abstract interface Processor extends Serializable {
		
		public abstract boolean process(int value);
		
	}
	
}
