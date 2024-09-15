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
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class AtomicFileIntegrationTest {

    @Test
    fun `case 0 when value written will be persisted as an independent file`() {
        val file = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
        val fileOutputStream = fileOutputStream(file)
        fileOutputStream.write("123".toByteArray(StandardCharsets.UTF_8))
        fileOutputStream.close()
        val atomicFile = AtomicFile(file)
        val readFully = atomicFile.readFully()
        Truth.assertThat(readFully).isEqualTo("123".toByteArray(StandardCharsets.UTF_8))
    }

    private fun fileOutputStream(file: File): FileOutputStream {
        return try {
            file.outputStream()
        } catch (e: FileNotFoundException) {
            val parentFile = file.parentFile ?: throw e
            val failed = checkWithLock(parentFile)
            if (failed) {
                throw e
            } else {
                file.outputStream()
            }
        }
    }

    @Synchronized
    private fun checkWithLock(parentFile: File): Boolean {
        return !parentFile.exists() && !parentFile.mkdirs()
    }

    private fun app(): Application {
        return ApplicationProvider.getApplicationContext()
    }
}
