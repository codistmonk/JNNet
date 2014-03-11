package jnnet4;

import static java.lang.Double.parseDouble;
import static jnnet4.FeedforwardNeuralNetwork.reserve;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jnnet.DoubleList;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class Dataset implements Serializable {
	
	private final Map<String, Integer> labelIds;
	
	private final Map<String, AtomicInteger> labelCounts;
	
	private final List<String> labels;
	
	private final DoubleList data;
	
	private int step;
	
	public Dataset(final String resourcePath) {
		this(resourcePath, -1, 0, Integer.MAX_VALUE);
	}
	
	public Dataset(final String resourcePath, final int labelIndex, final int offset, final int count) {
		this.labelIds = new LinkedHashMap<String, Integer>();
		this.labelCounts = new LinkedHashMap<String, AtomicInteger>();
		this.labels = new ArrayList<String>();
		this.data = new DoubleList();
		final VectorStatistics[] statistics = new VectorStatistics[2];
		Scanner labelScanner = null;
		Scanner scanner = null;
		
		try {
			if (resourcePath.endsWith(".data")) {
				try {
					labelScanner = new Scanner(getResourceAsStream(resourcePath.replaceFirst("\\.data", "\\.labels")));
					debugPrint("Using label scanner");
				} catch (final Exception exception) {
					ignore(exception);
				}
			}
			
			scanner = new Scanner(getResourceAsStream(resourcePath));
			double[] buffer = {};
			int invalidItemCount = 0;
			int lineId = -1;
			final Pattern separator = Pattern.compile("(\\s|,)+");
			final TicToc timer = new TicToc();
			
			timer.tic();
			
			while (scanner.hasNext() && ++lineId < offset + count) {
				if (LOGGING_MILLISECONDS < timer.toc()) {
					debugPrint("lineId:", lineId);
					timer.tic();
				}
				
				final String line = scanner.nextLine();
				
				if (lineId < offset) {
					continue;
				}
				
				final String[] values = separator.split(line.trim());
				final int n = values.length + (labelScanner != null ? 1 : 0);
				
				if (2 <= n) {
					if (statistics[0] == null) {
						for (int i = 0; i < 2; ++i) {
							statistics[i] = new VectorStatistics(n - 1);
						}
					}
					
					buffer = reserve(buffer, n);
					final int labelOffset = 0 <= labelIndex ? labelIndex : n - 1;
					boolean itemIsValid = this.step == 0 || this.step == n;
					
					for (int i = 0, j = 0; i < values.length && itemIsValid; ++i) {
						try {
							if (i != labelOffset) {
								buffer[j++] = parseDouble(values[i]);
							}
						} catch (final NumberFormatException exception) {
							itemIsValid = false;
							++invalidItemCount;
						}
					}
					
					if (itemIsValid) {
						final int itemOffset = this.data.size();
						this.step = n;
						
						for (int i = 0; i < n - 1; ++i) {
							this.data.add(buffer[i]);
						}
						
						final String label = labelScanner != null ? labelScanner.next() : values[labelOffset];
						Integer labelId = this.getLabelIds().get(label);
						
						if (labelId == null) {
							labelId = this.getLabels().size();
							this.getLabelIds().put(label, labelId);
							this.getLabels().add(label);
						}
						
						this.data.add(labelId);
						getOrCreate(this.getLabelCounts(), label, ATOMIC_INTEGER_FACTORY).incrementAndGet();
						
						for (int i = itemOffset; i < itemOffset + n - 1; ++i) {
							statistics[labelId].getStatistics()[i - itemOffset].addValue(this.data.get(i));
						}
					}
				}
			}
			
			debugPrint("labelCounts:", this.getLabelCounts());
			
			for (int i = 0; i < 2; ++i) {
				debugPrint(i + "-statistics");
				debugPrint("means:", Arrays.toString(statistics[i].getMeans()));
				debugPrint("minima:", Arrays.toString(statistics[i].getMinima()));
				debugPrint("maxima:", Arrays.toString(statistics[i].getMaxima()));
				debugPrint("stddev:", Arrays.toString(statistics[i].getStandardDeviations()));
			}
			
			if (0 < invalidItemCount) {
				System.err.println(debug(DEBUG_STACK_OFFSET, "invalidItemCount:", invalidItemCount));
			}
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			
			if (labelScanner != null) {
				labelScanner.close();
			}
		}
	}
	
	public final Map<String, Integer> getLabelIds() {
		return this.labelIds;
	}
	
	public final List<String> getLabels() {
		return this.labels;
	}
	
	public final Map<String, AtomicInteger> getLabelCounts() {
		return this.labelCounts;
	}
	
	public final double[] getData() {
		return this.data.toArray();
	}
	
	public final int getStep() {
		return this.step;
	}
	
	public final int getItemCount() {
		return this.getData().length / this.getStep();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 5770717293628383428L;
	
	/**
	 * {@value}.
	 */
	public static final long LOGGING_MILLISECONDS = 5000L;
	
	public static final DefaultFactory<AtomicInteger> ATOMIC_INTEGER_FACTORY = DefaultFactory.forClass(AtomicInteger.class);
	
}
