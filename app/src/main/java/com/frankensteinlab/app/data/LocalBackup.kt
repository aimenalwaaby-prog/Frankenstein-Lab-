package com.frankensteinlab.app.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Versioned, integrity-checked backup of app metadata only; it never stores credentials or media bytes. */
object LocalBackup {
    private const val FORMAT = "frankenstein-lab-backup"
    private const val VERSION = 1
    private const val MAX_BACKUP_BYTES = 8 * 1024 * 1024
    private const val MAX_ITEMS = 2_000
    private const val MAX_TITLE_LENGTH = 512
    private const val MAX_URI_LENGTH = 2_048

    data class RestoreData(
        val libraryItems: List<LibraryItem>?,
        val preferences: AppPreferences.Snapshot?,
        val createdAtMillis: Long,
    )

    suspend fun export(
        context: Context,
        destination: Uri,
        store: LibraryStore,
        preferences: AppPreferences,
        includeLibrary: Boolean,
        includePreferences: Boolean,
    ) = withContext(Dispatchers.IO) {
        require(includeLibrary || includePreferences) { "اختر المكتبة أو الإعدادات على الأقل." }
        val payload = JSONObject()
        if (includeLibrary) {
            val items = store.all()
            require(items.size <= MAX_ITEMS) { "عدد عناصر المكتبة يتجاوز حد النسخ الاحتياطي الآمن." }
            val rows = JSONArray()
            items.forEach { item ->
                require(item.title.isNotBlank() && item.title.length <= MAX_TITLE_LENGTH) {
                    "عنوان عنصر غير صالح للنسخ الاحتياطي."
                }
                val uriText = item.uri.toString()
                require(uriText.length <= MAX_URI_LENGTH && item.uri.scheme == "content") {
                    "يوجد عنصر بمسار URI غير مدعوم للنسخ الاحتياطي."
                }
                rows.put(
                    JSONObject()
                        .put("title", item.title)
                        .put("uri", uriText)
                        .put("kind", item.kind.name)
                        .put("progress", item.progress.coerceAtLeast(0L)),
                )
            }
            payload.put("library", rows)
        }
        if (includePreferences) {
            val snapshot = preferences.snapshot()
            payload.put(
                "preferences",
                JSONObject()
                    .put("theme", snapshot.theme.name)
                    .put("mangaMode", snapshot.mangaMode.name)
                    .put("novelFontSize", snapshot.novelFontSize),
            )
        }

        val payloadText = payload.toString()
        val envelope = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("payload", payloadText)
            .put("sha256", sha256(payloadText.toByteArray(StandardCharsets.UTF_8)))
        val bytes = envelope.toString().toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BACKUP_BYTES) { "حجم النسخة يتجاوز حد 8 ميغابايت." }
        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: throw IOException("تعذر فتح ملف الوجهة لكتابة النسخة الاحتياطية.")
        output.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
    }

    suspend fun read(context: Context, source: Uri): RestoreData = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("تعذر فتح ملف النسخة الاحتياطية.")
        val bytes = input.use { readLimited(it, MAX_BACKUP_BYTES) }
        val envelope = try {
            JSONObject(String(bytes, StandardCharsets.UTF_8))
        } catch (failure: Exception) {
            throw IOException("ملف النسخة ليس JSON صالحًا.", failure)
        }
        require(envelope.getString("format") == FORMAT) { "الملف ليس نسخة Frankenstein Lab." }
        require(envelope.getInt("version") == VERSION) { "إصدار النسخة غير مدعوم في هذا التطبيق." }
        val payloadText = envelope.getString("payload")
        val expectedHash = envelope.getString("sha256")
        require(expectedHash.matches(Regex("[0-9a-f]{64}"))) { "قيمة تحقق النسخة غير صالحة." }
        val actualHash = sha256(payloadText.toByteArray(StandardCharsets.UTF_8))
        require(MessageDigest.isEqual(expectedHash.toByteArray(StandardCharsets.US_ASCII), actualHash.toByteArray(StandardCharsets.US_ASCII))) {
            "فشل فحص سلامة النسخة؛ لم تُستعد أي بيانات."
        }
        val payload = try {
            JSONObject(payloadText)
        } catch (failure: Exception) {
            throw IOException("محتوى النسخة غير صالح.", failure)
        }
        val items = if (payload.has("library")) parseLibrary(payload.getJSONArray("library")) else null
        val preferences = if (payload.has("preferences")) parsePreferences(payload.getJSONObject("preferences")) else null
        require(items != null || preferences != null) { "لا تتضمن النسخة أقسامًا قابلة للاستعادة." }
        RestoreData(
            libraryItems = items,
            preferences = preferences,
            createdAtMillis = envelope.optLong("createdAtMillis", 0L).coerceAtLeast(0L),
        )
    }

    private fun parseLibrary(rows: JSONArray): List<LibraryItem> {
        require(rows.length() <= MAX_ITEMS) { "عدد العناصر في النسخة يتجاوز الحد الآمن." }
        return buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val title = row.getString("title")
                require(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) { "عنوان عنصر في النسخة غير صالح." }
                val uriText = row.getString("uri")
                require(uriText.length <= MAX_URI_LENGTH) { "URI عنصر في النسخة أطول من الحد الآمن." }
                val uri = Uri.parse(uriText)
                require(uri.scheme == "content") { "تحتوي النسخة على URI غير مدعوم؛ لم تُستعد." }
                val kind = try {
                    LibraryItem.Kind.valueOf(row.getString("kind"))
                } catch (failure: Exception) {
                    throw IOException("نوع عنصر في النسخة غير معروف.", failure)
                }
                val progress = row.getLong("progress")
                require(progress >= 0L) { "قيمة تقدم عنصر في النسخة غير صالحة." }
                add(LibraryItem("restore-${UUID.randomUUID()}", title, uri, kind, progress))
            }
        }
    }

    private fun parsePreferences(value: JSONObject): AppPreferences.Snapshot {
        val theme = try {
            AppPreferences.Theme.valueOf(value.getString("theme"))
        } catch (failure: Exception) {
            throw IOException("المظهر في النسخة غير معروف.", failure)
        }
        val mangaMode = try {
            AppPreferences.MangaMode.valueOf(value.getString("mangaMode"))
        } catch (failure: Exception) {
            throw IOException("وضع قارئ المانغا في النسخة غير معروف.", failure)
        }
        val fontSize = value.getInt("novelFontSize")
        require(fontSize in 14..32) { "حجم النص في النسخة خارج النطاق المسموح." }
        return AppPreferences.Snapshot(theme, mangaMode, fontSize)
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "حجم ملف النسخة يتجاوز حد 8 ميغابايت." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
}
