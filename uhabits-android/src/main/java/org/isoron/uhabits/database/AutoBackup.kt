/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.database

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.utils.DateFormats.Companion.getBackupDateFormat
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.DatabaseUtils
import java.text.SimpleDateFormat

class AutoBackup(private val context: Context) {

    //FIXME This works only because AutoBackup is reinstantiated everytime
    val preferences: Preferences = ( context.applicationContext as HabitsApplication).component.preferences
    val backupDir: DocumentFile = DocumentFile.fromTreeUri(context, preferences.backupPath.toUri())!!
    val backupFileNameTemplate = "Loop Habits Backup %s.db"
    val backupDateFormat: SimpleDateFormat = getBackupDateFormat()

    fun run(keep: Int = 5, backupEveryMs: Long = DateUtils.DAY_LENGTH) {
        Log.i("AutoBackup", "Starting automatic backups inside ${backupDir.uri}...")

        val backupFiles = listBackupFilesByDescendingTimestamp()
        removeOldestIfAny(backupFiles, keep)

        val newestTimestamp = backupFiles.getOrNull(0)?.lastModified() ?: 0L
        val nowTimestamp = DateUtils.getLocalTime()

        if (nowTimestamp - newestTimestamp > backupEveryMs) {
            executeBackups(nowTimestamp)
        } else {
            Log.i("AutoBackup", "Fresh backup found (timestamp=$newestTimestamp)")
        }
    }
    private fun executeBackups(nowTimestamp: Long) {
        val parsedDate = backupDateFormat.format(nowTimestamp)
        val datedFilename = backupFileNameTemplate.format(parsedDate)
        DatabaseUtils.saveDatabaseCopy(context, backupDir, datedFilename)

        val latestFilename = backupFileNameTemplate.format("latest")
        DatabaseUtils.saveDatabaseCopy(context, backupDir, latestFilename)
    }

    private fun listBackupFilesByDescendingTimestamp(): Array<DocumentFile> {
        val backupFiles: Array<DocumentFile> = backupDir.listFiles()
        backupFiles.sortByDescending { it.lastModified() }
        return backupFiles
    }

    private fun removeOldestIfAny(backupFiles: Array<DocumentFile>, keep: Int) {
        Log.d("AutoBackup", "Removing oldest than first $keep files")
        backupFiles.drop(keep).forEach { it.delete() }
    }
}
