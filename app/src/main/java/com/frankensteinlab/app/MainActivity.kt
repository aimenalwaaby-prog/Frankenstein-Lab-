package com.frankensteinlab.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.frankensteinlab.app.data.AppPreferences
import com.frankensteinlab.app.data.LibraryItem
import com.frankensteinlab.app.data.LibraryStore
import com.frankensteinlab.app.data.LocalBackup
import com.frankensteinlab.app.data.LocalMediaFiles
import com.frankensteinlab.app.files.FileManagerScreen
import com.frankensteinlab.app.ui.LocalReaderScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FrankensteinApp() }
    }
}

private enum class MainTab(val title: String, val glyph: String) {
    LIBRARY("المكتبة", "▦"),
    FILES("الملفات", "▤"),
    SETTINGS("الإعدادات", "⚙"),
}

private val FrankensteinLight = lightColorScheme(
    primary = Color(0xFF5D458A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFF9A4F3E),
    background = Color(0xFFF8F5FA),
    surface = Color(0xFFF8F5FA),
)

private val FrankensteinDark = darkColorScheme(
    primary = Color(0xFFD2BDFF),
    onPrimary = Color(0xFF35215F),
    primaryContainer = Color(0xFF4B3577),
    secondary = Color(0xFFFFB5A0),
    background = Color(0xFF17131E),
    surface = Color(0xFF211B2A),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrankensteinApp() {
    val context = LocalContext.current
    val store = remember { LibraryStore(context) }
    val preferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var theme by remember { mutableStateOf(preferences.theme()) }
    var items by remember { mutableStateOf(emptyList<LibraryItem>()) }
    var selectedItem by remember { mutableStateOf<LibraryItem?>(null) }
    var tab by remember { mutableIntStateOf(MainTab.LIBRARY.ordinal) }
    var pendingRemoval by remember { mutableStateOf<LibraryItem?>(null) }
    var pendingRestore by remember { mutableStateOf<LocalBackup.RestoreData?>(null) }
    var pendingBackupSelection by remember { mutableStateOf(true to true) }
    var settingsRevision by remember { mutableIntStateOf(0) }
    var storeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store) {
        try {
            items = withContext(Dispatchers.IO) { store.all() }
        } catch (failure: Exception) {
            storeError = failure.localizedMessage ?: "تعذرت قراءة المكتبة المحلية."
        }
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun closeReader() {
        selectedItem = null
        scope.launch {
            try {
                items = withContext(Dispatchers.IO) { store.all() }
            } catch (failure: Exception) {
                storeError = failure.localizedMessage ?: "تعذرت إعادة تحميل المكتبة."
            }
        }
    }

    suspend fun addLocalUri(uri: Uri, displayName: String, fromTree: Boolean) {
        try {
            val kind = withContext(Dispatchers.IO) { LocalMediaFiles.typeFor(context, uri, displayName) }
            if (kind == null) {
                message("نوع الملف غير مدعوم. يدعم التطبيق حاليًا EPUB وCBZ والفيديو المحلي.")
                return
            }
            if (!fromTree) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            if (kind == LibraryItem.Kind.NOVEL || kind == LibraryItem.Kind.COMIC) {
                val file = LocalMediaFiles.copyToCache(context, uri)
                when (kind) {
                    LibraryItem.Kind.NOVEL -> LocalMediaFiles.parseEpub(file)
                    LibraryItem.Kind.COMIC -> LocalMediaFiles.comicPages(file)
                    LibraryItem.Kind.VIDEO -> Unit
                }
            }
            val item = withContext(Dispatchers.IO) { store.add(displayName, uri, kind) }
            items = withContext(Dispatchers.IO) { store.all() }
            storeError = null
            selectedItem = item
            message("أُضيف الملف المحلي إلى المكتبة.")
        } catch (failure: Exception) {
            message("تعذرت إضافة الملف: ${failure.localizedMessage ?: "تحقق من نوعه وإذن قراءته."}")
        }
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = try {
                    withContext(Dispatchers.IO) { LocalMediaFiles.displayName(context, uri) }
                } catch (failure: Exception) {
                    message("تعذر قراءة اسم الملف المحدد: ${failure.localizedMessage ?: "خطأ من مزود الملفات."}")
                    return@launch
                }
                addLocalUri(uri, name, fromTree = false)
            }
        }
    }

    val backupExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val selection = pendingBackupSelection
            scope.launch {
                try {
                    LocalBackup.export(
                        context = context,
                        destination = uri,
                        store = store,
                        preferences = preferences,
                        includeLibrary = selection.first,
                        includePreferences = selection.second,
                    )
                    message("كُتبت نسخة JSON وفُحص حجمها؛ احفظها في مكان آمن.")
                } catch (failure: Exception) {
                    message("تعذر إنشاء النسخة: ${failure.localizedMessage ?: "تحقق من مساحة الوجهة وصلاحياتها."}")
                }
            }
        }
    }

    val backupImportPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    pendingRestore = LocalBackup.read(context, uri)
                } catch (failure: Exception) {
                    message("رُفضت النسخة ولم تُستعد بيانات: ${failure.localizedMessage ?: "تعذر فحص الملف."}")
                }
            }
        }
    }

    val dark = when (theme) {
        AppPreferences.Theme.SYSTEM -> isSystemInDarkTheme()
        AppPreferences.Theme.LIGHT -> false
        AppPreferences.Theme.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = if (dark) FrankensteinDark else FrankensteinLight) {
            val isReader = selectedItem != null
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                if (isReader) selectedItem?.title.orEmpty() else "Frankenstein Lab",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            if (isReader) TextButton(onClick = ::closeReader) { Text("عودة") }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!isReader) {
                        NavigationBar {
                            MainTab.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item.ordinal,
                                    onClick = { tab = item.ordinal },
                                    icon = { Text(item.glyph) },
                                    label = { Text(item.title) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (storeError != null) {
                        Text("تحذير بيانات المكتبة: ${storeError}", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                    }
                    val current = selectedItem
                    if (current != null) {
                        LocalReaderScreen(
                            item = current,
                            store = store,
                            preferences = preferences,
                            onBack = ::closeReader,
                            onMessage = ::message,
                        )
                    } else {
                        when (MainTab.entries.getOrElse(tab) { MainTab.LIBRARY }) {
                            MainTab.LIBRARY -> LibraryScreen(
                                items = items,
                                onAdd = { documentPicker.launch(arrayOf("*/*")) },
                                onOpen = { selectedItem = it },
                                onRemove = { pendingRemoval = it },
                            )
                            MainTab.FILES -> FileManagerScreen(
                                onOpenLocalFile = { uri, name ->
                                    scope.launch { addLocalUri(uri, name, fromTree = true) }
                                },
                                onMessage = ::message,
                            )
                            MainTab.SETTINGS -> SettingsScreen(
                                preferences = preferences,
                                currentTheme = theme,
                                settingsRevision = settingsRevision,
                                onThemeChange = { theme = it; preferences.setTheme(it) },
                                onMessage = ::message,
                                onExportBackup = { includeLibrary, includePreferences ->
                                    pendingBackupSelection = includeLibrary to includePreferences
                                    backupExportPicker.launch("FrankensteinLab-backup.json")
                                },
                                onImportBackup = { backupImportPicker.launch(arrayOf("*/*")) },
                            )
                        }
                    }
                }
            }

            pendingRemoval?.let { item ->
                AlertDialog(
                    onDismissRequest = { pendingRemoval = null },
                    title = { Text("إزالة من المكتبة؟") },
                    text = { Text("ستُزال بيانات الفهرسة والتقدم فقط. لن يحذف التطبيق الملف الأصلي.") },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingRemoval = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { store.remove(item.id) }
                                    runCatching {
                                        context.contentResolver.releasePersistableUriPermission(
                                            item.uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                        )
                                    }
                                    items = withContext(Dispatchers.IO) { store.all() }
                                    message("أُزيل العنصر من فهرس المكتبة؛ بقي الملف الأصلي كما هو.")
                                } catch (failure: Exception) {
                                    storeError = failure.localizedMessage ?: "تعذرت إزالة العنصر من المكتبة."
                                    message("تعذرت إزالة العنصر؛ لم يُحذف الملف الأصلي.")
                                }
                            }
                        }) { Text("إزالة") }
                    },
                    dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("إلغاء") } },
                    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                )
            }

            pendingRestore?.let { backup ->
                AlertDialog(
                    onDismissRequest = { pendingRestore = null },
                    title = { Text("معاينة واستعادة النسخة") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("عناصر المكتبة في الملف: ${backup.libraryItems?.size ?: 0}. ستُضاف العناصر غير الموجودة فقط؛ لن تُحذف العناصر الحالية ولن يُستبدل تقدمها.")
                            Text(if (backup.preferences != null) "تتضمن النسخة إعدادات التطبيق؛ تأكيد الاستعادة سيستبدل إعدادات المظهر والمانغا وحجم النص الحالية." else "لا تتضمن النسخة إعدادات التطبيق.")
                            Text("النسخة تحفظ بيانات الفهرس والتقدم والإعدادات فقط، ولا تحتوي ملفات EPUB/CBZ/الفيديو أو أذونات الوصول إليها. قد تحتاج لإعادة اختيار الملفات بعد نقل النسخة إلى جهاز آخر. لا تتضمن مفاتيح أو بيانات اعتماد.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingRestore = null
                            scope.launch {
                                try {
                                    val added = backup.libraryItems?.let { library ->
                                        withContext(Dispatchers.IO) { store.mergeBackup(library) }
                                    } ?: 0
                                    backup.preferences?.let { snapshot ->
                                        preferences.restore(snapshot)
                                        theme = snapshot.theme
                                        settingsRevision++
                                    }
                                    items = withContext(Dispatchers.IO) { store.all() }
                                    storeError = null
                                    message("اكتملت الاستعادة: أُضيف $added عنصر جديد؛ لم تُستبدل العناصر المتكررة.")
                                } catch (failure: Exception) {
                                    storeError = failure.localizedMessage ?: "تعذرت استعادة البيانات."
                                    message("تعذرت الاستعادة؛ لم تُحذف الملفات الأصلية.")
                                }
                            }
                        }) { Text("دمج/استعادة") }
                    },
                    dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("إلغاء") } },
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    items: List<LibraryItem>,
    onAdd: () -> Unit,
    onOpen: (LibraryItem) -> Unit,
    onRemove: (LibraryItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(items, query) {
        items.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("محتوى محلي تختاره أنت")
            Button(onClick = onAdd) { Text("إضافة ملف") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("بحث في المكتبة المحلية") },
            singleLine = true,
        )
        if (visible.isEmpty()) {
            if (items.isEmpty()) {
                Image(
                    painter = painterResource(com.frankensteinlab.app.R.drawable.ic_frankenstein),
                    contentDescription = "شخصية Frankenstein Lab",
                    modifier = Modifier.size(96.dp),
                )
                Text("المكتبة فارغة. أضف ملف EPUB أو CBZ أو فيديو محليًا من جهازك. لا توجد مصادر بث أو إضافات مفعّلة بعد.")
            } else Text("لا توجد نتائج مطابقة.", Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(item) }) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.kind.label, style = MaterialTheme.typography.bodyMedium)
                            if (item.progress > 0L) {
                                val progress = when (item.kind) {
                                    LibraryItem.Kind.VIDEO -> "موضع الفيديو: ${item.progress / 1000}s"
                                    LibraryItem.Kind.NOVEL -> "الفصل المحفوظ: ${item.progress + 1}"
                                    LibraryItem.Kind.COMIC -> "الصفحة المحفوظة: ${item.progress + 1}"
                                }
                                Text(progress, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onRemove(item) }) { Text("إزالة من الفهرس") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: AppPreferences,
    currentTheme: AppPreferences.Theme,
    settingsRevision: Int,
    onThemeChange: (AppPreferences.Theme) -> Unit,
    onMessage: (String) -> Unit,
    onExportBackup: (includeLibrary: Boolean, includePreferences: Boolean) -> Unit,
    onImportBackup: () -> Unit,
) {
    var mangaMode by remember(settingsRevision) { mutableStateOf(preferences.mangaMode()) }
    var showGuide by remember { mutableStateOf(false) }
    var backupLibrary by remember { mutableStateOf(true) }
    var backupPreferences by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = { showGuide = !showGuide }) {
            Text(if (showGuide) "العودة إلى الإعدادات" else "دليل الاستخدام والإعدادات")
        }
        if (showGuide) {
            Text("دليل Frankenstein Lab", style = MaterialTheme.typography.titleLarge)
            Text("المكتبة: زر «إضافة ملف» يفتح منتقي Android لاختيار ملف EPUB أو CBZ أو فيديو محلي. البحث يصفّي العناصر المحلية. إزالة العنصر تحذف الفهرس والتقدم فقط، ولا تحذف الملف الأصلي.")
            Text("الملفات: اختر مجلدًا من منتقي Android. الإذن محصور بالمجلد الذي تختاره. افتح مجلدًا بالضغط عليه؛ من قائمة «إجراءات» يمكن النسخ أو النقل أو إعادة التسمية أو الحذف. النسخ والنقل يستخدمان مجلد الوجهة الحالي، والحذف يعرض تأكيدًا قبل التنفيذ.")
            Text("قارئ المانغا: التمرير الرأسي يعرض الصفحات بالتتابع ويحفظ آخر صفحة. التقليب الأفقي يعمل باتجاه من اليمين إلى اليسار ويحفظ الصفحة. الصور الكبيرة تُفكّ إلى حجم مناسب للذاكرة. الحد الحالي لملف القراءة المحلي 512 ميغابايت.")
            Text("قارئ EPUB: أزرار الفصل السابق/التالي تنقل بين فصول الكتاب وتحفظ الفصل. أ− وأ+ تغيّران حجم النص بين 14 و32 نقطة. يعرض المحلل النصوص والصور المضمنة، ويقبل محتوى مفكوكًا حتى 64 ميغابايت.")
            Text("الفيديو: يشغّل Media3 الملف المحلي ويعيد فتحه من آخر موضع محفوظ. يتوقف عند انتقال التطبيق للخلفية ويُستأنف عند العودة فقط إذا كان يعمل قبلها، وتُحرر جلسة المشغل عند مغادرة الشاشة. لا تظهر خيارات جودة أو ترجمة أو بث لأنها غير مدمجة.")
            Text("المظهر: «اتباع مظهر الجهاز» يختار الفاتح أو الداكن وفق إعداد Android؛ الخياران الآخران يثبتان المظهر. وضع المانغا يغيّر طريقة العرض فقط، ولا يؤثر في مشغل الفيديو أو قارئ الروايات.")
            Text("الاتصال: وظائف القراءة والملفات الحالية تعمل على محتوى محلي اختاره المستخدم ولا تحتاج إلى اتصال بالإنترنت. لا توجد مستودعات إضافات أو خدمات مزامنة أو ذكاء اصطناعي مفعلة حاليًا.")
            Text("النسخ الاحتياطي: ملف JSON بإصدار محدد وفحص SHA-256، ويشمل عند اختيارك فهرس المكتبة وتقدمها وإعدادات التطبيق. لا ينسخ الملفات نفسها أو أذونات SAF؛ نقل الملف إلى جهاز آخر لا ينقل محتوى الوسائط.")
            Text("استعادة الافتراضيات: اضغط الزر في نهاية صفحة الإعدادات؛ يعيد المظهر إلى اتباع الجهاز، والمانغا إلى التمرير الرأسي، وحجم نص EPUB إلى 18. لا يمسح مكتبتك أو ملفاتك.")
        } else {
            Text("المظهر", style = MaterialTheme.typography.titleLarge)
            AppPreferences.Theme.entries.forEach { value ->
                TextButton(onClick = { onThemeChange(value); onMessage("حُفظ إعداد المظهر.") }) {
                    Text((if (currentTheme == value) "● " else "○ ") + when (value) {
                        AppPreferences.Theme.SYSTEM -> "اتباع مظهر الجهاز"
                        AppPreferences.Theme.LIGHT -> "فاتح"
                        AppPreferences.Theme.DARK -> "داكن"
                    })
                }
            }
            Text("قارئ المانغا", style = MaterialTheme.typography.titleLarge)
            AppPreferences.MangaMode.entries.forEach { value ->
                TextButton(onClick = {
                    mangaMode = value
                    preferences.setMangaMode(value)
                    onMessage("حُفظ إعداد قارئ المانغا.")
                }) {
                    Text((if (mangaMode == value) "● " else "○ ") + when (value) {
                        AppPreferences.MangaMode.CONTINUOUS_VERTICAL -> "تمرير رأسي مستمر"
                        AppPreferences.MangaMode.PAGED_HORIZONTAL -> "تقليب أفقي من اليمين إلى اليسار"
                    })
                }
            }
            Text("حجم نص EPUB يُعدّل من داخل القارئ بواسطة أ− وأ+. لا تظهر إعدادات لخدمات لم تُدمج.")
            Text("النسخ الاحتياطي والاستعادة", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = backupLibrary, onCheckedChange = { backupLibrary = it })
                Text("فهرس المكتبة والتقدم")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = backupPreferences, onCheckedChange = { backupPreferences = it })
                Text("إعدادات المظهر والقراء")
            }
            Text("يحتوي الملف عناوين العناصر وURIs وتقدمها والإعدادات المحددة. لا يحتوي ملفات الوسائط أو صلاحيات الوصول أو أسرارًا. ملف JSON غير مشفّر؛ اختر وجهة خاصة.")
            Button(
                enabled = backupLibrary || backupPreferences,
                onClick = { onExportBackup(backupLibrary, backupPreferences) },
            ) { Text("إنشاء نسخة احتياطية") }
            TextButton(onClick = onImportBackup) { Text("فحص واستعادة نسخة JSON") }
            TextButton(onClick = {
                preferences.reset()
                mangaMode = AppPreferences.MangaMode.CONTINUOUS_VERTICAL
                onThemeChange(AppPreferences.Theme.SYSTEM)
                onMessage("أُعيدت الإعدادات الافتراضية؛ لم تتغير ملفاتك أو المكتبة.")
            }) { Text("استعادة الإعدادات الافتراضية") }
        }
    }
}
