package com.harish.mediaplayer.domain.rename

/**
 * Rules for the name typed in the "Rename" box.
 *
 * The user only types the name part ("Trip to Ooty"); the extension (".mp4") is kept for them,
 * so a file can never lose its type by accident.
 */
sealed interface NameCheck {
    /** Good to go: the full new file name, extension included, e.g. "Trip to Ooty.mp4". */
    data class Ok(val fileName: String) : NameCheck

    /** Can't use it. [reason] is shown under the box; empty = just keep the button disabled. */
    data class Problem(val reason: String) : NameCheck
}

// Characters Android (and SD cards / PCs) don't allow in file names
private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
private const val MAX_NAME_BYTES = 255

/** "Trip.mp4" -> "Trip", "README" -> "README" */
fun baseNameOf(fileName: String): String =
    fileName.substringBeforeLast('.', fileName).ifEmpty { fileName }

/** "Trip.mp4" -> "mp4", "README" -> "" */
fun extensionOf(fileName: String): String =
    if (baseNameOf(fileName) == fileName) "" else fileName.substringAfterLast('.')

/**
 * Checks [typed] as the new name for [currentFileName].
 * [otherNamesInFolder] = the other files in the same folder, so we don't clash with one of them.
 */
fun checkNewFileName(typed: String, currentFileName: String, otherNamesInFolder: Collection<String>): NameCheck {
    val base = typed.trim()
    if (base.isEmpty()) return NameCheck.Problem("")
    if (FORBIDDEN.containsMatchIn(base)) return NameCheck.Problem("A name can't contain \\ / : * ? \" < > |")
    if (base.startsWith('.')) return NameCheck.Problem("A name can't start with a dot")
    if (base.endsWith('.')) return NameCheck.Problem("A name can't end with a dot")

    val extension = extensionOf(currentFileName)
    // Typed the extension as well ("Trip.mp4")? Don't add it twice.
    val full = if (extension.isEmpty() || base.endsWith(".$extension", ignoreCase = true)) base else "$base.$extension"

    if (full.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES) return NameCheck.Problem("That name is too long")
    if (full == currentFileName) return NameCheck.Problem("") // nothing changed yet
    // Phones treat "Trip.mp4" and "trip.mp4" as the same file, so compare ignoring case
    if (otherNamesInFolder.any { it.equals(full, ignoreCase = true) }) {
        return NameCheck.Problem("Another file in this folder already has this name")
    }
    return NameCheck.Ok(full)
}
