package com.harish.mediaplayer.data.files

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renames a folder on the phone.
 *
 * Android 11+ only lets an app rename folders with "All files access" (Settings > Apps >
 * Special app access > All files access). The first time, we send the user there to switch it
 * on; after that renames happen straight away. Android updates its media list itself when a
 * folder is renamed, so every video / song keeps its id (resume spots, favourites... stay).
 */
@Singleton
class FolderRenamer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun rename(folderPath: String, newName: String): RenameResult = withContext(Dispatchers.IO) {
        if (!Environment.isExternalStorageManager()) {
            RenameResult.NeedsAllFilesAccess(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        } else {
            try {
                renameNow(File(folderPath), newName)
            } catch (e: Exception) {
                Log.w(TAG, "Folder rename failed for $folderPath", e)
                RenameResult.Failed("Couldn't rename this folder")
            }
        }
    }

    private fun renameNow(from: File, newName: String): RenameResult {
        val parent = from.parentFile
        if (parent == null || isProtected(from)) return RenameResult.Failed("Android doesn't allow renaming this folder")
        if (!from.isDirectory) return RenameResult.Failed("This folder isn't there any more. Tap refresh")

        val to = File(parent, newName)
        // "trip" -> "Trip": the phone sees these as the same name, so go through a temporary name
        val caseOnly = from.name.equals(newName, ignoreCase = true)
        if (!caseOnly && to.exists()) return RenameResult.Failed("Something in this folder already has this name")

        val ok = if (caseOnly) {
            val temp = File(parent, ".$newName.renaming")
            from.renameTo(temp) && (temp.renameTo(to) || temp.renameTo(from).let { false })
        } else {
            from.renameTo(to)
        }
        if (!ok) return RenameResult.Failed("Couldn't rename this folder")

        // Android normally updates its media list at once; a scan makes extra sure
        MediaScannerConnection.scanFile(context, arrayOf(to.path), null, null)
        return RenameResult.Renamed
    }

    /** Storage roots ("Internal storage", SD card) and the Android/ system folder stay untouched. */
    private fun isProtected(dir: File): Boolean {
        val path = dir.absolutePath.trimEnd('/')
        return ROOT.matches(path) || ANDROID_DIR.containsMatchIn(path)
    }

    private companion object {
        const val TAG = "FolderRenamer"
        val ROOT = Regex("^/storage(/emulated)?(/[^/]+)?$")              // /storage/emulated/0, /storage/1A2B-3C4D
        val ANDROID_DIR = Regex("^/storage/(emulated/\\d+|[^/]+)/Android(/|$)")
    }
}
