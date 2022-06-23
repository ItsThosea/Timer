import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A timer.
 */
public class Timer implements Serializable {

	private static final List<Timer> timersToRemove = new ArrayList<>();

	private static volatile boolean stopThread = false;

	private static final Thread checkerThread = new Thread(new Runnable() {
		private long lastCheckTime = -1;

		public void run() {
			while(true) {
				if(stopThread)
					return;

				long now = System.currentTimeMillis();

				if(lastCheckTime == -1) {
					lastCheckTime = now;
				} else if((now - lastCheckTime) > 10) {
					lastCheckTime = now;

					// Remove timers that have finished
					for(Timer timer : timers) {
						if(timer.isDone()) {
							timer.stop();
						}
					}

					for(Timer timer : timersToRemove) {
						timers.remove(timer);
					}
					timersToRemove.clear();
				}
			}
		}
	});

	public static void stopThread() {
		stopThread = true;
	}

	static {
		checkerThread.setName("Timer Checker");
		checkerThread.setDaemon(true);
		checkerThread.start();
	}

	@Serial
	private static final long serialVersionUID = 0;

	private static final List<Timer> timers = Collections.synchronizedList(new ArrayList<>());

	private volatile long startTime = -1;
	private volatile long countdownTime = -1;

	private transient Runnable whenDone;

	private transient volatile boolean isStopping = false;

	/**
	 * Sets the time that the timer will count down from
	 *
	 * @param countdownTime the time the timer will count down from
	 * @param unit the source unit
	 * @return this
	 */
	public Timer setCountdownTime(long countdownTime, TimeUnit unit) {
		this.countdownTime = unit.toMillis(countdownTime);
		return this;
	}

	/**
	 * Sets the callback that will be run upon finishing the countdown
	 *
	 * @param callback the new callback
	 * @return this
	 */
	public Timer setCallback(Runnable callback) {
		this.whenDone = callback;

		return this;
	}

	/**
	 * Starts the timer
	 *
	 * @return this
	 * @throws IllegalStateException if the timer is already running or no countdown time has been set
	 */
	public Timer start() {
		if(isRunning())
			throw new IllegalStateException("Timer is already running!");
		if(countdownTime == -1)
			throw new IllegalStateException("No countdown time set! Use setCountdownTime");

		startTime = System.currentTimeMillis();
		timers.add(this);
		return this;
	}

	/**
	 * Gets if the timer is running
	 *
	 * @return whether the timer is running or not
	 */
	public boolean isRunning() {
		return startTime != -1;
	}

	/**
	 * Stops the timer
	 *
	 * @return this
	 * @throws IllegalStateException if the timer isn't running
	 */
	public Timer stop() {
		if(!isRunning())
			throw new IllegalStateException("Timer is not running!");

		if(isStopping)
			return this;

		isStopping = true;
		startTime = -1;

		if(whenDone != null)
			whenDone.run();

		if(Thread.currentThread() == checkerThread)
			timersToRemove.add(this);
		else
			timers.remove(this);

		isStopping = false;

		return this;
	}
	
	/**
	 * Gets whether the timer is in the process of stopping or not
	 *
	 * @return whether the timer is stopping or not
	 */
	public boolean isStopping() {
		return isStopping;
	}

	/**
	 * Tells whether the timer is done counting down or not.
	 * This will always be false if the timer hasn't started yet.
	 *
	 * @return whether the time is done counting down or not
	 */
	public boolean isDone() {
		long remainingTime = getRemainingTime(TimeUnit.MILLISECONDS);

		return remainingTime != -1 && remainingTime <= 0;
	}

	/**
	 * Gets the remaining time, or -1 if the timer isn't running
	 *
	 * @param unit the unit you want the remaining time in
	 * @return the remaining time, or -1 if the timer isn't running
	 */
	public long getRemainingTime(TimeUnit unit) {
		if(!isRunning())
			return -1;

		return unit.convert(countdownTime -
				(System.currentTimeMillis() - startTime), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Resets the timer, without changing the countdown time
	 * 
	 * @return this, for chaining
	 */
	public Timer reset() {
		if(!isRunning())
			throw new IllegalStateException("Timer is not running!");

		startTime = System.currentTimeMillis();

		return this;
	}

	/**
	 * Gets whether the timer is in the process of stopping or not
	 *
	 * @return true if the timer is in the process of stopping
	 */
	public boolean isStopping() {
		return isStopping;
	}
}
