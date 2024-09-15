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
import net.jodah.concurrentunit.Waiter
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

    @Test(expected = AssertionError::class)
    fun `case 2 when accessed concurrently without fix it will trigger error`() {
        val targetFile = createTargetFile()
        val atomicFile = AtomicFile(targetFile)
        executeConcurrent { atomicFile.createFileOutputStreamForWrite(false ) }
    }

    @Test
    fun `case 3 when accessed concurrently with fix it will not trigger error`() {
        val targetFile = createTargetFile()
        val atomicFile = AtomicFile(targetFile)
        executeConcurrent { atomicFile.createFileOutputStreamForWrite(true   ) }
    }

    private fun executeConcurrent(action: () -> Unit) {
        val waiter = Waiter()
        val threadCount = 5
        for (i in 0 until threadCount) {
            Thread {
                executeConcurrentInternally(action, waiter)
            }.start()
        }
        // Wait for all threads to complete
        waiter.await(1000L * threadCount)
    }

    private fun executeConcurrentInternally(action: () -> Unit, waiter: Waiter) {
        try {
            run(action)
        } catch (e: Exception) {
            waiter.fail(e)
        } finally {
            waiter.resume()
        }
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
