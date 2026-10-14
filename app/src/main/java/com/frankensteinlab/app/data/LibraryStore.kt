package com.frankensteinlab.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Small metadata store. API credentials must never be written here. */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    private val lock = Any()

    fun all(): List<LibraryItem> = synchronized(lock) {
        val raw = prefs.getString(KEY_ITEMS, "[]") ?: "[]"
        try {
            val json = JSONArray(raw)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val entry = json.getJSONObject(index)
                    add(
                        LibraryItem(
                            id = entry.getString("id"),
                            title = entry.getString("title"),
                            uri = Uri.parse(entry.getString("uri")),
                            kind = LibraryItem.Kind.valueOf(entry.getString("kind")),
                            progress = entry.optLong("progress", 0L).coerceAtLeast(0L),
                        ),
                    )
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("تعذرت قراءة فهرس المكتبة المحلي؛ لم تُحذف بياناته.", error)
        }
    }

    fun add(title: String, uri: Uri, kind: LibraryItem.Kind): LibraryItem = synchronized(lock) {
        val existing = all().firstOrNull { it.uri == uri && it.kind == kind }
        if (existing != null) return existing
        val item = LibraryItem(UUID.randomUUID().toString(), title, uri, kind)
        write(all() + item)
        item
    }

    fun updateProgress(id: String, progress: Long) = synchronized(lock) {
        write(all().map { if (it.id == id) it.copy(progress = progress.coerceAtLeast(0L)) else it })
    }

    /** Adds only missing URI/kind pairs. Existing titles and progress are never silently overwritten. */
    fun mergeBackup(imported: List<LibraryItem>): Int = synchronized(lock) {
        val current = all().toMutableList()
        val existing = HashSet<Pair<String, LibraryItem.Kind>>()
        current.forEach { item -> existing.add(item.uri.toString() to item.kind) }
        var added = 0
        imported.forEach { item ->
            if (existing.add(item.uri.toString() to item.kind)) {
                current += item.copy(id = UUID.randomUUID().toString())
                added++
            }
        }
        if (added > 0) write(current)
        added
    }

    fun remove(id: String): Uri? = synchronized(lock) {
        val items = all()
        val removed = items.firstOrNull { it.id == id } ?: return null
        write(items.filterNot { it.id == id })
        removed.uri
    }

    private fun write(items: List<LibraryItem>) {
        val json = JSONArray()
        items.forEach { item ->
            json.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("uri", item.uri.toString())
                    .put("kind", item.kind.name)
                    .put("progress", item.progress),
            )
        }
        prefs.edit().putString(KEY_ITEMS, json.toString()).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items_v1"
    }
}
