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
import com.uber.simplestore.impl.ConcurrentTestUtil.executeConcurrent
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException


@RunWith(RobolectricTestRunner::class)
class AtomicFileConcurrentTest {

    @Test(expected = FileNotFoundException::class)
    fun `case 0 when parent folder is absent will trigger FileNotFoundException`() {
        //arrange
        //makeParentFolder()
        val targetFile = createTargetFile()
        //act
        val target = targetFile.outputStream()
        //assert
        assertThat(target).isNotNull()
    }

    @Test
    fun `case 1 when parent folder is present will not trigger FileNotFoundException`() {
        //arrange
        makeParentFolder()
        val targetFile = createTargetFile()
        //act
        val target = targetFile.outputStream()
        //assert
        assertThat(target).isNotNull()
    }

    @Ignore("failing_on_ci")
    @Test(expected = AssertionError::class)
    fun `case 2 when accessed concurrently without fix it will trigger error`() {
        val baseFile = createTargetFile()
        val atomicFile = AtomicFile(baseFile)
        executeConcurrent { atomicFile.createFileOutputStreamForWrite(false) }
    }

    @Test
    fun `case 3 when accessed concurrently with fix it will not trigger error`() {
        val baseFile = createTargetFile()
        val atomicFile = AtomicFile(baseFile)
        executeConcurrent { atomicFile.createFileOutputStreamForWrite(true) }
    }


    private fun createTargetFile(): File {
        return File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
    }

    private fun makeParentFolder() {
        val parent = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234")
        parent.mkdirs()
    }

    private fun app(): Application {
        return ApplicationProvider.getApplicationContext()
    }
}
