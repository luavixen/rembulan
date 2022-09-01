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

package dev.foxgirl.rembulan.exec;

/**
 * An exception thrown to indicate that an attempt to resume a continuation has failed
 * because the continuation object is invalid.
 */
public class InvalidContinuationException extends RuntimeException {

	/**
	 * Constructs a new instance of {@code InvalidContinuationException} with the given
	 * {@code message}.
	 *
	 * @param message  the message, may be {@code null}
	 */
	public InvalidContinuationException(String message) {
		super(message);
	}

}
