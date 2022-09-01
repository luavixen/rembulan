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

package dev.foxgirl.rembulan.env;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;

/**
 * The runtime environment used by functions in the standard library.
 *
 * <p>To obtain instances of this interface, use the utility class {@link RuntimeEnvironments}.</p>
 */
public interface RuntimeEnvironment {

	/**
	 * Returns the standard input stream.
	 *
	 * @return  the standard input stream
	 */
	InputStream standardInput();

	/**
	 * Returns the standard output stream.
	 *
	 * @return  the standard output stream
	 */
	OutputStream standardOutput();

	/**
	 * Returns the standard error stream.
	 *
	 * @return  the standard output stream
	 */
	OutputStream standardError();

	/**
	 * Returns the file system.
	 *
	 * @return  the file system
	 */
	FileSystem fileSystem();

	/**
	 * Returns the value of the environment variable {@code name}, or {@code null} when
	 * {@code name} is not defined.
	 *
	 * @param name  the name of the environment variable, must not be {@code null}
	 * @return  the value of the environment variable {@code name} (possibly {@code null})
	 *
	 * @throws NullPointerException  if {@code name} is {@code null}
	 */
	String getEnv(String name);

	/**
	 * Returns an approximation of the CPU time used by this program in seconds.
	 *
	 * @return  an approximation of the CPU time used by this program in seconds
	 *
	 * @throws UnsupportedOperationException  if this operation is not supported by the
	 *                                        runtime environment
	 */
	double getCpuTime();

}
