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

package dev.foxgirl.rembulan.lib.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class SeekableOutputStream extends OutputStream implements SeekableStream {

	private final OutputStream out;
	private long position;

	public SeekableOutputStream(OutputStream out) {
		this.out = Objects.requireNonNull(out);
		this.position = 0L;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
		position += 1;
	}

	@Override
	public long getPosition() {
		return position;
	}

	@Override
	public long setPosition(long newPosition) {
		position = newPosition;
		return position;
	}

	@Override
	public long addPosition(long offset) {
		long newPosition = position + offset;
		if (position < 0) {
			throw new IllegalArgumentException("Illegal argument");
		}
		else {
			return setPosition(newPosition);
		}
	}

}
