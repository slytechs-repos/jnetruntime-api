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
package com.slytechs.jnet.jnetruntime.util;

import java.util.concurrent.TimeUnit;

import com.slytechs.jnet.jnetruntime.time.TimestampSource;

/**
 * The Interface IsExpirable.
 */
public interface IsExpirable {

	/**
	 * Checks if is expirable.
	 *
	 * @param obj the obj
	 * @return true, if is expirable
	 */
	static boolean isExpirable(IsExpirable obj) {
		return obj == null || obj.isExpired();
	}

	/**
	 * Checks if is expired.
	 *
	 * @return true, if is expired
	 */
	boolean isExpired();

	/**
	 * Checks if is not expired.
	 *
	 * @return true, if is not expired
	 */
	default boolean isNotExpired() {
		return !isExpired();
	}

	/**
	 * Expires in.
	 *
	 * @param unit the unit
	 * @return the long
	 */
	long expiresIn(TimeUnit unit);

	/**
	 * Timestamp source.
	 *
	 * @return the timestamp source
	 */
	TimestampSource timestampSource();

	/**
	 * Expire.
	 */
	void expire();
}