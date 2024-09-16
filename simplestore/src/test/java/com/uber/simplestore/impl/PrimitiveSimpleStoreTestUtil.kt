/*
 * Copyright (C) 2019. Uber Technologies
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
package com.uber.simplestore.impl


object PrimitiveSimpleStoreTestUtil {

    fun transformToLong(b: ByteArray): Long {
        return b[0].toLong() and 0xFFL shl 56 or
                (b[1].toLong() and 0xFFL shl 48) or
                (b[2].toLong() and 0xFFL shl 40) or
                (b[3].toLong() and 0xFFL shl 32) or
                (b[4].toLong() and 0xFFL shl 24) or
                (b[5].toLong() and 0xFFL shl 16) or
                (b[6].toLong() and 0xFFL shl 8) or
                (b[7].toLong() and 0xFFL)
    }

}
