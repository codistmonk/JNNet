package jnnet;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public final class ConstantValueSource implements ValueSource {
	
	private final double value;
	
	public ConstantValueSource(final double value) {
		this.value = value;
	}
	
	@Override
	public final void update() {
		// NOP
	}
	
	@Override
	public final double getValue() {
		return this.value;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 5846379572555159967L;
	
	public static final ConstantValueSource ONE = new ConstantValueSource(1.0);
	
}
