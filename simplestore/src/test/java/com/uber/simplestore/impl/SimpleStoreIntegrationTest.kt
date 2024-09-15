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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class SimpleStoreIntegrationTest {

    @Test
    fun `case 0 when value written will be persisted as an independent file`() {
        generateFile()
        assertFile1()
        assertFile2()
    }

    private fun generateFile() {
        val repo = repo()
        val test1 = repo.writeSingle(123, "file1").test()
        val test2 = repo.writeString("456", "file2").test()
        test1.assertValue(123)
        test2.assertValue("456")
        repo.close()
    }

    private fun assertFile1() {
        File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/file1").apply {
            assertThat(exists()).isTrue()
            val atomicFile = AtomicFile(this)
            val readFully = atomicFile.readFully()
            val bytes = readFully
            val value = PrimitiveSimpleStoreTestUtil.transformToLong(bytes)
            assertThat(value).isNotEqualTo("123")
            assertThat(value).isEqualTo(123)
            val repo = repo()
            repo.readSingle("file1").test().assertValue(123)
            repo.close()
        }
    }

    private fun assertFile2() {
        File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/file2").apply {
            assertThat(exists()).isTrue()
            val atomicFile = AtomicFile(this)
            val readFully = atomicFile.readFully()
            val bytes = readFully
            val value =String(bytes, StandardCharsets.UTF_16BE)
            assertThat(value).isEqualTo("456")
            val repo = repo()
            repo.readString("file2").test().assertValue("456")
            repo.close()
        }
    }

    private fun repo(): PrimitiveSimpleStoreRepo {
        return PrimitiveSimpleStoreRepo(app())
    }

    private fun app(): Application {
        return ApplicationProvider.getApplicationContext()
    }
}
