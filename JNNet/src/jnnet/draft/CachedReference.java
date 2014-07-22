package jnnet.draft;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		
		synchronized (cache) {
			cache.add(this);
		}
	}
	
	public final T get() {
		synchronized (cache) {
			++this.usageCount;
			
			return this.referent;
		}
	}
	
	@Override
	public final int compareTo(final CachedReference<?> that) {
		synchronized (cache) {
			return Long.compare(this.usageCount, that.usageCount);
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
	
	private static final List<CachedReference<?>> cache = new CacheReducer().getCache();
	
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
			try {
				synchronized (this.getCache()) {
					Collections.sort((List) this.getCache());
					
					final int n = (int) (this.getCache().size() * this.reductionRatio);
					
					for (int i = 0; i < n; ++i) {
						this.getCache().get(i).clear();
					}
					
					this.getCache().subList(0, n).clear();
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
