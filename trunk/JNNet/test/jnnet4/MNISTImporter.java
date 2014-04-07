package jnnet4;

import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import jnnet4.MNISTImporter.IDX.DataType;

import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-03-21)
 */
public final class MNISTImporter {
	
	private MNISTImporter() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		for (final String dataKind : array("train", "test")) {
			debugPrint("dataKind:", dataKind);
			
			final DataInputStream labelsInput = new DataInputStream(new FileInputStream("../Libraries/datasets/mnist/" + dataKind +"-labels.idx1-ubyte"));
			final DataInputStream imagesInput = new DataInputStream(new FileInputStream("../Libraries/datasets/mnist/" + dataKind + "-images.idx3-ubyte"));
			
			try {
				debugPrint(imagesInput.available(), labelsInput.available());
				
				final IDX labels = new IDX(labelsInput);
				final Set<Short> labelIds = new TreeSet<Short>();
				
				for (final short label : (short[]) labels.getData()) {
					labelIds.add(label);
				}
				
				debugPrint(labels.getDataType(), Arrays.toString(labels.getDimensions()), labelIds.size());
				
				final IDX imageSet = new IDX(imagesInput);
				
				debugPrint(imageSet.getDataType(), Arrays.toString(imageSet.getDimensions()));
				
				final short[] labelData = (short[]) labels.getData();
				final short[][][] imagesData = (short[][][]) imageSet.getData();
				final int n = labelData.length;
				final int w = imageSet.getDimensions()[1];
				final int h = imageSet.getDimensions()[2];
				
				if (n != imagesData.length) {
					throw new IllegalArgumentException();
				}
				
				for (final short labelId : labelIds) {
					final PrintStream out = new PrintStream("../Libraries/datasets/mnist/mnist_" + labelId + "." + dataKind);
					
					try {
						for (int i = 0; i < n; ++i) {
							final short[][] rows = imagesData[i];
							
							for (int y = 0; y < h; ++y) {
								final short[] row = rows[y];
								
								for (int x = 0; x < w; ++x) {
									out.print(row[x]);
									out.print(' ');
								}
							}
							
							out.println(labelData[i] == labelId ? 1 : 0);
						}
					} finally {
						out.close();
					}
				}
			} finally {
				imagesInput.close();
				labelsInput.close();
			}
		}
	}
	
	public static final BufferedImage toAwtImage(final IDX imageSet, final int imageIndex) {
		if (imageSet.getDataType() != DataType.UNSIGNED_BYTE) {
			throw new IllegalArgumentException();
		}
		
		final int width = imageSet.getDimensions()[1];
		final int height = imageSet.getDimensions()[2];
		final BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final short[][][] data = (short[][][]) imageSet.getData();
		final short[][] rows = data[imageIndex];
		
		for (int y = 0; y < height; ++y) {
			final short[] row = rows[y];
			
			for (int x = 0; x < width; ++x) {
				result.setRGB(x, y, 0xFF000000 | (0x00010101 * row[x]));
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-21)
	 */
	public static final class IDX implements Serializable {
		
		private final DataType dataType;
		
		private final int[] dimensions;
		
		private final Object data;
		
		public IDX(final InputStream input) {
			try {
				final DataInputStream data = new DataInputStream(input);
				
				if (data.readShort() != (short) 0) {
					throw new IllegalArgumentException();
				}
				
				this.dataType = DataType.getInstance(data.readByte());
				
				final int dimensionCount = ((int) data.readByte()) & 0x000000FF;
				this.dimensions = new int[dimensionCount];
				
				for (int i = 0; i < dimensionCount; ++i) {
					this.dimensions[i] = data.readInt();
				}
				
				this.data = this.dataType.read(data, Array.newInstance(this.dataType.getValueType(), this.dimensions));
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		public final DataType getDataType() {
			return this.dataType;
		}
		
		public final int[] getDimensions() {
			return this.dimensions;
		}
		
		public final Object getData() {
			return this.data;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2080634280843716572L;
		
		/**
		 * @author codistmonk (creation 2014-03-21)
		 */
		public static enum DataType {
			
			UNSIGNED_BYTE {
				
				@Override
				public final Class<?> getValueType() {
					return short.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setShort(out, i, (short) (in.readByte() & 0x000000FF));
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			}, SIGNED_BYTE {
				
				@Override
				public final Class<?> getValueType() {
					return byte.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setByte(out, i, in.readByte());
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			}, SIGNED_SHORT {
				
				@Override
				public final Class<?> getValueType() {
					return short.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setShort(out, i, in.readShort());
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			}, SIGNED_INT {
				
				@Override
				public final Class<?> getValueType() {
					return int.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setInt(out, i, in.readInt());
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			}, FLOAT {
				
				@Override
				public final Class<?> getValueType() {
					return float.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setFloat(out, i, in.readFloat());
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			}, DOUBLE {
				
				@Override
				public final Class<?> getValueType() {
					return double.class;
				}
				
				@Override
				protected final <T> T protectedRead(final DataInputStream in, final T out) {
					try {
						final int n = Array.getLength(out);
						
						for (int i = 0; i < n; ++i) {
							Array.setDouble(out, i, in.readDouble());
						}
						
						return out;
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
			};
			
			public abstract Class<?> getValueType();
			
			public final <T> T read(final DataInputStream in, final T out) {
				final int n = Array.getLength(out);
				
				if (out.getClass().getComponentType().isArray()) {
					for (int i = 0; i < n; ++i) {
						this.read(in, Array.get(out, i));
					}
				} else {
					this.protectedRead(in, out);
				}
				
				return out;
			}
			
			protected abstract <T> T protectedRead(DataInputStream in, T out);
			
			public static final DataType getInstance(final byte dataType) {
				switch (dataType) {
				case 0x08:
					return UNSIGNED_BYTE;
				case 0x09:
					return SIGNED_BYTE;
				case 0x0B:
					return SIGNED_SHORT;
				case 0x0C:
					return SIGNED_INT;
				case 0x0D:
					return FLOAT;
				case 0x0E:
					return DOUBLE;
				default:
					throw new IllegalArgumentException();
				}
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-03-21)
	 */
	public static final class IdFactory implements Factory<Integer> {
		
		private int nextId;
		
		public IdFactory() {
			this(0);
		}
		
		public IdFactory(final int nextId) {
			this.nextId = nextId;
		}
		
		@Override
		public final Integer newInstance() {
			return this.nextId++;
		}
		
		@Override
		public final Class<Integer> getInstanceClass() {
			return Integer.class;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6950246657979817968L;
		
	}
	
}
