package io.grimoire.app.data.epub

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EpubParserTest {
    @Test
    fun `parses metadata navigation chapter text and embedded cover`() {
        val epub = createEpub(
            mapOf(
                "META-INF/container.xml" to """<?xml version="1.0"?><container><rootfiles><rootfile full-path="OEBPS/package.opf"/></rootfiles></container>""",
                "OEBPS/package.opf" to """<?xml version="1.0"?><package xmlns:dc="http://purl.org/dc/elements/1.1/"><metadata><dc:title>كتاب تجريبي</dc:title><dc:creator>مؤلف</dc:creator><dc:language>ar</dc:language><dc:subject>خيال</dc:subject></metadata><manifest><item id="nav" href="nav.xhtml" properties="nav"/><item id="chapter" href="chapter%201.xhtml" media-type="application/xhtml+xml"/><item id="cover" href="cover.png" media-type="image/png" properties="cover-image"/></manifest><spine><itemref idref="chapter"/></spine></package>""",
                "OEBPS/nav.xhtml" to """<html><body><nav><a href="chapter%201.xhtml#start">الفصل الأول</a></nav></body></html>""",
                "OEBPS/chapter 1.xhtml" to """<html><head><title>عنوان داخلي</title></head><body><h1>الفصل الأول</h1><p>نص عربي للاختبار.</p><script>غير مقروء</script></body></html>""",
                "OEBPS/cover.png" to "cover-bytes",
            ),
        )

        try {
            val parsed = EpubParser.parse(epub.toFile())
            assertEquals("كتاب تجريبي", parsed.title)
            assertEquals("مؤلف", parsed.author)
            assertEquals("ar", parsed.language)
            assertEquals(listOf("خيال"), parsed.genres)
            assertNotNull(parsed.cover)
            assertEquals("png", parsed.cover?.extension)
            assertEquals(1, parsed.chapters.size)
            assertEquals("الفصل الأول", parsed.chapters.single().title)
            assertEquals(
                listOf("الفصل الأول", "نص عربي للاختبار."),
                parsed.chapters.single().pages.filterIsInstance<EpubPage.Text>().map { it.text },
            )
        } finally {
            Files.deleteIfExists(epub)
        }
    }

    @Test
    fun `rejects a zip without epub container metadata`() {
        val invalid = createEpub(mapOf("not-epub.txt" to "no container"))
        try {
            val error = assertThrows(IllegalStateException::class.java) {
                EpubParser.parse(invalid.toFile())
            }
            assertEquals("Not a valid EPUB: missing META-INF/container.xml", error.message)
        } finally {
            Files.deleteIfExists(invalid)
        }
    }

    private fun createEpub(entries: Map<String, String>) =
        Files.createTempFile("frankenstein-test-", ".epub").also { path ->
            ZipOutputStream(Files.newOutputStream(path)).use { zip ->
                entries.forEach { (name, text) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }
}
