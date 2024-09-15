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
import java.lang.AssertionError


@RunWith(RobolectricTestRunner::class)
class AtomicFileConcurrentTest {

    @Test(expected = FileNotFoundException::class)
    fun `case 0 when parent folder is absent will trigger FileNotFoundException`() {
        //arrange
        val targetFile = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
        //act
        val target = targetFile.outputStream()
        //assert
        assertThat(target).isNotNull()
    }

    @Test
    fun `case 1 when parent folder is present will not trigger FileNotFoundException`() {
        //arrange
        makeParentFolder()
        val targetFile = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
        //act
        val target = targetFile.outputStream()
        //assert
        assertThat(target).isNotNull()
    }

    @Test(expected = AssertionError::class)
    fun `case 2 when accessed concurrently without fix it will trigger error`() {
        val targetFile = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
        val atomicFile = AtomicFile(targetFile)
        val waiter = Waiter()
        val threadCount = 5

        for (i in 0 until threadCount) {
            Thread {
                try {
                    // Simulate some work
                    testCreate(atomicFile)
                    // Perform assertions
                    waiter.assertTrue(true)
                } catch (e: Exception) {
                    waiter.fail(e)
                } finally {
                    // Signal that this thread is done
                    waiter.resume()
                }
            }.start()
        }

        // Wait for all threads to complete
        waiter.await(1000L * threadCount)
    }
    @Test
    fun `case 3 when accessed concurrently with fix it will not trigger error`() {
        val targetFile = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234/random_key")
        val atomicFile = AtomicFile(targetFile)
        val waiter = Waiter()
        val threadCount = 5

        for (i in 0 until threadCount) {
            Thread {
                try {
                    // Simulate some work
                    testCreate2(atomicFile)
                    // Perform assertions
                    waiter.assertTrue(true)
                } catch (e: Exception) {
                    waiter.fail(e)
                } finally {
                    // Signal that this thread is done
                    waiter.resume()
                }
            }.start()
        }

        // Wait for all threads to complete
        waiter.await(1000L * threadCount)
    }

    private fun testCreate2(atomicFile: AtomicFile) {
        atomicFile.createFileOutputStreamForWrite(true  )
    }
    private fun testCreate(atomicFile: AtomicFile) {
        atomicFile.createFileOutputStreamForWrite(false)
    }

    private fun makeParentFolder() {
        val parent = File(app().dataDir, "files/simplestore/657b3cd7-f689-451b-aca0-628de60aa234")
        parent.mkdirs()
    }

    private fun app(): Application {
        return ApplicationProvider.getApplicationContext()
    }
}
