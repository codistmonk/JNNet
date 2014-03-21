package jnnet4;

import static java.lang.reflect.Array.newInstance;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import jnnet4.MNISTViewer.IDX.DataType;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-03-21)
 */
public final class MNISTViewer {
	
	private MNISTViewer() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final DataInputStream input = new DataInputStream(new FileInputStream("../Libraries/datasets/mnist/t10k-images-idx3-ubyte"));
		
		try {
			debugPrint(input.available());
			
			final IDX imageSet = new IDX(input);
			
			debugPrint(imageSet.getDataType(), Arrays.toString(imageSet.getDimensions()));
			
			SwingTools.show(toAwtImage(imageSet, 0), MNISTViewer.class.getSimpleName(), false);
		} finally {
			input.close();
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
	
}
