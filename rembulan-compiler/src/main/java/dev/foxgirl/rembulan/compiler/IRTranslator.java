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

package dev.foxgirl.rembulan.compiler;

import dev.foxgirl.rembulan.parser.ast.Chunk;

public class IRTranslator {

	public static Module translate(Chunk chunk) {
		ModuleBuilder moduleBuilder = new ModuleBuilder();
		IRTranslatorTransformer translator = new IRTranslatorTransformer(moduleBuilder);
		translator.transform(chunk);
		return moduleBuilder.build();
	}

}
