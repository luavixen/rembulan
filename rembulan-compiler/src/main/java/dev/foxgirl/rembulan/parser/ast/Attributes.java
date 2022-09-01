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

package dev.foxgirl.rembulan.parser.ast;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Attributes {

	private final Map<Class<?>, Object> attribs;

	private static final Attributes EMPTY = new Attributes(Collections.<Class<?>, Object>emptyMap());

	private Attributes(Map<Class<?>, Object> attribs) {
		this.attribs = Objects.requireNonNull(attribs);
	}

	public static Attributes empty() {
		return EMPTY;
	}

	public static Attributes of(Object... objects) {
		if (objects.length > 0) {
			Map<Class<?>, Object> as = new HashMap<>();
			for (Object o : objects) {
				as.put(o.getClass(), o);
			}
			return new Attributes(Collections.unmodifiableMap(as));
		}
		else {
			return empty();
		}
	}

	public Attributes with(Object o) {
		Objects.requireNonNull(o);
		Class<?> clazz = o.getClass();

		if (Objects.equals(attribs.get(clazz), o)) {
			return this;
		}
		else {
			Map<Class<?>, Object> as = new HashMap<>();
			as.putAll(attribs);
			as.put(clazz, o);
			return new Attributes(Collections.unmodifiableMap(as));
		}
	}

	public <T> T get(Class<T> clazz) {
		Objects.requireNonNull(clazz);
		Object result = attribs.get(clazz);

		if (result != null) {
			if (clazz.isAssignableFrom(result.getClass())) {
				@SuppressWarnings("unchecked")
				T r = (T) result;
				return r;
			}
			else {
				throw new IllegalStateException("Illegal entry for " + clazz.getName());
			}
		}
		else {
			return null;
		}
	}

	public boolean has(Class<?> clazz) {
		return get(clazz) != null;
	}

}
