package jnnet.draft;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public final class ModifiableValueSource implements ValueSource {
	
	private double value;
	
	@Override
	public final void update() {
		// NOP
	}
	
	@Override
	public final double getValue() {
		return this.value;
	}
	
	public final void setValue(final double value) {
		this.value = value;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -4620991329089575653L;
	
}
