package com.frankensteinlab.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.frankensteinlab.app.data.AppPreferences
import com.frankensteinlab.app.data.LibraryItem
import com.frankensteinlab.app.data.LibraryStore
import com.frankensteinlab.app.data.LocalMediaFiles
import io.grimoire.app.data.epub.EpubPage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.skepsun.kototoro.reader.core.PageGeometryHint
import org.skepsun.kototoro.reader.core.PageId
import org.skepsun.kototoro.reader.core.PagedPageSpec
import org.skepsun.kototoro.reader.core.PagedReaderScene
import org.skepsun.kototoro.reader.core.PagedSpreadConfig
import org.skepsun.kototoro.reader.core.SceneReadingDirection
import org.skepsun.kototoro.reader.core.SpreadBehavior

@Composable
fun LocalReaderScreen(
    item: LibraryItem,
    store: LibraryStore,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    when (item.kind) {
        LibraryItem.Kind.NOVEL -> NovelReaderScreen(item, store, preferences, onBack, onMessage)
        LibraryItem.Kind.COMIC -> ComicReaderScreen(item, store, preferences, onBack, onMessage)
        LibraryItem.Kind.VIDEO -> LocalVideoScreen(item, store, onBack)
    }
}

@Composable
private fun NovelReaderScreen(
    item: LibraryItem,
    store: LibraryStore,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var book by remember(item.uri) { mutableStateOf<io.grimoire.app.data.epub.ParsedEpub?>(null) }
    var error by remember(item.uri) { mutableStateOf<String?>(null) }
    var chapterIndex by remember(item.id) { mutableIntStateOf(item.progress.toInt().coerceAtLeast(0)) }
    var fontSize by remember { mutableIntStateOf(preferences.novelFontSize()) }

    LaunchedEffect(item.uri) {
        error = null
        try {
            val file = LocalMediaFiles.copyToCache(context, item.uri)
            book = LocalMediaFiles.parseEpub(file)
            val maxIndex = (book?.chapters?.lastIndex ?: 0).coerceAtLeast(0)
            chapterIndex = chapterIndex.coerceIn(0, maxIndex)
        } catch (failure: Exception) {
            error = failure.localizedMessage ?: "تعذرت قراءة ملف EPUB."
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("عودة") }
            Text(item.title, maxLines = 1)
        }
        error?.let { Text("تعذر فتح الكتاب: $it") }
        val parsed = book
        if (parsed == null && error == null) {
            CircularProgressIndicator()
            Text("جارٍ فحص الكتاب المحلي…")
        } else if (parsed != null) {
            val chapter = parsed.chapters.getOrNull(chapterIndex)
            LaunchedEffect(item.id, chapterIndex) {
                withContext(Dispatchers.IO) { store.updateProgress(item.id, chapterIndex.toLong()) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = chapterIndex > 0,
                    onClick = { chapterIndex-- },
                ) { Text("الفصل السابق") }
                Text("${chapterIndex + 1} / ${parsed.chapters.size}")
                TextButton(
                    enabled = chapterIndex < parsed.chapters.lastIndex,
                    onClick = { chapterIndex++ },
                ) { Text("الفصل التالي") }
            }
            Text(chapter?.title ?: parsed.title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    fontSize = (fontSize - 1).coerceAtLeast(14)
                    preferences.setNovelFontSize(fontSize)
                }) { Text("أ−") }
                Text("حجم النص: $fontSize")
                TextButton(onClick = {
                    fontSize = (fontSize + 1).coerceAtMost(32)
                    preferences.setNovelFontSize(fontSize)
                }) { Text("أ+") }
            }
            if (chapter != null) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(chapter.pages) { _, page ->
                        when (page) {
                            is EpubPage.Text -> Text(page.text, fontSize = fontSize.sp, lineHeight = (fontSize * 1.65f).sp)
                            is EpubPage.Image -> EpubImage(page.bytes)
                        }
                    }
                }
            } else {
                Text("لا يحتوي هذا الفصل على صفحات قابلة للعرض.")
            }
            if (parsed.author != null) Text("المؤلف: ${parsed.author}")
        }
    }
}

@Composable
private fun EpubImage(bytes: ByteArray) {
    val bitmap by produceState<Bitmap?>(null, bytes) {
        value = withContext(Dispatchers.Default) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                },
            )
        }
    }
    bitmap?.let {
        Image(
            it.asImageBitmap(),
            contentDescription = "صورة من الكتاب",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ComicReaderScreen(
    item: LibraryItem,
    store: LibraryStore,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var archive by remember(item.uri) { mutableStateOf<File?>(null) }
    var pages by remember(item.uri) { mutableStateOf<List<String>>(emptyList()) }
    var error by remember(item.uri) { mutableStateOf<String?>(null) }
    var currentPage by remember(item.id) { mutableIntStateOf(item.progress.toInt().coerceAtLeast(0)) }
    val mode = preferences.mangaMode()

    LaunchedEffect(item.uri) {
        try {
            val localArchive = LocalMediaFiles.copyToCache(context, item.uri)
            val entries = LocalMediaFiles.comicPages(localArchive)
            archive = localArchive
            pages = entries
            currentPage = currentPage.coerceIn(0, entries.lastIndex)
        } catch (failure: Exception) {
            error = failure.localizedMessage ?: "تعذر فتح أرشيف المانغا."
        }
    }

    val scene = remember(pages, mode) {
        if (pages.isEmpty()) null else PagedReaderScene(
            viewportWidth = 1080,
            viewportHeight = 1920,
            config = PagedSpreadConfig(
                isDoublePage = false,
                readingDirection = if (mode == AppPreferences.MangaMode.PAGED_HORIZONTAL) {
                    SceneReadingDirection.RIGHT_TO_LEFT
                } else SceneReadingDirection.TOP_TO_BOTTOM,
            ),
            initialSpecs = pages.mapIndexed { index, _ ->
                PagedPageSpec(
                    pageId = PageId(index.toLong()),
                    geometryHint = PageGeometryHint.Estimated(0.7f),
                    chapterId = 0L,
                    chapterPageIndex = index,
                    spreadBehavior = SpreadBehavior.SOLO,
                )
            },
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("عودة") }
            Text(item.title, maxLines = 1)
        }
        error?.let { Text("تعذر فتح المانغا: $it") }
        if (pages.isEmpty() && error == null) {
            CircularProgressIndicator()
            Text("جارٍ فحص أرشيف CBZ المحلي…")
        } else if (pages.isNotEmpty()) {
            if (mode == AppPreferences.MangaMode.CONTINUOUS_VERTICAL) {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage.coerceIn(0, pages.lastIndex))
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    val active = listState.firstVisibleItemIndex.coerceIn(0, pages.lastIndex)
                    currentPage = active
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(pages, key = { index, path -> "$index:$path" }) { _, path ->
                        ComicPage(archive, path)
                    }
                }
            } else {
                val id = PageId(currentPage.toLong())
                val canonicalIndex = scene?.indexOf(id)?.coerceAtLeast(0) ?: currentPage
                ComicPage(archive, pages[canonicalIndex], Modifier.weight(1f).fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = currentPage > 0, onClick = { currentPage-- }) { Text("السابق") }
                    Text("${canonicalIndex + 1} / ${pages.size}")
                    TextButton(enabled = currentPage < pages.lastIndex, onClick = { currentPage++ }) { Text("التالي") }
                }
            }
            LaunchedEffect(item.id, currentPage, pages.size) {
                if (pages.isNotEmpty()) {
                    kotlinx.coroutines.delay(350)
                    withContext(Dispatchers.IO) { store.updateProgress(item.id, currentPage.toLong()) }
                }
            }
            LinearProgressIndicator(
                progress = ((currentPage + 1f) / pages.size).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ComicPage(archive: File?, entryName: String, modifier: Modifier = Modifier) {
    val result by produceState<Pair<Bitmap?, Boolean>>(null to false, archive, entryName) {
        val decoded = runCatching {
            archive?.let { LocalMediaFiles.decodeComicPage(it, entryName) }
        }.getOrNull()
        value = decoded to true
    }
    val bitmap = result.first
    if (!result.second) {
        Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
    } else if (bitmap == null) {
        Text("تعذر فك صورة الصفحة.", modifier.padding(16.dp))
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "صفحة مانغا",
            modifier = modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun LocalVideoScreen(item: LibraryItem, store: LibraryStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playbackError by remember(item.id) { mutableStateOf<String?>(null) }
    var resumeAfterForeground by remember(item.id) { mutableStateOf(false) }
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.uri))
            prepare()
            if (item.progress > 0L) seekTo(item.progress)
            playWhenReady = true
        }
    }
    androidx.compose.runtime.DisposableEffect(player, lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeAfterForeground = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_START -> {
                    if (resumeAfterForeground) {
                        resumeAfterForeground = false
                        player.play()
                    }
                }
                else -> Unit
            }
        }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "تعذر على Media3 قراءة الملف. رمز التشخيص: ${error.errorCodeName}"
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        player.addListener(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(listener)
            store.updateProgress(item.id, player.currentPosition.coerceAtLeast(0L))
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            kotlinx.coroutines.delay(10_000)
            withContext(Dispatchers.IO) {
                store.updateProgress(item.id, player.currentPosition.coerceAtLeast(0L))
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("عودة وإيقاف المشغل") }
            Text("فيديو محلي")
        }
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Text(item.title)
        playbackError?.let { Text("تعذر تشغيل هذا الفيديو: $it") }
        Text("يُشغّل هذا العنصر محليًا عبر Media3؛ لم تُحمّل مصادر بث أو خيارات ترجمة غير متاحة.")
    }
}
