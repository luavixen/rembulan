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

package dev.foxgirl.rembulan.lib;

import dev.foxgirl.rembulan.ByteString;
import dev.foxgirl.rembulan.LuaRuntimeException;

public class AssertionFailedException extends LuaRuntimeException {

	public AssertionFailedException(ByteString message) {
		super(message);
	}

	public AssertionFailedException(String message) {
		super(ByteString.of(message));
	}

	public AssertionFailedException(Object errorObject) {
		super(errorObject);
	}

}
