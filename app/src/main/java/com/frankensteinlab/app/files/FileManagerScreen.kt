package com.frankensteinlab.app.files

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TransferRequest(val source: DocumentFile, val move: Boolean)

@Composable
fun FileManagerScreen(
    onOpenLocalFile: (Uri, String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var treeUri by remember {
        mutableStateOf(context.getSharedPreferences("file_manager", Context.MODE_PRIVATE)
            .getString("root_uri", null)?.let(Uri::parse))
    }
    val root = remember(treeUri) { treeUri?.let { DocumentFile.fromTreeUri(context, it) } }
    var path by remember(treeUri) {
        mutableStateOf(root?.let { listOf(it) } ?: emptyList())
    }
    val current = path.lastOrNull()
    var children by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dialogMode by remember { mutableStateOf<String?>(null) }
    var editedDocument by remember { mutableStateOf<DocumentFile?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var pendingTransfer by remember { mutableStateOf<TransferRequest?>(null) }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val permissionResult = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            if (permissionResult.isFailure) {
                onMessage("تعذر حفظ إذن المجلد: ${permissionResult.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"}")
                return@rememberLauncherForActivityResult
            }
            context.getSharedPreferences("file_manager", Context.MODE_PRIVATE)
                .edit().putString("root_uri", uri.toString()).apply()
            treeUri = uri
            onMessage("تم فتح مجلد بإذن يحدده Android.")
        }
    }

    LaunchedEffect(current?.uri) {
        if (current == null) {
            children = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            children = withContext(Dispatchers.IO) {
                current.listFiles().sortedWith(
                    compareBy<DocumentFile> { !it.isDirectory }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() },
                )
            }
        } catch (failure: Exception) {
            error = failure.localizedMessage ?: "تعذر قراءة المجلد."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (path.size > 1) {
                OutlinedButton(onClick = { path = path.dropLast(1) }) { Text("رجوع للمجلد السابق") }
            }
            if (current == null) {
                Button(onClick = { treePicker.launch(null) }) { Text("اختيار مجلد محلي") }
            } else {
                TextButton(onClick = { treePicker.launch(null) }) { Text("تغيير المجلد") }
                TextButton(onClick = { dialogMode = "folder"; nameInput = "" }) { Text("مجلد جديد") }
            }
        }
        Text(current?.name ?: "لم يُحدد مجلد بعد")
        if (pendingTransfer != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (pendingTransfer!!.move) "اختر الوجهة ثم اضغط نقل هنا" else "اختر الوجهة ثم اضغط لصق هنا")
                Button(onClick = {
                    val destination = current
                    val request = pendingTransfer
                    if (destination == null || request == null) return@Button
                    if (request.source.isDirectory && path.any { it.uri == request.source.uri }) {
                        onMessage("لا يمكن نسخ مجلد إلى نفسه أو إلى أحد مجلداته الفرعية.")
                        return@Button
                    }
                    loading = true
                    scope.launch {
                        val result = runCatching {
                            SafFileOperations.copyOrMove(context, request.source, destination, request.move)
                        }
                        result.onSuccess {
                            pendingTransfer = null
                            children = withContext(Dispatchers.IO) { destination.listFiles().sortedBy { it.name?.lowercase(Locale.ROOT) } }
                            onMessage(if (request.move) "اكتمل النقل." else "اكتمل النسخ.")
                        }.onFailure { onMessage("لم تكتمل العملية: ${it.localizedMessage ?: "تحقق من إذن المجلد والمساحة."}") }
                        loading = false
                    }
                }) { Text(if (pendingTransfer!!.move) "نقل هنا" else "لصق هنا") }
                TextButton(onClick = { pendingTransfer = null }) { Text("إلغاء") }
            }
        }
        error?.let { Text("خطأ: $it") }
        if (loading) CircularProgressIndicator()
        if (current != null && !loading && children.isEmpty() && error == null) Text("المجلد فارغ.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(children, key = { it.uri.toString() }) { document ->
                var menuExpanded by remember(document.uri) { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (document.isDirectory) path = path + document
                        else onOpenLocalFile(document.uri, document.name ?: "ملف محلي")
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text((if (document.isDirectory) "مجلد: " else "ملف: ") + (document.name ?: "عنصر بلا اسم"))
                    TextButton(onClick = { menuExpanded = true }) { Text("إجراءات") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("إعادة تسمية") }, onClick = {
                            menuExpanded = false
                            editedDocument = document
                            nameInput = document.name.orEmpty()
                            dialogMode = "rename"
                        })
                        DropdownMenuItem(text = { Text("نسخ") }, onClick = {
                            menuExpanded = false
                            pendingTransfer = TransferRequest(document, false)
                            onMessage("انتقل إلى مجلد الوجهة ثم اختر «لصق هنا».")
                        })
                        DropdownMenuItem(text = { Text("نقل") }, onClick = {
                            menuExpanded = false
                            pendingTransfer = TransferRequest(document, true)
                            onMessage("انتقل إلى مجلد الوجهة ثم اختر «نقل هنا».")
                        })
                        DropdownMenuItem(text = { Text("حذف") }, onClick = {
                            menuExpanded = false
                            editedDocument = document
                            dialogMode = "delete"
                        })
                        if (!document.isDirectory) {
                            DropdownMenuItem(text = { Text("فتح داخل التطبيق") }, onClick = {
                                menuExpanded = false
                                onOpenLocalFile(document.uri, document.name ?: "ملف محلي")
                            })
                        }
                    }
                }
            }
        }
    }

    if (dialogMode == "folder" || dialogMode == "rename") {
        AlertDialog(
            onDismissRequest = { dialogMode = null },
            title = { Text(if (dialogMode == "folder") "إنشاء مجلد" else "إعادة تسمية") },
            text = {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("الاسم") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = current
                    val document = editedDocument
                    val action = dialogMode
                    val name = nameInput.trim()
                    if (name.isBlank() || name in setOf(".", "..") || name.contains('/')) {
                        onMessage("أدخل اسمًا صالحًا.")
                        return@TextButton
                    }
                    dialogMode = null
                    loading = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                if (action == "folder") {
                                    checkNotNull(target).createDirectory(name) ?: error("تعذر إنشاء المجلد.")
                                } else {
                                    checkNotNull(document)
                                    check(document.renameTo(name)) { "لم يقبل مزود الملفات إعادة التسمية." }
                                }
                            }
                        }
                        result.onFailure { onMessage(it.localizedMessage ?: "تعذر تنفيذ العملية.") }
                        if (target != null) children = withContext(Dispatchers.IO) { target.listFiles().sortedBy { it.name?.lowercase(Locale.ROOT) } }
                        loading = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { dialogMode = null }) { Text("إلغاء") } },
        )
    }

    if (dialogMode == "delete") {
        AlertDialog(
            onDismissRequest = { dialogMode = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("سيُحذف العنصر ومحتوى المجلدات التابعة له من مزود الملفات. لا يمكن التراجع عن ذلك.") },
            confirmButton = {
                TextButton(onClick = {
                    val document = editedDocument
                    dialogMode = null
                    loading = true
                    scope.launch {
                        val result = runCatching { withContext(Dispatchers.IO) { checkNotNull(document).let(SafFileOperations::deleteRecursively) } }
                        result.onFailure { onMessage("تعذر الحذف: ${it.localizedMessage ?: "خطأ غير معروف"}") }
                        if (current != null) children = withContext(Dispatchers.IO) { current.listFiles().sortedBy { it.name?.lowercase(Locale.ROOT) } }
                        loading = false
                    }
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { dialogMode = null }) { Text("إلغاء") } },
        )
    }
}

/** SAF-only operations; no broad storage permission is requested and no archive is extracted. */
private object SafFileOperations {
    private const val MAX_DEPTH = 48
    private const val MAX_ENTRIES = 20_000

    suspend fun copyOrMove(context: Context, source: DocumentFile, destination: DocumentFile, move: Boolean) =
        withContext(Dispatchers.IO) {
            require(source.uri != destination.uri) { "المصدر والوجهة متماثلان." }
            var count = 0
            copyNode(context, source, destination, 0, HashSet()) {
                count++
                require(count <= MAX_ENTRIES) { "المجلد أكبر من الحد الآمن للنسخ." }
            }
            if (move) {
                try {
                    deleteRecursively(source)
                } catch (failure: Exception) {
                    throw IOException(
                        "أُنشئت نسخة كاملة في الوجهة، لكن تعذر إكمال حذف الأصل. تحقّق من الوجهة قبل إعادة المحاولة.",
                        failure,
                    )
                }
            }
        }

    private fun copyNode(
        context: Context,
        source: DocumentFile,
        destination: DocumentFile,
        depth: Int,
        visited: MutableSet<String>,
        onEntry: () -> Unit,
    ): DocumentFile {
        require(depth <= MAX_DEPTH) { "عمق المجلدات يتجاوز الحد الآمن." }
        require(visited.add(source.uri.toString())) { "اكتُشفت دورة غير متوقعة في شجرة الملفات." }
        onEntry()
        val name = uniqueName(destination, source.name ?: "عنصر")
        if (source.isDirectory) {
            val target = destination.createDirectory(name) ?: error("تعذر إنشاء مجلد الوجهة.")
            try {
                source.listFiles().forEach { child -> copyNode(context, child, target, depth + 1, visited, onEntry) }
            } catch (failure: Exception) {
                runCatching { deleteRecursively(target) }
                throw failure
            }
            return target
        }
        val mime = source.type ?: MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase(Locale.ROOT))
            ?: "application/octet-stream"
        val target = destination.createFile(mime, name) ?: error("تعذر إنشاء ملف الوجهة.")
        try {
            val input = context.contentResolver.openInputStream(source.uri) ?: error("تعذر فتح الملف الأصلي.")
            val output = context.contentResolver.openOutputStream(target.uri, "w") ?: error("تعذر الكتابة إلى الوجهة.")
            input.use { inputStream -> output.use { outputStream -> inputStream.copyTo(outputStream, 64 * 1024) } }
        } catch (failure: Exception) {
            runCatching { target.delete() }
            throw failure
        }
        return target
    }

    private fun uniqueName(parent: DocumentFile, requested: String): String {
        if (parent.findFile(requested) == null) return requested
        val extension = requested.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        val base = if (extension == null) requested else requested.removeSuffix(".$extension")
        for (index in 1..999) {
            val candidate = if (extension == null) "$base (نسخة $index)" else "$base (نسخة $index).$extension"
            if (parent.findFile(candidate) == null) return candidate
        }
        error("يوجد عدد كبير من العناصر المتشابهة في مجلد الوجهة.")
    }

    fun deleteRecursively(document: DocumentFile) {
        val plan = ArrayList<DocumentFile>()
        var count = 0
        fun collect(node: DocumentFile, depth: Int) {
            require(depth <= MAX_DEPTH) { "عمق المجلدات يتجاوز حد الحذف الآمن." }
            count += 1
            require(count <= MAX_ENTRIES) { "عدد عناصر المجلد يتجاوز حد الحذف الآمن." }
            if (node.isDirectory) node.listFiles().forEach { child -> collect(child, depth + 1) }
            plan += node
        }
        collect(document, 0)
        plan.forEach { node ->
            check(node.delete()) { "رفض مزود الملفات حذف ${node.name ?: "العنصر"}." }
        }
    }
}
