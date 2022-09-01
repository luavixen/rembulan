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

import dev.foxgirl.rembulan.*;

import java.util.Objects;

/**
 * A value type namer that looks up type names in object metatables, and distinguishes
 * light userdata from full userdata.
 *
 * <p>The namer first tries to find the key {@code "__name"} (see {@link BasicLib#MT_NAME})
 * in the object's metatable. If a value with such key exists and is a string, it is
 * used as the type name. Otherwise, light userdata is assigned the type name
 * {@code "light userdata"} ({@link BasicLib#TYPENAME_LIGHT_USERDATA}), and the rest is
 * assigned the same name as assigned by {@link PlainValueTypeNamer}.</p>
 *
 */
public class NameMetamethodValueTypeNamer implements ValueTypeNamer {

	private final MetatableProvider metatableProvider;

	/**
	 * Creates a new instance of this value type namer that uses the supplied
	 * metatable provider {@code metatableProvider} for looking up type names.
	 *
	 * @param metatableProvider  the metatable provider, must not be {@code null}
	 * @throws NullPointerException  if {@code metatableProvider} is {@code null}
	 */
	public NameMetamethodValueTypeNamer(MetatableProvider metatableProvider) {
		this.metatableProvider = Objects.requireNonNull(metatableProvider);
	}

	/**
	 * Returns the type name (a string) of the given value {@code instance}, using
	 * {@code metatableProvider} to look up the the name in the {@code "__name"} field
	 * of {@code instance}'s metatable, if it is defined.
	 *
	 * @param instance  the object instance, may be {@code null}
	 * @param metatableProvider  the metatable provider, must not be {@code null}
	 * @return  type name of {@code instance}
	 *
	 * @throws NullPointerException  if {@code metatableProvider} is {@code null}
	 */
	public static ByteString typeNameOf(Object instance, MetatableProvider metatableProvider) {
		Object nameField = Metatables.getMetamethod(metatableProvider, BasicLib.MT_NAME, instance);
		if (nameField instanceof ByteString) {
			return (ByteString) nameField;
		}
		else if (nameField instanceof String) {
			return ByteString.of((String) nameField);
		}
		else {
			if (LuaType.isLightUserdata(instance)) {
				return BasicLib.TYPENAME_LIGHT_USERDATA;
			}
			else {
				return PlainValueTypeNamer.INSTANCE.typeNameOf(instance);
			}
		}
	}

	@Override
	public ByteString typeNameOf(Object instance) {
		return typeNameOf(instance, metatableProvider);
	}

}
