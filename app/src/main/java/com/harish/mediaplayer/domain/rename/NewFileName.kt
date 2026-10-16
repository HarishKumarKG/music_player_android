package com.harish.mediaplayer.domain.rename

/**
 * Rules for the name typed in the "Rename" box (files and folders).
 *
 * For a file the user only types the name part ("Trip to Ooty"); the extension (".mp4") is kept
 * for them, so a file can never lose its type by accident.
 */
sealed interface NameCheck {
    /** Good to go: the full new name, e.g. "Trip to Ooty.mp4" (file) or "Ooty 2024" (folder). */
    data class Ok(val fileName: String) : NameCheck

    /** Can't use it. [reason] is shown under the box; empty = just keep the button disabled. */
    data class Problem(val reason: String) : NameCheck
}

// Characters Android (and SD cards / PCs) don't allow in names
private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
private const val MAX_NAME_BYTES = 255

/** "Trip.mp4" -> "Trip", "README" -> "README" */
fun baseNameOf(fileName: String): String =
    fileName.substringBeforeLast('.', fileName).ifEmpty { fileName }

/** "Trip.mp4" -> "mp4", "README" -> "" */
fun extensionOf(fileName: String): String =
    if (baseNameOf(fileName) == fileName) "" else fileName.substringAfterLast('.')

/**
 * Checks [typed] as the new name for the file [currentFileName].
 * [otherNamesInFolder] = the other files in the same folder, so we don't clash with one of them.
 */
fun checkNewFileName(typed: String, currentFileName: String, otherNamesInFolder: Collection<String>): NameCheck {
    val base = typed.trim()
    basicProblem(base)?.let { return it }
    val extension = extensionOf(currentFileName)
    // Typed the extension as well ("Trip.mp4")? Don't add it twice.
    val full = if (extension.isEmpty() || base.endsWith(".$extension", ignoreCase = true)) base else "$base.$extension"
    return finalCheck(full, currentFileName, otherNamesInFolder)
}

/** Same rules for a folder: no extension to keep, the whole name is what you type. */
fun checkNewFolderName(typed: String, currentName: String, otherNamesInParent: Collection<String>): NameCheck {
    val name = typed.trim()
    basicProblem(name)?.let { return it }
    return finalCheck(name, currentName, otherNamesInParent)
}

/**
 * Names directly inside [folder], worked out from file paths.
 * Example: folder "/s/Movies" with "/s/Movies/Ooty/a.mp4" and "/s/Movies/b.mp4" -> {"Ooty", "b.mp4"}
 */
fun namesInside(folder: String, filePaths: Iterable<String>): Set<String> {
    val prefix = folder.trimEnd('/') + "/"
    return filePaths.filter { it.startsWith(prefix) }.mapTo(HashSet()) { it.removePrefix(prefix).substringBefore('/') }
}

private fun basicProblem(name: String): NameCheck.Problem? = when {
    name.isEmpty() -> NameCheck.Problem("")
    FORBIDDEN.containsMatchIn(name) -> NameCheck.Problem("A name can't contain \\ / : * ? \" < > |")
    name.startsWith('.') -> NameCheck.Problem("A name can't start with a dot")
    name.endsWith('.') -> NameCheck.Problem("A name can't end with a dot")
    else -> null
}

private fun finalCheck(full: String, current: String, others: Collection<String>): NameCheck = when {
    full.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES -> NameCheck.Problem("That name is too long")
    full == current -> NameCheck.Problem("") // nothing changed yet
    // Phones treat "Trip" and "trip" as the same name, so compare ignoring case
    others.any { it.equals(full, ignoreCase = true) && !it.equals(current, ignoreCase = true) } ->
        NameCheck.Problem("Something in this folder already has this name")
    else -> NameCheck.Ok(full)
}
