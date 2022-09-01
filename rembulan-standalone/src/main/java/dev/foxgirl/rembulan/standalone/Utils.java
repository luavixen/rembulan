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

package dev.foxgirl.rembulan.standalone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

final class Utils {

	private Utils() {
		// not to be instantiated or extended
	}

	public static String bytesToString(byte[] bytes) {
		// Lua likes clean 8-bit input; FIXME: is ISO-8859-1 okay?
		Charset charset = Charset.forName("ISO-8859-1");
		return new String(bytes, charset);
	}

	public static String readFile(String fileName) throws IOException {
		Objects.requireNonNull(fileName);
		return bytesToString(Files.readAllBytes(Paths.get(fileName)));
	}

	public static String readInputStream(InputStream stream) throws IOException {
		// FIXME: this ia a quick-and-dirty hack

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int b;
		do {
			b = stream.read();
			if (b != -1) {
				baos.write(b);
			}
		} while (b != -1);

		return bytesToString(baos.toByteArray());
	}

	public static String skipLeadingShebang(String s) {
		if (s.startsWith("#")) {
			int endOfFirstLine = s.indexOf('\n');
			// don't skip the newline at the end of the shebang
			return endOfFirstLine >= 0 ? s.substring(endOfFirstLine) : "";
		}
		else {
			return s;
		}
	}

	public static boolean isVerbose() {
		return System.getenv(Constants.ENV_VERBOSE) != null;
	}

	public static void logClassPath(ClassLoader cl, String name) {
		if (isVerbose()) {
			System.err.println(name + ":");
			if (cl instanceof URLClassLoader) {
				for (URL url : ((URLClassLoader) cl).getURLs()) {
					System.err.println("\t" + url);
				}
			}
			else if (cl == null) {
				System.err.println("\t(none)");
			}
			else {
				System.err.println("\t(unknown)");
			}
		}
	}

}
