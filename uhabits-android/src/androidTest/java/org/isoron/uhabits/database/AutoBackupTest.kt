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
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
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
        assertBackupPathSelection("", "Tap to choose a folder to enable auto export!")

        backupPathSelection()
        assertBackupPathSelection("content://com.android.externalstorage.documents/tree/primary%3ADocuments", "Exported backups will be saved inside ->${prefs.backupPath}")

        //get and cleanup backup selected path
        val baseDirPath = prefs.backupPath
        val baseDirTreeUri = Uri.parse(baseDirPath)
        val baseDirDocumentFile = DocumentFile.fromTreeUri(ApplicationProvider.getApplicationContext<Context>(), baseDirTreeUri)!!
        cleanUpDatabaseFiles(baseDirDocumentFile)

        assertEquals(0, baseDirDocumentFile.listFiles().count())

        //trigger auto backup
        triggerAutoBackupInDate("2025-08-19 000000")

        //assert backup exist
        assertEquals(2, baseDirDocumentFile.listFiles().count())
        assertBackupExists(baseDirDocumentFile, "latest")
        assertBackupExists(baseDirDocumentFile, "2025-08-19 000000")

    }

    private fun triggerAutoBackupInDate(backupDateString: String) {
        DateUtils.setFixedLocalTime(DateFormats.getBackupDateFormat().parse(backupDateString)?.time)
        clickMenu(SETTINGS)
        pressBack()
    }

    private fun assertBackupExists(baseDirDocumentFile: DocumentFile, filename: String) {
        assertTrue(baseDirDocumentFile.findFile("${backupFileName}${filename}.db")?.exists()!!)
    }

    private fun assertBackupPathSelection(backupPathSelection: String, backupPathSelectionActionDescription: String) {
        assertTrue(prefs.backupPath == backupPathSelection)
        clickMenu(SETTINGS)
        scrollToText("Auto export folder selection")
        assertTrue(device.findObject(UiSelector().text(backupPathSelectionActionDescription)).exists())
        pressBack()
    }

    private fun backupPathSelection() {
        clickMenu(SETTINGS)
        clickText("Auto export folder selection")

        //TODO move to commonSteps?
        val useFolderButton = device.findObject(UiSelector().text("USE THIS FOLDER"))
        if (useFolderButton.exists() && useFolderButton.isEnabled) {
            useFolderButton.click()
        }

        val allowButton = device.findObject(UiSelector().text("ALLOW"))
        if (allowButton.exists() && allowButton.isEnabled) {
            allowButton.click()
        }
        device.waitForIdle()
    }


/*
    @Test
    fun testRun_whenEmptyFolder_executeBackup() {
        //DateUtils.setFixedLocalTime(40 * DateUtils.DAY_LENGTH)
        //val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        val baseDirPath = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
        val baseDirTreeUri = Uri.parse(baseDirPath)
        val baseDirDocumentFile = DocumentFile.fromTreeUri(targetContext, baseDirTreeUri)!!
        prefs.backupPath = baseDirPath
        assertEquals(0, baseDirDocumentFile.listFiles().count())

        val autoBackup = AutoBackup(targetContext)
        autoBackup.run(keep = 5)

        assertEquals(2, baseDirDocumentFile.listFiles().count())


        //assertEquals(30, baseDirDocumentFile.listFiles().count())

        //for (k in 1..25) assertDoesNotExist("${basedir.path}/$backupFileName-$k.db")
        //for (k in 26..30) assertExists("${basedir.path}/$backupFileName-$k.db")
        //assertExists("${basedir.path}/Loop Habits Backup 1970-02-10 000000.db")
    }*/

    /*
    @Test
    fun testRun_whenAlreadyExistingDatabases_cleanUpAndExecute() {
        DateUtils.setFixedLocalTime(40 * DateUtils.DAY_LENGTH)
        //val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        val baseDirPath = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
        val baseDirTreeUri = Uri.parse(baseDirPath)
        val baseDirDocumentFile = DocumentFile.fromTreeUri(targetContext, baseDirTreeUri)!!
        prefs.backupPath = baseDirPath

        cleanUpDatabaseFiles(baseDirDocumentFile)
        createAlreadyExistingDatabaseFiles(baseDirDocumentFile, 30)
        assertEquals(30, baseDirDocumentFile.listFiles().count())

        val autoBackup = AutoBackup(targetContext)
        autoBackup.run(keep = 5)

        //assertEquals(30, baseDirDocumentFile.listFiles().count())

        //for (k in 1..25) assertDoesNotExist("${basedir.path}/$backupFileName-$k.db")
        //for (k in 26..30) assertExists("${basedir.path}/$backupFileName-$k.db")
        //assertExists("${basedir.path}/Loop Habits Backup 1970-02-10 000000.db")
    }
*/

    /*
    @Test
    fun testRun_whenNoPreferenceSet_DoNothing() {
        DateUtils.setFixedLocalTime(40 * DateUtils.DAY_LENGTH)
        val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        createTestFiles(basedir, 30)

        val autoBackup = AutoBackup(targetContext)
        autoBackup.run(keep = 5)

        for (k in 1..30) assertExists("${basedir.path}/$backupFileName-$k.db")
        //assertExists("${basedir.path}/Loop Habits Backup 1970-02-10 000000.db")
    }*/

    /*
    @Test
    fun testRunWithEmptyDir() {
        val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        cleanUpDatabaseFiles(basedir)
        basedir.delete()

        // Should not crash
        val autoBackup = AutoBackup(targetContext)
        autoBackup.run()
    }*/
    /*
        private fun assertExists(path: String) {
            assertTrue("File $path should exist", File(path).exists())
        }

        private fun assertDoesNotExist(path: String) {
            assertFalse("File $path should not exist", File(path).exists())
        }


        private fun createAlreadyExistingDatabaseFiles(basedir: DocumentFile, nfiles: Int) {
            for (k in 1..nfiles) {
                basedir.createFile("application/octet-stream", "${backupFileName}-$k.db")
            //touch("${basedir.path}/${backupFileName}-$k.db", DateUtils.DAY_LENGTH * k)
            }
        }

        private fun touch(path: String, time: Long) {
            val file = File(path)
            FileOutputStream(file).close()
            file.setLastModified(time)
        }

     */
        private fun cleanUpDatabaseFiles(dir: DocumentFile) {
            dir.listFiles().forEach { file ->
                assertTrue(file.delete())
            }
        }

}
