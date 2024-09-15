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

import android.app.Application
import com.google.common.util.concurrent.ListenableFuture
import com.uber.simplestore.NamespaceConfig
import com.uber.simplestore.primitive.PrimitiveSimpleStore
import com.uber.simplestore.primitive.PrimitiveSimpleStoreFactory
import io.reactivex.Single

internal class PrimitiveSimpleStoreRepo(app: Application) {

    private val dataStore = dataStore(app)

    fun writeSingle(counter: Long, key: String): Single<Long> {
        return Single.fromFuture(listenableFuture(key, counter))
    }


    private fun listenableFuture(key: String, counter: Long): ListenableFuture<Long> {
        return dataStore.put(key, counter)
    }

    fun writeString(value: String, key: String): Single<String> {
        return Single.fromFuture(dataStore.putString(key, value))
    }

    fun readSingle(key: String): Single<Long> {
        return Single.fromFuture(dataStore.getLong(key))
    }

    /** No more read fallback as it is new API added. */
    fun readString(key: String): Single<String> {
        return Single.fromFuture(dataStore.getString(key))
    }


    fun close() {
        dataStore.close()
    }

    companion object {

        private fun dataStore(app: Application): PrimitiveSimpleStore {
            return PrimitiveSimpleStoreFactory.create(
                AndroidDirectoryProvider(app),
                "657b3cd7-f689-451b-aca0-628de60aa234",
                NamespaceConfig.CRITICAL
            )
        }

    }
}
