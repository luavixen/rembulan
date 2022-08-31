/*
 * Copyright 2016 Miroslav Janíček
 * Copyright 2022 Lua MacDougall <lua@foxgirl.dev>
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

package dev.foxgirl.rembulan;

import dev.foxgirl.rembulan.util.ByteIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An input stream that wraps a {@link ByteIterator}.
 */
public class ByteStringInputStream extends InputStream {

	private final ByteIterator iterator;

	/**
	 * Constructs a new input stream from the given byte iterator.
	 *
	 * @param iterator  byte iterator to wrap
	 * @throws NullPointerException  if {@code iterator} is null
	 */
	public ByteStringInputStream(ByteIterator iterator) {
		this.iterator = Objects.requireNonNull(iterator);
	}

	/**
	 * Constructs a new input stream from the given byte string.
	 *
	 * @param string  byte string to wrap
	 * @throws NullPointerException  if {@code string} is null
	 */
	public ByteStringInputStream(ByteString string) {
		this(string.iterator());
	}

	@Override
	public int read() throws IOException {
		return iterator.hasNext() ? iterator.nextByte() & 0xFF : -1;
	}

}
