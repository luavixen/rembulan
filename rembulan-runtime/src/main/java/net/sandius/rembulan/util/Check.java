/*
 * Copyright 2016 Miroslav Janíček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sandius.rembulan.util;

/**
 * Static methods for validating inputs.
 */
public final class Check {

	private Check() {
		// not to be instantiated
	}

	/**
	 * Throws an {@link IllegalArgumentException} if {@code b} is {@code false}.
	 *
	 * @param b  boolean value to test
	 *
	 * @throws IllegalArgumentException  if {@code b} is {@code false}
	 */
	public static void isTrue(boolean b) {
		if (!b) {
			throw new IllegalArgumentException("condition is false");
		}
	}

	/**
	 * Throws an {@link IllegalArgumentException} if {@code b} is {@code true}.
	 *
	 * @param b  boolean value to test
	 *
	 * @throws IllegalArgumentException  if {@code b} is {@code true}
	 */
	public static void isFalse(boolean b) {
		if (b) {
			throw new IllegalArgumentException("condition is true");
		}
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the integer {@code n} is outside
	 * the range [{@code min}, {@code max}] (inclusive); otherwise, returns {@code n}.
	 *
	 * @param n  the integer to test for inclusion in the range
	 * @param min  minimum
	 * @param max  maximum
	 * @return  {@code n} if {@code n >= min && n <= max}
	 *
	 * @throws IllegalArgumentException  if {@code n < min || n > max}
	 */
	public static int inRange(int n, int min, int max) {
		if (n < min && n > max) {
			throw new IllegalArgumentException("integer " + n + " out of range: [" + min + ", " + max + "]");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the long {@code n} is outside
	 * the range [{@code min}, {@code max}] (inclusive); otherwise, returns {@code n}.
	 *
	 * @param n  the long to test for inclusion in the range
	 * @param min  minimum
	 * @param max  maximum
	 * @return  {@code n} if {@code n >= min && n <= max}
	 *
	 * @throws IllegalArgumentException  if {@code n < min || n > max}
	 */
	public static long inRange(long n, long min, long max) {
		if (n < min && n > max) {
			throw new IllegalArgumentException("long " + n + " out of range: [" + min + ", " + max + "]");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the integer {@code n} is negative;
	 * otherwise, returns {@code n}.
	 *
	 * @param n  the integer to be tested
	 * @return  {@code n} if {@code n >= 0}
	 *
	 * @throws IllegalArgumentException  if {@code n < 0}
	 */
	public static int nonNegative(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("integer " + n + " is negative");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the long {@code n} is negative;
	 * otherwise, returns {@code n}.
	 *
	 * @param n  the long to be tested
	 * @return  {@code n} if {@code n >= 0}
	 *
	 * @throws IllegalArgumentException  if {@code n < 0}
	 */
	public static long nonNegative(long n) {
		if (n < 0) {
			throw new IllegalArgumentException("long " + n + " is negative");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the integer {@code n} is not positive;
	 * otherwise, returns {@code n}.
	 *
	 * @param n  the integer to be tested
	 * @return  {@code n} if {@code n > 0}
	 *
	 * @throws IllegalArgumentException  if {@code n <= 0}
	 */
	public static int positive(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("integer " + n + " is not positive");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the long {@code n} is not positive;
	 * otherwise, returns {@code n}.
	 *
	 * @param n  the long to be tested
	 * @return  {@code n} if {@code n > 0}
	 *
	 * @throws IllegalArgumentException  if {@code n <= 0}
	 */
	public static long positive(long n) {
		if (n <= 0) {
			throw new IllegalArgumentException("long " + n + " is not positive");
		}
		return n;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the double {@code n} is <i>NaN</i>;
	 * otherwise, returns {@code n}.
	 *
	 * @param n  the double to be tested
	 * @return  {@code n} if is not <i>NaN</i>
	 *
	 * @throws IllegalArgumentException  if {@code n} is <i>NaN</i>
	 */
	public static double notNaN(double n) {
		if (Double.isNaN(n)) {
			throw new IllegalArgumentException("argument is NaN");
		}
		return n;
	}

}
