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
package com.uber.simplestore.impl;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.jetbrains.annotations.Nullable;

/*
 Forked from AOSP to return failures instead of logcatting.
*/

/**
 * Static library support version of the framework's {@link android.util.AtomicFile}, a helper class
 * for performing atomic operations on a file by creating a backup file until a write has
 * successfully completed.
 *
 * <p>Atomic file guarantees file integrity by ensuring that a file has been completely written and
 * sync'd to disk before renaming it to the original file. Previously this is done by renaming the
 * original file to a backup file beforehand, but this approach couldn't handle the case where the
 * file is created for the first time. This class will also handle the backup file created by the
 * old implementation properly.
 *
 * <p>Atomic file does not confer any file locking semantics. Do not use this class when the file
 * may be accessed or modified concurrently by multiple threads or processes. The caller is
 * responsible for ensuring appropriate mutual exclusion invariants whenever it accesses the file.
 */
final class AtomicFile {

  private static final String LOG_TAG = "AtomicFile";
  private final File mBaseName;
  private final File mNewName;
  private final File mLegacyBackupName;

  /**
   * Create a new AtomicFile for a file located at the given File path. The secondary backup file
   * will be the same file path with ".bak" appended.
   */
  public AtomicFile(File baseName) {
    mBaseName = baseName;
    mNewName = new File(baseName.getPath() + ".new");
    mLegacyBackupName = new File(baseName.getPath() + ".bak");
  }

  /**
   * Return the path to the base file. You should not generally use this, as the data at that path
   * may not be valid.
   */
  public File getBaseFile() {
    return mBaseName;
  }

  /** Delete the atomic file. This deletes both the base and backup files. */
  public void delete() {
    mBaseName.delete();
    mNewName.delete();
    mLegacyBackupName.delete();
  }

  /**
   * Start a new write operation on the file. This returns a FileOutputStream to which you can write
   * the new file data. The existing file is replaced with the new data. You <em>must not</em>
   * directly close the given FileOutputStream; instead call either {@link
   * #finishWrite(FileOutputStream)} or {@link #failWrite(FileOutputStream)}.
   *
   * <p>Note that if another thread is currently performing a write, this will simply replace
   * whatever that thread is writing with the new file being written by this thread, and when the
   * other thread finishes the write the new write operation will no longer be safe (or will be
   * lost). You must do your own threading protection for access to AtomicFile.
   */
  public FileOutputStream startWrite() throws IOException {
    if (mLegacyBackupName.exists()) {
      rename(mLegacyBackupName, mBaseName);
    }

    try {
      return new FileOutputStream(mNewName);
    } catch (FileNotFoundException e) {
      File parent = mNewName.getParentFile();
        makeParentIfAbsent(parent);
        try {
        return new FileOutputStream(mNewName);
      } catch (FileNotFoundException e2) {
        throw new IOException("Failed to create new file " + mNewName, e2);
      }
    }
  }

    private void makeParentIfAbsent(File parent) throws IOException {
        if (!parent.exists()){
          boolean mkdirs = parent.mkdirs();
          if (!mkdirs&& !parent.isDirectory()) {
            throw new IOException("Failed to create directory for " + mNewName);
          }
        }
    }

    /**
   * Call when you have successfully finished writing to the stream returned by {@link
   * #startWrite()}. This will close, sync, and commit the new data. The next attempt to read the
   * atomic file will return the new file stream.
   */
  public void finishWrite(@Nullable FileOutputStream str) {
    if (str == null) {
      return;
    }
    if (!sync(str)) {
      Log.e(LOG_TAG, "Failed to sync file output stream");
    }
    try {
      str.close();
    } catch (IOException e) {
      Log.e(LOG_TAG, "Failed to close file output stream");
    }
    rename(mNewName, mBaseName);
  }

  /**
   * Call when you have failed for some reason at writing to the stream returned by {@link
   * #startWrite()}. This will close the current write stream, and delete the new file.
   */
  public void failWrite(@Nullable FileOutputStream str) {
    if (str == null) {
      return;
    }
    if (!sync(str)) {
      Log.e(LOG_TAG, "Failed to sync file output stream");
    }
    try {
      str.close();
    } catch (IOException e) {
      Log.e(LOG_TAG, "Failed to close file output stream");
    }
    if (!mNewName.delete()) {
      Log.e(LOG_TAG, "Failed to delete new file " + mNewName);
    }
  }

  /**
   * Open the atomic file for reading. You should call close() on the FileInputStream when you are
   * done reading from it.
   *
   * <p>You must do your own threading protection for access to AtomicFile.
   */
  public FileInputStream openRead() throws FileNotFoundException {
    if (mLegacyBackupName.exists()) {
      rename(mLegacyBackupName, mBaseName);
    }

    // It was okay to call openRead() between startWrite() and finishWrite() for the first time
    // (because there is no backup file), where openRead() would open the file being written,
    // which makes no sense, but finishWrite() would still persist the write properly. For all
    // subsequent writes, if openRead() was called in between, it would see a backup file and
    // delete the file being written, the same behavior as our new implementation. So we only
    // need a special case for the first write, and don't delete the new file in this case so
    // that finishWrite() can still work.
    if (mNewName.exists() && mBaseName.exists()) {
      if (!mNewName.delete()) {
        Log.e(LOG_TAG, "Failed to delete outdated new file " + mNewName);
      }
    }
    return new FileInputStream(mBaseName);
  }

  /**
   * A convenience for {@link #openRead()} that also reads all of the file contents into a byte
   * array which is returned.
   */
  public byte[] readFully() throws IOException {
    FileInputStream stream = openRead();
    try {
      int pos = 0;
      int avail = stream.available();
      byte[] data = new byte[avail];
      while (true) {
        int amt = stream.read(data, pos, data.length - pos);
        // Log.i("foo", "Read " + amt + " bytes at " + pos
        //        + " of avail " + data.length);
        if (amt <= 0) {
          // Log.i("foo", "**** FINISHED READING: pos=" + pos
          //        + " len=" + data.length);
          return data;
        }
        pos += amt;
        avail = stream.available();
        if (avail > data.length - pos) {
          byte[] newData = new byte[pos + avail];
          System.arraycopy(data, 0, newData, 0, pos);
          data = newData;
        }
      }
    } finally {
      stream.close();
    }
  }

  private static boolean sync(FileOutputStream stream) {
    try {
      stream.getFD().sync();
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  private static void rename(File source, File target) {
    // We used to delete the target file before rename, but that isn't atomic, and the rename()
    // syscall should atomically replace the target file. However in the case where the target
    // file is a directory, a simple rename() won't work. We need to delete the file in this
    // case because there are callers who erroneously called mBaseName.mkdirs() (instead of
    // mBaseName.getParentFile().mkdirs()) before creating the AtomicFile, and it worked
    // regardless, so this deletion became some kind of API.
    if (target.isDirectory()) {
      if (!target.delete()) {
        Log.e(LOG_TAG, "Failed to delete file which is a directory " + target);
      }
    }
    if (!source.renameTo(target)) {
      Log.e(LOG_TAG, "Failed to rename " + source + " to " + target);
    }
  }
}
