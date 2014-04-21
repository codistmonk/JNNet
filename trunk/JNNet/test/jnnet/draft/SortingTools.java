package jnnet.draft;

import java.io.Serializable;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-18)
 */
public final class SortingTools {
	
	private SortingTools() {
		throw new IllegalInstantiationException();
	}
	
    /**
     * Sorts the specified array of ints into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param indices the array to be sorted
     */
    public static final void sort(final int[] indices, final IndexComparator comparator) {
    	sort1(indices, 0, indices.length, comparator);
    }
    
	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	private static final void sort1(final int indices[], final int offset,
			final int length, final IndexComparator comparator) {
		if (length < 7) {
			for (int i = offset; i < length + offset; i++) {
				for (int j = i; j > offset && comparator.compare(indices[j - 1], indices[j]) > 0; j--) {
					swap(indices, j, j - 1);
				}
			}
			
			return;
		}
		
		// Choose a partition element, v
		int m = offset + (length >> 1); // Small arrays, middle element
		if (length > 7) {
			int l = offset;
			int n = offset + length - 1;
			if (length > 40) { // Big arrays, pseudomedian of 9
				int s = length / 8;
				l = med3(indices, l, l + s, l + 2 * s, comparator);
				m = med3(indices, m - s, m, m + s, comparator);
				n = med3(indices, n - 2 * s, n - s, n, comparator);
			}
			m = med3(indices, l, m, n, comparator); // Mid-size, med of 3
		}
		
		// Establish Invariant: v* (<v)* (>v)* v*
		int a = offset, b = a, c = offset + length - 1, d = c;
		while (true) {
			int cmp;
			while (b <= c && (cmp = comparator.compare(indices[b], indices[m])) <= 0) {
				if (cmp == 0) {
					swap(indices, a++, b);
				}
				b++;
			}
			while (c >= b && (cmp = comparator.compare(indices[c], indices[m])) >= 0) {
				if (cmp == 0) {
					swap(indices, c, d--);
				}
				c--;
			}
			if (b > c) {
				break;
			}
			swap(indices, b++, c--);
		}
		
		// Swap partition elements back to middle
		int s, n = offset + length;
		s = Math.min(a - offset, b - a);
		vecswap(indices, offset, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(indices, b, n - s, s);
		
		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) {
			sort1(indices, offset, s, comparator);
		}
		if ((s = d - c) > 1) {
			sort1(indices, n - s, s, comparator);
		}
	}
	
	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(int x[], int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
	
	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(int x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++) {
			swap(x, a, b);
		}
	}
	
	/**
	 * Returns the index of the median of the three indexed integers.
	 */
	private static int med3(int indices[], int a, int b, int c, final IndexComparator comparator) {
//		return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
//				: (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
		return (comparator.compare(indices[a], indices[b]) < 0 ? (comparator.compare(indices[b], indices[c]) < 0 ? b : comparator.compare(indices[a], indices[c]) < 0 ? c : a)
				: (comparator.compare(indices[b], indices[c]) > 0 ? b : comparator.compare(indices[a], indices[c]) > 0 ? c : a));
	}
    
    /**
     * @author codistmonk (creation 2014-03-14)
     */
    public static abstract interface IndexComparator extends Serializable {
    	
    	public abstract int compare(int index1, int index2);
    	
    }
	
}
