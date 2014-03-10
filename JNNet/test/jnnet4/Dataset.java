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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jnnet.DoubleList;

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
		this(resourcePath, -1);
	}
	
	public Dataset(final String resourcePath, final int labelIndex) {
		this.labelIds = new LinkedHashMap<String, Integer>();
		this.labelCounts = new LinkedHashMap<String, AtomicInteger>();
		this.labels = new ArrayList<String>();
		this.data = new DoubleList();
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
			int lineId = 0;
			final Pattern separator = Pattern.compile("(\\s|,)+");
			
			while (scanner.hasNext()) {
				if (lineId  % 100000 == 0) {
					debugPrint("lineId:", lineId);
				}
				
				final String[] line = separator.split(scanner.nextLine().trim());
				final int n = line.length + (labelScanner != null ? 1 : 0);
				
				if (2 <= n) {
					buffer = reserve(buffer, n);
					final int labelOffset = 0 <= labelIndex ? labelIndex : n - 1;
					boolean itemIsValid = true;
					
					for (int i = 0, j = 0; i < line.length && itemIsValid; ++i) {
						try {
							if (i != labelOffset) {
								buffer[j++] = parseDouble(line[i]);
							}
						} catch (final NumberFormatException exception) {
							itemIsValid = false;
							++invalidItemCount;
						}
					}
					
					if (itemIsValid) {
						this.step = n;
						
						for (int i = 0; i < line.length; ++i) {
							if (i != labelOffset) {
								this.data.add(buffer[i]);
							}
						}
						
						final String label = labelScanner != null ? labelScanner.next() : line[labelOffset];
						Integer labelId = this.getLabelIds().get(label);
						
						if (labelId == null) {
							labelId = this.getLabels().size();
							this.getLabelIds().put(label, labelId);
							this.getLabels().add(label);
						}
						
						this.data.add(labelId);
						getOrCreate(this.getLabelCounts(), label, ATOMIC_INTEGER_FACTORY).incrementAndGet();
					}
				}
				
				++lineId;
			}
			
			debugPrint("labelCounts:", this.getLabelCounts());
			
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
	
	public static final DefaultFactory<AtomicInteger> ATOMIC_INTEGER_FACTORY = DefaultFactory.forClass(AtomicInteger.class);
	
}
