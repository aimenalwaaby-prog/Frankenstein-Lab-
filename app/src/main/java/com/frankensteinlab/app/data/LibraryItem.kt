package com.frankensteinlab.app.data

import android.net.Uri

/** A user-selected local item; the app stores its SAF URI, never a copy of the source itself. */
data class LibraryItem(
    val id: String,
    val title: String,
    val uri: Uri,
    val kind: Kind,
    val progress: Long = 0L,
) {
    enum class Kind(val label: String) {
        NOVEL("رواية EPUB"),
        COMIC("مانغا CBZ"),
        VIDEO("فيديو محلي"),
    }
}
