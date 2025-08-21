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
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.apache.commons.lang3.StringUtils
import org.isoron.uhabits.BaseUserInterfaceTest
import org.isoron.uhabits.acceptance.steps.CommonSteps.clickText
import org.isoron.uhabits.acceptance.steps.CommonSteps.launchApp
import org.isoron.uhabits.acceptance.steps.CommonSteps.pressBack
import org.isoron.uhabits.acceptance.steps.CommonSteps.scrollToText
import org.isoron.uhabits.acceptance.steps.ListHabitsSteps.MenuItem.SETTINGS
import org.isoron.uhabits.acceptance.steps.ListHabitsSteps.clickMenu
import org.isoron.uhabits.core.utils.DateFormats
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoBackupTest : BaseUserInterfaceTest() {
    val backupFileName = "loop_habits_tracker_backup_"

    @Test
    fun testAutoBackup_WhenFolderIsEmpty_SelectPathAndExecuteBackup() {
        launchApp()
        assertBackupPathSelection(true, "Tap to choose a folder to enable auto export!")

        backupPathSelection()
        assertBackupPathSelection(false, "Exported backups will be saved inside ->${prefs.backupPath}")

        // get and cleanup backup selected path
        val baseDirPath = prefs.backupPath
        val baseDirTreeUri = Uri.parse(baseDirPath)
        val baseDirDocumentFile = DocumentFile.fromTreeUri(ApplicationProvider.getApplicationContext<Context>(), baseDirTreeUri)!!
        cleanUpDatabaseFiles(baseDirDocumentFile)

        assertEquals(0, baseDirDocumentFile.listFiles().count())

        // trigger auto backup
        triggerAutoBackupInDate("2025-08-19 000000")

        // assert backup exist
        assertEquals(2, baseDirDocumentFile.listFiles().count())
        assertBackupExists(baseDirDocumentFile, "latest")
        assertBackupExists(baseDirDocumentFile, "2025-08-19 000000")
    }

    @Test
    fun testAutoBackup_WhenFolderIsNOTEmpty_SelectPathAndExecuteBackupAndRemoveOldOnes() {
        launchApp()
        assertBackupPathSelection(true, "Tap to choose a folder to enable auto export!")

        backupPathSelection()
        assertBackupPathSelection(false, "Exported backups will be saved inside ->${prefs.backupPath}")

        // get and cleanup backup selected path
        val baseDirPath = prefs.backupPath
        val baseDirTreeUri = Uri.parse(baseDirPath)
        val baseDirDocumentFile = DocumentFile.fromTreeUri(ApplicationProvider.getApplicationContext<Context>(), baseDirTreeUri)!!
        cleanUpDatabaseFiles(baseDirDocumentFile)

        assertEquals(0, baseDirDocumentFile.listFiles().count())

        // trigger auto backup
        triggerAutoBackupInDate("2125-08-11 000000")
        triggerAutoBackupInDate("2125-08-13 000000")
        triggerAutoBackupInDate("2125-08-15 000000")
        triggerAutoBackupInDate("2125-08-17 000000")
        triggerAutoBackupInDate("2125-08-19 000000")

        // assert backup exist
        assertEquals(6, baseDirDocumentFile.listFiles().count())
        assertBackupExists(baseDirDocumentFile, "latest")
        assertBackupExists(baseDirDocumentFile, "2125-08-11 000000")
        Thread.sleep(1000) // so it is considered older than the others.
        assertBackupExists(baseDirDocumentFile, "2125-08-13 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-15 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-17 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-19 000000")

        // when add new backup, all previous are deleted
        triggerAutoBackupInDate("2125-08-21 000000")

        assertEquals(6, baseDirDocumentFile.listFiles().count())
        assertBackupExists(baseDirDocumentFile, "latest")
        assertBackupNOTExists(baseDirDocumentFile, "2125-08-11 000000") // not exists anymore
        assertBackupExists(baseDirDocumentFile, "2125-08-13 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-15 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-17 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-19 000000")
        assertBackupExists(baseDirDocumentFile, "2125-08-21 000000")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAutoBackup_WhenBackupPathIsNotSelected_NoBackupOccurs_AndThrowExceptionIfTryToAccessDocumentFile() {
        launchApp()
        assertBackupPathSelection(true, "Tap to choose a folder to enable auto export!")

        // trigger auto backup
        triggerAutoBackupInDate("2025-08-19 000000")

        // should throw illegalArgumentexception
        val baseDirPath = prefs.backupPath
        val baseDirTreeUri = Uri.parse(baseDirPath)
        DocumentFile.fromTreeUri(ApplicationProvider.getApplicationContext<Context>(), baseDirTreeUri)!!
    }

    private fun triggerAutoBackupInDate(backupDateString: String) {
        DateUtils.setFixedLocalTime(DateFormats.getBackupDateFormat().parse(backupDateString)?.time)
        clickMenu(SETTINGS)
        pressBack()
    }

    private fun assertBackupExists(baseDirDocumentFile: DocumentFile, filename: String) {
        assertTrue(baseDirDocumentFile.findFile("$backupFileName$filename.db")?.exists()!!)
    }
    private fun assertBackupNOTExists(baseDirDocumentFile: DocumentFile, filename: String) {
        assertEquals(null, baseDirDocumentFile.findFile("$backupFileName$filename.db"))
    }

    private fun assertBackupPathSelection(assertBlank: Boolean, backupPathSelectionActionDescription: String) {
        if (assertBlank) {
            assertTrue(StringUtils.isBlank(prefs.backupPath))
        } else {
            assertTrue(StringUtils.isNotBlank(prefs.backupPath))
        }

        clickMenu(SETTINGS)
        scrollToText("Auto export folder selection")
        assertTrue(device.findObject(UiSelector().text(backupPathSelectionActionDescription)).exists())
        pressBack()
    }

    private fun backupPathSelection() {
        clickMenu(SETTINGS)
        clickText("Auto export folder selection")

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) { // 28
            clickButton("SELECT")
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) { // 29
            clickButton("ALLOW ACCESS TO \"DOWNLOADS\"")
            clickButton("ALLOW")
        } else {
            clickButton("USE THIS FOLDER")
            clickButton("ALLOW")
        }

        device.waitForIdle()
    }

    private fun clickButton(textButton: String) {
        val button = device.findObject(UiSelector().text(textButton))
        if (button.exists() && button.isEnabled) {
            button.click()
        }
    }

    private fun cleanUpDatabaseFiles(dir: DocumentFile) {
        dir.listFiles().forEach { file ->
            assertTrue(file.delete())
        }
    }
}
