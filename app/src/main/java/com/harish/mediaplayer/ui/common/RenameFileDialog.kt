package com.harish.mediaplayer.ui.common

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.data.files.RenameResult
import com.harish.mediaplayer.domain.rename.NameCheck
import com.harish.mediaplayer.domain.rename.baseNameOf
import com.harish.mediaplayer.domain.rename.checkNewFileName
import com.harish.mediaplayer.domain.rename.extensionOf
import kotlinx.coroutines.launch

/**
 * "Rename" popup for a file.
 *
 * The box starts with the current name selected (type to replace it), the extension is shown
 * greyed after it and always kept. If the file belongs to another app, Android's own
 * "Allow ... to modify this file?" popup appears once, and the rename continues after "Allow".
 */
@Composable
internal fun RenameFileDialog(
    fileName: String,
    otherNamesInFolder: Collection<String>,
    rename: suspend (newFileName: String) -> RenameResult,
    onRenamed: (newFileName: String) -> Unit,
    onDismiss: () -> Unit,
    note: String? = null
) {
    val scope = rememberCoroutineScope()
    val extension = remember(fileName) { extensionOf(fileName) }
    var input by remember(fileName) {
        val base = baseNameOf(fileName)
        mutableStateOf(TextFieldValue(base, selection = TextRange(0, base.length)))
    }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var waitingFor by remember { mutableStateOf<String?>(null) }   // name waiting for "Allow"
    var allowedFor by remember { mutableStateOf<String?>(null) }   // "Allow" tapped: try again
    val check = remember(input.text, otherNamesInFolder) { checkNewFileName(input.text, fileName, otherNamesInFolder) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val name = waitingFor
        waitingFor = null
        if (result.resultCode == Activity.RESULT_OK && name != null) allowedFor = name
        else working = false // "Deny": just stay in the dialog
    }

    fun attempt(newName: String, afterPermission: Boolean) {
        scope.launch {
            working = true
            error = null
            when (val result = rename(newName)) {
                RenameResult.Renamed -> {
                    working = false
                    onRenamed(newName)
                }
                is RenameResult.NeedsPermission ->
                    if (afterPermission) {
                        working = false
                        error = "Android didn't allow renaming this file"
                    } else {
                        waitingFor = newName
                        askPermission.launch(IntentSenderRequest.Builder(result.request).build())
                    }
                is RenameResult.Failed -> {
                    working = false
                    error = result.message
                }
            }
        }
    }

    LaunchedEffect(allowedFor) {
        val name = allowedFor ?: return@LaunchedEffect
        allowedFor = null
        attempt(name, afterPermission = true)
    }

    val submit = { (check as? NameCheck.Ok)?.let { attempt(it.fileName, afterPermission = false) } }
    val shownError = error ?: (check as? NameCheck.Problem)?.reason?.takeIf { it.isNotEmpty() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text("Name") },
                    suffix = if (extension.isNotEmpty()) ({ Text(".$extension") }) else null,
                    singleLine = true,
                    enabled = !working,
                    isError = shownError != null,
                    supportingText = if (shownError != null) ({ Text(shownError) }) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                )
                if (note != null) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = check is NameCheck.Ok && !working) {
                Text(if (working) "Renaming…" else "Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("Cancel") } }
    )
}
