package jnnet.draft;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import multij.tools.Tools;

/**
 * @author codistmonk (creation 2014-07-22)
 * 
 * @param <T>
 */
public final class CachedReference<T> implements Serializable, Comparable<CachedReference<?>> {
	
	private transient T referent;
	
	private transient long usageCount;
	
	public CachedReference(final T referent) {
		this.referent = referent;
		
		if (referent != null) {
			synchronized (cache) {
				cache.add(this);
			}
		}
	}
	
	public final T get() {
		if (this.referent == null) {
			return null;
		}
		
		synchronized (cache) {
			++this.usageCount;
			
			return this.referent;
		}
	}
	
	@Override
	public final int compareTo(final CachedReference<?> that) {
		synchronized (cache) {
			return -Long.compare(this.usageCount, that.usageCount);
		}
	}
	
	final void clear() {
		synchronized (cache) {
			this.referent = null;
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 416637597373736132L;
	
	private static final List<CachedReference<?>> cache = new CacheReducer(0.15).getCache();
	
	public static final int getCacheSize() {
		synchronized (cache) {
			return cache.size();
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-07-22)
	 */
	public static final class CacheReducer implements Serializable {
		
		private final List<CachedReference<?>> cache;
		
		private final double reductionRatio;
		
		public CacheReducer() {
			this(0.5);
		}
		
		public CacheReducer(final double reductionRatio) {
			this(new ArrayList<>(), reductionRatio);
		}
		
		public CacheReducer(final List<CachedReference<?>> cache, final double reductionRatio) {
			this.cache = cache;
			this.reductionRatio = reductionRatio;
		}
		
		public final List<CachedReference<?>> getCache() {
			return this.cache;
		}
		
		@Override
		protected final void finalize() throws Throwable {
			final Runtime runtime = Runtime.getRuntime();
			
			try {
				synchronized (this.getCache()) {
					if (runtime.freeMemory() < runtime.totalMemory() * this.reductionRatio) {
						System.out.println(Tools.debug(Tools.DEBUG_STACK_OFFSET
								, runtime.freeMemory(), runtime.totalMemory()));
						
						Collections.sort((List) this.getCache());
						
						final int oldEnd = this.getCache().size();
						final int newEnd = (int) (oldEnd * (1.0 - this.reductionRatio));
						
						for (int i = newEnd; i < oldEnd; ++i) {
							this.getCache().get(i).clear();
						}
						
						this.getCache().subList(newEnd, oldEnd).clear();
					}
				}
				
				new WeakReference<>(new CacheReducer(this.getCache(), this.reductionRatio));
			} finally {
				super.finalize();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5787810914935666192L;
		
	}
	
}
