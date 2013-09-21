/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evalon.chrysalis.functional;

/**
 * Implementations of this class perform work on the given parameter and return a result of an optionally different
 * type.
 *
 * @param <R> The type of the result of the apply operation
 * @param <V> The type of the input to the apply operation
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public interface Function<R, V> {

	/**
	 * Execute the logic of the action, accepting the given parameter.
	 *
	 * @param v The parameter to pass to the action.
	 *
	 * @return result
	 */
	R apply(V v);

}
