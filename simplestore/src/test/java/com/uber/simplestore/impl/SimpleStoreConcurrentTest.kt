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
import androidx.test.core.app.ApplicationProvider
import com.uber.simplestore.impl.ConcurrentTestUtil.executeConcurrent
import com.uber.simplestore.primitive.PrimitiveSimpleStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleStoreConcurrentTest {


    @Test
    fun `when accessed concurrently by same store instance without fix  will not trigger error`() {
        val store: PrimitiveSimpleStore = PrimitiveSimpleStoreRepo.createInstance(app())
        executeConcurrent {
            for (i in 0..10) {
                store.putString("key_$i", "value:$i").get()
            }
        }
    }


    @Test(expected = IllegalStateException::class)
    fun `when two name space created it will trigger error`() {
        val store: PrimitiveSimpleStore = PrimitiveSimpleStoreRepo.createInstance(app())
        val store2: PrimitiveSimpleStore = PrimitiveSimpleStoreRepo.createInstance(app())
    }


    private fun app(): Application {
        return ApplicationProvider.getApplicationContext()
    }
}
