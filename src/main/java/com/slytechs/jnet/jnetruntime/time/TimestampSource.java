/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.jnetruntime.time;

import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The Interface TimestampSource.
 *
 * @author Sly Technologies
 * @author repos@slytechs.com
 */
public interface TimestampSource extends InstantSource {

	/**
	 * The Interface AssignableTimestampSource.
	 */
	public interface AssignableTimestampSource extends TimestampSource {

		/**
		 * Timestamp.
		 *
		 * @param newTimestamp the new timestamp
		 */
		void timestamp(long newTimestamp);
	}

	/**
	 * System.
	 *
	 * @return the timestamp source
	 */
	public static AssignableTimestampSource system() {
		return new AssignableTimestampSource() {
			Clock clock = Clock.systemUTC();

			@Override
			public long timestamp() {
				return clock.millis();
			}

			@Override
			public Instant instant() {
				return clock.instant();
			}

			@Override
			public void sleep(long duration, TimeUnit unit) throws InterruptedException {
				unit.sleep(duration);
			}

			@Override
			public void timestamp(long newTimestamp) {
			}
		};
	}

	/**
	 * Assignable.
	 *
	 * @return the assignable timestamp source
	 */
	public static AssignableTimestampSource assignable() {
		return new AssignableTimestampSource() {
			final Exchanger<Long> exch = new Exchanger<>();;

			long ts;

			@Override
			public long timestamp() {
				return ts;
			}

			@Override
			public Instant instant() {
				return Instant.ofEpochMilli(ts);
			}

			@Override
			public void timestamp(long newTimestamp) {
				ts = newTimestamp;

				try {
					exch.exchange(newTimestamp, 1, TimeUnit.NANOSECONDS);
				} catch (InterruptedException | TimeoutException e) {}

			}

			@Override
			public void sleep(long duration, TimeUnit unit) throws InterruptedException {
				long oldTime = ts;
				long newTime = 0;

				for (;;) {
					try {
						newTime = exch.exchange(null, duration, unit);
					} catch (TimeoutException e) {
						// Interrupt on system time, if packet time hasn't arrived yet.
					}

					if (newTime - oldTime >= unit.toMillis(duration))
						return;
				}
			}

			@Override
			public void close() {
				timestamp(Long.MAX_VALUE);
			}
		};
	}

	/**
	 * Inits the.
	 *
	 * @param initialTimestamp the initial timestamp
	 */
	default void init(long initialTimestamp) {

	}

	/**
	 * Update.
	 *
	 * @param newTimestamp the new timestamp
	 * @throws InterruptedException the interrupted exception
	 */
	default void update(long newTimestamp) throws InterruptedException {

	}

	/**
	 * Timestamp.
	 *
	 * @return the long
	 */
	long timestamp();

	/**
	 * Nano time.
	 *
	 * @return the long
	 */
	default long nanoTime() {
		return System.nanoTime();
	}

	/**
	 * Milli time.
	 *
	 * @return the long
	 */
	default long milliTime() {
		return System.currentTimeMillis();
	}

	/**
	 * Epoch nano.
	 *
	 * @return the long
	 */
	default long epochNano() {
		long nano = nanoTime();
		long millis = milliTime();

		return millis + (nano % 1000);
	}

	/**
	 * Unit.
	 *
	 * @return the timestamp unit
	 */
	default TimestampUnit timestampUnit() {
		return TimestampUnit.EPOCH_NANO;
	}

	/**
	 * Time unit.
	 *
	 * @return the time unit
	 */
	default TimeUnit timeUnit() {
		return TimeUnit.NANOSECONDS;
	}

	/**
	 * Sleep.
	 *
	 * @param duration the duration
	 * @param unit     the unit
	 * @throws InterruptedException the interrupted exception
	 */
	default void sleep(long duration, TimeUnit unit) throws InterruptedException {
		unit.sleep(duration);
	}

	/**
	 * Timer.
	 *
	 * @param duration the duration
	 * @param unit     the unit
	 * @param action   the action
	 * @throws InterruptedException the interrupted exception
	 */
	default void timer(long duration, TimeUnit unit, Runnable action) throws InterruptedException {
		sleep(duration, unit);

		action.run();
	}

	/**
	 * Checks if is realtime.
	 *
	 * @return true, if is realtime
	 */
	default boolean isRealtime() {
		return false;
	}

	/**
	 * Close.
	 */
	default void close() {
	}
}
