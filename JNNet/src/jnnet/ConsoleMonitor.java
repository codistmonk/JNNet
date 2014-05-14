package jnnet;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-04-07)
 */
public final class ConsoleMonitor implements Serializable {
	
	private final TicToc timer;
	
	private final long periodMilliseconds;
	
	private final AtomicBoolean newLineNeeded;
	
	public ConsoleMonitor(final long periodMilliseconds) {
		this.timer = new TicToc();
		this.periodMilliseconds = periodMilliseconds;
		this.newLineNeeded = new AtomicBoolean();
		this.timer.tic();
	}
	
	public final void ping() {
		this.ping(".");
	}
	
	public final synchronized void ping(final String text) {
		if (this.periodMilliseconds <= this.timer.toc()) {
			System.out.print(text);
			this.newLineNeeded.set(true);
			this.timer.tic();
		}
	}
	
	public final void pause() {
		if (this.newLineNeeded.getAndSet(false)) {
			System.out.println();
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -3669736743010335592L;
	
}
