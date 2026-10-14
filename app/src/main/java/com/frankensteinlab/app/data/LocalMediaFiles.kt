package com.frankensteinlab.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import io.grimoire.app.data.epub.EpubParser
import io.grimoire.app.data.epub.ParsedEpub
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMediaFiles {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")
    private val videoExtensions = setOf("mp4", "m4v", "mkv", "webm", "mov", "3gp", "ts")
    private const val MAX_SOURCE_BYTES = 512L * 1024 * 1024
    private const val MAX_EPUB_EXPANDED_BYTES = 64L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 20_000

    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index) ?: "ملف محلي"
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "ملف محلي"
    }

    fun typeFor(context: Context, uri: Uri, name: String): LibraryItem.Kind? {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        return when {
            extension == "epub" || mime == "application/epub+zip" -> LibraryItem.Kind.NOVEL
            extension == "cbz" -> LibraryItem.Kind.COMIC
            extension in videoExtensions || mime.startsWith("video/") -> LibraryItem.Kind.VIDEO
            else -> null
        }
    }

    suspend fun copyToCache(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val metadata = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE, OpenableColumns.LAST_MODIFIED),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val modifiedIndex = cursor.getColumnIndex(OpenableColumns.LAST_MODIFIED)
                    "${if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L}:" +
                        "${if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else -1L}"
                } else "unknown"
            } ?: "unknown"
        }.getOrDefault("unknown")
        val identity = "$uri:$metadata"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
        val directory = File(context.cacheDir, "opened-local-media").apply { mkdirs() }
        val finalFile = File(directory, "$digest.bin")
        if (finalFile.isFile && finalFile.length() in 1..MAX_SOURCE_BYTES) return@withContext finalFile
        val temporary = File(directory, "$digest.part")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: error("تعذر فتح الملف المحدد.")
            input.use { stream ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        check(total <= MAX_SOURCE_BYTES) { "حجم الملف يتجاوز الحد المحلي الآمن (512 ميغابايت)." }
                        output.write(buffer, 0, read)
                    }
                }
            }
            check(temporary.length() > 0L) { "الملف المحدد فارغ." }
            check(temporary.renameTo(finalFile)) { "تعذر تجهيز الملف للقراءة." }
            finalFile
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    suspend fun comicPages(file: File): List<String> = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
            check(entries.size <= MAX_ZIP_ENTRIES) { "الأرشيف يحتوي عناصر أكثر من الحد الآمن." }
            val pages = entries
                .filter { it.size in 1..(256L * 1024 * 1024) }
                .map { it.name }
                .filter { path -> path.substringAfterLast('.', "").lowercase() in imageExtensions }
                .sortedWith(NaturalPathComparator)
            check(pages.isNotEmpty()) { "ملف CBZ لا يحتوي صفحات صور مدعومة." }
            pages
        }
    }

    suspend fun decodeComicPage(file: File, entryName: String, maxDimension: Int = 2400): Bitmap? =
        withContext(Dispatchers.IO) {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName) ?: return@withContext null
                if (entry.size !in 1..(256L * 1024 * 1024)) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sample = 1
                while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
                    sample *= 2
                }
                val extension = entryName.substringAfterLast('.', "").lowercase()
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = if (extension in setOf("png", "webp", "bmp")) {
                        Bitmap.Config.ARGB_8888
                    } else Bitmap.Config.RGB_565
                }
                zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, options) }
            }
        }

    suspend fun parseEpub(file: File): ParsedEpub = withContext(Dispatchers.IO) {
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().toList()
            check(entries.size <= MAX_ZIP_ENTRIES) { "كتاب EPUB يحتوي عناصر أكثر من الحد الآمن." }
            val expandedBytes = entries.fold(0L) { total, entry ->
                check(entry.size >= 0L) { "تعذر التحقق من حجم عنصر داخل كتاب EPUB." }
                val next = total + entry.size
                check(next >= total) { "تجاوز مجموع أحجام عناصر EPUB النطاق الآمن." }
                next
            }
            check(expandedBytes <= MAX_EPUB_EXPANDED_BYTES) {
                "حجم المحتوى بعد فك الضغط كبير جدًا لهذا المحلل المحلي (الحد 64 ميغابايت)."
            }
        }
        EpubParser.parse(file)
    }

    private object NaturalPathComparator : Comparator<String> {
        override fun compare(first: String, second: String): Int {
            val left = first.lowercase().split(Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"))
            val right = second.lowercase().split(Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"))
            for (index in 0 until minOf(left.size, right.size)) {
                val a = left[index]
                val b = right[index]
                val aNumber = a.toLongOrNull()
                val bNumber = b.toLongOrNull()
                val result = if (aNumber != null && bNumber != null) aNumber.compareTo(bNumber) else a.compareTo(b)
                if (result != 0) return result
            }
            return left.size.compareTo(right.size).takeIf { it != 0 } ?: first.compareTo(second, ignoreCase = true)
        }
    }
}
