package com.harish.mediaplayer.data.files

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What happened when we tried to rename a file. */
sealed interface RenameResult {
    data object Renamed : RenameResult

    /**
     * The file belongs to another app (camera, downloads, WhatsApp...). Android shows its own
     * "Allow this app to modify this file?" popup; launch [request], then try again.
     */
    class NeedsPermission(val request: IntentSender) : RenameResult

    data class Failed(val message: String) : RenameResult
}

/**
 * Renames a file on the phone through MediaStore.
 *
 * Changing DISPLAY_NAME renames the real file on disk; the MediaStore id stays the same,
 * so everything we keep per id (resume spots, favourites, playlists...) carries over.
 */
@Singleton
class FileRenamer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun rename(uri: Uri, newFileName: String): RenameResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName) }
        try {
            if (resolver.update(uri, values, null, null) > 0) RenameResult.Renamed
            else RenameResult.Failed("Couldn't rename. The file may have been moved or deleted")
        } catch (e: SecurityException) {
            // Not created by this app: ask the user once with Android's own popup
            try {
                RenameResult.NeedsPermission(MediaStore.createWriteRequest(resolver, listOf(uri)).intentSender)
            } catch (e2: Exception) {
                Log.w(TAG, "No write request for $uri", e2)
                RenameResult.Failed("Android doesn't allow renaming this file")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Rename failed for $uri", e)
            RenameResult.Failed("Couldn't rename. A file with this name may already exist")
        }
    }

    private companion object {
        const val TAG = "FileRenamer"
    }
}
