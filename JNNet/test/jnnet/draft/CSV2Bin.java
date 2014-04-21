package jnnet.draft;

import static jnnet.draft.Functional.map;
import static jnnet.draft.Functional.method;
import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Scanner;

import jnnet.draft.MitosAtypiaImporter.ConsoleMonitor;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-04-08)
 */
public final class CSV2Bin {
	
	private CSV2Bin() {
		throw new IllegalInstantiationException();
	}
	
	public static final Method PARSE_DOUBLE = method(Double.class, "parseDouble", String.class);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "");
		final String outPath = arguments.get("out", baseName(filePath) + ".bin");
		final String delimiter = arguments.get("delimiter", ",");
		final DataType dataType = DataType.valueOf(arguments.get("dataType", "FLOAT"));
		
		if ("".equals(filePath)) {
			System.out.println("Usage: file <path> [out <path>(baseName(inPath).bin)] [delimiter <regex>(,)] [dataType <BYTE|SHORT|INT|LONG|FLOAT|DOUBLE>(FLOAT)]");
			
			return;
		}
		
		System.out.println("file: " + filePath);
		System.out.println("out: " + outPath);
		System.out.println("delimiter: " + delimiter);
		System.out.println("dataType: " + dataType);
		
		final ConsoleMonitor monitor = new ConsoleMonitor(5000L);
		final Scanner scanner = new Scanner(Tools.getResourceAsStream(filePath));
		boolean headerWritten = false;
		int ignoredLines = 0;
		int lineId = 0;
		
		try {
			final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outPath)));
			
			try {
				while (scanner.hasNext()) {
					monitor.ping((lineId++) + "\r");
					
					try {
						final double[] values = parseDoubles(scanner.nextLine().trim().split(delimiter));
						
						if (!headerWritten) {
							headerWritten = true;
							out.writeByte(dataType.ordinal());
							out.writeInt(values.length);
						}
						
						for (final double value : values) {
							dataType.write(value, out);
						}
					} catch (final Exception exception) {
						ignore(exception);
						++ignoredLines;
					}
				}
				
				if (0 < ignoredLines) {
					monitor.pause();
					System.err.println("ignored: " + ignoredLines);
				}
			} finally {
				out.close();
				monitor.pause();
			}
		} finally {
			scanner.close();
		}
	}
	
	public static final double[] parseDoubles(final String[] values) {
		return map(double.class, null, PARSE_DOUBLE, values);
	}
	
	public static final String baseName(final String fileName) {
		final int i = fileName.lastIndexOf('.');
		
		return i < 0 ? fileName : fileName.substring(0, i);
	}
	
	/**
	 * @author codistmonk (creation 2014-04-08)
	 */
	public static enum DataType {
		
		BYTE {
			
			@Override
			public final int getByteCount() {
				return Byte.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeByte(((int) value) & 0x000000FF);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readByte() & 0x000000FF;
				}
			}
			
		}, SHORT {
			
			@Override
			public final int getByteCount() {
				return Short.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeShort(((int) value) & 0xFFFF);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readShort();
				}
			}
			
		}, INT {
			
			@Override
			public final int getByteCount() {
				return Integer.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeInt((int) value);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readInt();
				}
			}
			
		}, LONG {
			
			@Override
			public final int getByteCount() {
				return Long.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeLong((long) value);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readLong();
				}
			}
			
		}, FLOAT {
			
			@Override
			public final int getByteCount() {
				return Float.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeFloat((float) value);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readFloat();
				}
			}
			
		}, DOUBLE {
			
			@Override
			public final int getByteCount() {
				return Double.SIZE / Byte.SIZE;
			}
			
			@Override
			public final void write(final double value, final DataOutputStream out) throws IOException {
				out.writeDouble(value);
			}
			
			@Override
			public final void read(final DataInputStream in,
					final double[] out, final int offset, final int count) throws IOException {
				final int end = offset + count;
				
				for (int i = offset; i < end; ++i) {
					out[i] = in.readDouble();
				}
			}
			
		};
		
		public abstract int getByteCount();
		
		public abstract void write(double value, DataOutputStream out) throws IOException;
		
		public abstract void read(DataInputStream in, double[] out, int offset, int count) throws IOException;
		
	}
	
}
