package jnnet2.draft;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import jgencode.primitivelists.LongList;
import net.sourceforge.aprog.events.EventManager;
import net.sourceforge.aprog.tools.Tools;
import jnnet2.core.Dataset;
import jnnet2.core.LabelStatistics;

/**
 * @author codistmonk (creation 2014-07-17)
 */
public final class VirtualARFFDataset implements Dataset {
	
	private InputStream arff;
	
	private final Map<String, Double> labelIds;
	
	private final LongList offsets;
	
	private final long endOffset;
	
	private final int itemSize;
	
	private final int classAttributeIndex;
	
	private final LabelStatistics labelStatistics;
	
	public VirtualARFFDataset(final InputStream arff) {
		if (!arff.markSupported()) {
			throw new IllegalArgumentException();
		}
		
		this.arff = arff;
		this.labelIds = new HashMap<>();
		this.offsets = new LongList();
		this.labelStatistics = new LabelStatistics();
		int attributeCount = 0;
		int classAttributeIndex = -1;
		long endOffset = 0L;
		State state = ARFFState.ROOT;
		StringBuilder buffer = new StringBuilder();
		double[] item = null;
		
		try {
			this.arff.mark(0);
			
			long offset = 0L;
			
			for (int token = arff.read(); token != -1; token = arff.read(), ++offset) {
				buffer.append((char) token);
				
				final State oldState = state;
				state = state.next((char) token);
				
				if (ARFFState.ROOT == state) {
					if (ARFFState.COMMAND == oldState) {
						final String[] command = buffer.toString().split("\\s+");
						
						if ("@ATTRIBUTE".equals(command[0])) {
							if ("class".equals(command[1])) {
								classAttributeIndex = attributeCount;
								
								if (command[2].startsWith("{")) {
									final String[] labels = command[2].substring(1, command[2].length() - 1).split(",");
									
									for (int i = 0; i < labels.length; ++i) {
										this.labelIds.put(labels[i], (double) i);
									}
								}
							} else if (!"REAL".equals(command[2])) {
								throw new IllegalArgumentException();
							}
							
							++attributeCount;
						}
					} else if (ARFFState.DATUM == oldState) {
						if (item == null) {
							item = new double[attributeCount];
						}
						
						this.getLabelStatistics().addItem(this.getItem(buffer.toString(), classAttributeIndex, item));
						
						endOffset = offset;
					}
					
					if (0 < buffer.length()) {
						buffer = new StringBuilder();
					}
				} else if (ARFFState.DATUM == state && ARFFState.ROOT == oldState) {
					this.offsets.add(offset);
				}
			}
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
		
		this.itemSize = attributeCount;
		this.classAttributeIndex = classAttributeIndex;
		this.endOffset = endOffset;
	}
	
	@Override
	public final Iterator<double[]> iterator() {
		return new Iterator<double[]>() {
			
			private long itemId;
			
			private final double[] item = new double[VirtualARFFDataset.this.getItemSize()];
			
			@Override
			public final boolean hasNext() {
				return this.itemId < VirtualARFFDataset.this.getItemCount();
			}
			
			@Override
			public final double[] next() {
				VirtualARFFDataset.this.getItem(this.itemId++, this.item);
				
				return this.item;
			}
			
		};
	}
	
	@Override
	public final long getItemCount() {
		return this.offsets.size();
	}
	
	@Override
	public final int getItemSize() {
		return this.itemSize;
	}
	
	@Override
	public final double[] getItem(final long itemId, final double[] result) {
		try {
			this.arff.reset();
			final long offset = this.offsets.get((int) itemId);
			final int nextItemId = (int) (itemId + 1);
			final long nextOffset = nextItemId < this.offsets.size() ? this.offsets.get(nextItemId) : this.endOffset;
			final byte[] buffer = new byte[(int) (nextOffset - offset)];
			
			this.arff.skip(offset);
			this.arff.read(buffer);
			
			return getItem(new String(buffer), this.classAttributeIndex, result);
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final LabelStatistics getLabelStatistics() {
		return this.labelStatistics;
	}
	
	private final double[] getItem(final String buffer, final int classAttributeIndex, final double[] result) {
		final String[] attributes = buffer.trim().split(",");
		
		for (int i = 0, j = 0; i < attributes.length; ++i) {
			if (i != classAttributeIndex) {
				result[j++] = Double.parseDouble(attributes[i]);
			}
		}
		
		result[attributes.length - 1] = this.labelIds.get(attributes[classAttributeIndex]);
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 202679868180232684L;
	
	/**
	 * @author codistmonk (creation 2014-07-17)
	 */
	public static abstract interface State extends Serializable {
		
		public abstract State next(char token);
		
	}
	
	/**
	 * @author codistmonk (creation 2014-07-17)
	 */
	public static enum ARFFState implements State {
		
		ROOT {
			
			@Override
			public final State next(final char token) {
				switch (token) {
				case '%':
					return COMMENT;
				case '@':
					return COMMAND;
				case ' ':
				case '\t':
				case '\r':
				case '\n':
					return this;
				default:
					return DATUM;
				}
			}
			
		}, COMMENT {
			
			@Override
			public final State next(final char token) {
				switch (token) {
				case '\n':
					return ROOT;
				default:
					return this;
				}
			}
			
		}, COMMAND {
			
			@Override
			public final State next(final char token) {
				switch (token) {
				case '\n':
					return ROOT;
				default:
					return this;
				}
			}
			
		}, DATUM {
			
			@Override
			public final State next(final char token) {
				switch (token) {
				case '\n':
					return ROOT;
				default:
					return this;
				}
			}
			
		};
		
	}
	
	public static final class RootState implements State {
		
		@Override
		public final State next(final char token) {
			switch (token) {
			case '%':
			case '@':
			case ' ':
			case '\t':
			case '\r':
			case '\n':
			default:
			}
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
}
