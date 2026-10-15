package com.example.ui.screens
import androidx.compose.ui.text.drawText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Image
import com.example.core.text.PdfTextSearcher
import com.example.core.text.DocumentOutlineItem
import com.example.core.text.DocumentOutlineParser
import com.example.core.text.PdfSearchResult
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentFileEntity
import com.example.services.FileManager
import com.example.ui.component.ClaudeAppBar
import com.example.ui.component.ClaudeCard
import com.example.ui.component.FileIcon
import com.example.ui.component.formatFileSize
import com.example.ui.component.formatElapsedTime
import com.example.viewmodel.MainViewModel
import java.io.File
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.media.MediaPlayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileState by viewModel.currentFileState.collectAsState()
    val expandedZipPaths by viewModel.expandedZipPaths.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    
    val activeFile = remember(fileState) { fileState.fileOrNull }

    var isBarsVisible by remember { mutableStateOf(true) }

    val isHtmlFile = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm")
    }

    var isWebRenderActive by remember(activeFile) {
        // Keep the established native reader UI as the default for plain text and Markdown.
        mutableStateOf(activeFile?.extension?.lowercase() !in setOf("txt", "md"))
    }

    // HTML files are untrusted input: scripts stay off until the user opts in for this file.
    var htmlScriptsEnabled by remember(activeFile) { mutableStateOf(false) }

    val canBeRenderedInWeb = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm", "js", "ts", "css", "xml", "txt", "json", "md", "pdf")
    }
    
    var currentPdfPage by remember { mutableStateOf(1) }
    var totalPdfPages by remember { mutableStateOf(0) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    var isPdfNightMode by remember { mutableStateOf(false) }
    var scrollToPage by remember { mutableStateOf<Int?>(null) }
    var pendingLinkToOpen by remember { mutableStateOf<String?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var scrollToLineNumber by remember { mutableStateOf<Int?>(null) }
    var showAppearanceDialog by rememberSaveable { mutableStateOf(false) }
    var showOutlineSheet by rememberSaveable { mutableStateOf(false) }
    var markdownOutlineJumpIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var docxOutlineJumpIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var docxOutlineJumpTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var outlineItems by remember { mutableStateOf<List<DocumentOutlineItem>>(emptyList()) }

    var isSearchInDocumentOpen by rememberSaveable { mutableStateOf(false) }
    var inViewerSearchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var totalMatchesCount by remember { mutableStateOf(0) }
    var pdfSearchResults by remember { mutableStateOf<List<PdfSearchResult>>(emptyList()) }
    var docxWebView by remember(activeFile?.path) { mutableStateOf<android.webkit.WebView?>(null) }

    // PDF search effect
    LaunchedEffect(inViewerSearchQuery, activeFile?.path, fileState) {
        if (fileState is MainViewModel.FileContentState.PdfSuccess && inViewerSearchQuery.length >= 2 && activeFile != null) {
            val file = java.io.File(activeFile.path)
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                PdfTextSearcher.searchInPdf(file, inViewerSearchQuery)
            }
            pdfSearchResults = res
            totalMatchesCount = res.size
            currentMatchIndex = 0
            if (res.isNotEmpty()) {
                scrollToPage = res[0].pageNumber - 1
            }
        } else if (fileState is MainViewModel.FileContentState.PdfSuccess) {
            pdfSearchResults = emptyList()
            totalMatchesCount = 0
            currentMatchIndex = 0
        }
    }

    // Text search effect
    val textContent = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content ?: ""
    val textMatches = remember(textContent, inViewerSearchQuery) {
        if (inViewerSearchQuery.length >= 2) {
            com.example.core.text.PreviewTextUtils.findMatchOffsets(textContent, inViewerSearchQuery)
        } else emptyList()
    }
    LaunchedEffect(textMatches, textContent, fileState) {
        if (fileState is MainViewModel.FileContentState.TextSuccess) {
            totalMatchesCount = textMatches.size
            currentMatchIndex = 0
            scrollToLineNumber = textMatches.firstOrNull()?.let { offset ->
                textContent.substring(0, offset).count { it == '\n' } + 1
            }
        }
    }

    val context = LocalContext.current
    val viewerPreferences = remember(context) {
        context.getSharedPreferences("viewer_appearance", android.content.Context.MODE_PRIVATE)
    }
    var appearanceTheme by rememberSaveable {
        mutableStateOf(viewerPreferences.getString("theme", "Claude Glass") ?: "Claude Glass")
    }
    var appearanceFontSize by rememberSaveable {
        mutableStateOf(viewerPreferences.getInt("font_size_sp", 16).coerceIn(11, 22))
    }

    val previewView = androidx.compose.ui.platform.LocalView.current
    val previewWindow = remember(previewView) {
        var activityContext: android.content.Context? = previewView.context
        while (activityContext is android.content.ContextWrapper && activityContext !is android.app.Activity) {
            activityContext = activityContext.baseContext
        }
        (activityContext as? android.app.Activity)?.window
    }
    val originalStatusBarColor = remember(previewWindow) { previewWindow?.statusBarColor }
    val originalNavigationBarColor = remember(previewWindow) { previewWindow?.navigationBarColor }
    val originalSystemUiVisibility = remember(previewView) { previewView.systemUiVisibility }
    DisposableEffect(previewWindow, previewView, isBarsVisible) {
        val window = previewWindow
        val controller = window?.let { androidx.core.view.WindowInsetsControllerCompat(it, previewView) }
        controller?.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val systemBars = androidx.core.view.WindowInsetsCompat.Type.systemBars()
        if (!isBarsVisible && window != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            previewView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            controller?.hide(systemBars)
        } else {
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                originalStatusBarColor?.let { window.statusBarColor = it }
                originalNavigationBarColor?.let { window.navigationBarColor = it }
            }
            previewView.systemUiVisibility = originalSystemUiVisibility
            controller?.show(systemBars)
        }
        onDispose {
            controller?.show(systemBars)
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                originalStatusBarColor?.let { window.statusBarColor = it }
                originalNavigationBarColor?.let { window.navigationBarColor = it }
            }
            previewView.systemUiVisibility = originalSystemUiVisibility
        }
    }

    LaunchedEffect(activeFile?.path) { isBarsVisible = true }
    LaunchedEffect(activeFile?.path, activeFile?.extension, textContent, totalPdfPages) {
        val file = activeFile
        outlineItems = if (file == null) emptyList() else withContext(Dispatchers.IO) {
            when (file.extension.lowercase()) {
                "md" -> DocumentOutlineParser.fromMarkdown(textContent)
                "docx" -> com.example.core.format.DocxExtractor.extractText(File(file.path)).outline
                "pdf" -> (1..totalPdfPages.coerceAtLeast(0)).map { page ->
                    DocumentOutlineItem(title = "Page $page", level = 1, targetIndex = page - 1)
                }
                else -> emptyList()
            }
        }
    }

    // Restore previously opened file state on recreate
    // Attempted once per screen instance (process-death recovery). Without the flag, "Remove from history"
    // set the state to Idle and this effect immediately re-opened the same file.
    var restoreAttempted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(fileState) {
        if (fileState is MainViewModel.FileContentState.Idle && !restoreAttempted) {
            restoreAttempted = true
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val lastPath = prefs.getString("last_previewed_file_path", null)
            if (lastPath != null) {
                val file = java.io.File(lastPath)
                if (file.exists()) {
                    val entity = RecentFileEntity(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        extension = file.extension.lowercase(),
                        lastOpened = System.currentTimeMillis()
                    )
                    viewModel.openFile(entity)
                }
            }
        }
    }

    val handleBack = {
        val parentPath = activeFile?.parentZipPath
        if (parentPath != null) {
            viewModel.openParentZip(parentPath)
        } else {
            onBackClick()
        }
    }

    androidx.activity.compose.BackHandler {
        if (!isBarsVisible) isBarsVisible = true else handleBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBarsVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut()
            ) {
                val titleText = fileState.fileOrNull?.name ?: "Preview"
                
                var showMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current

                ClaudeAppBar(
                    title = titleText,
                    onNavIconClick = handleBack,
                    navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        val canSearchInDoc = fileState is MainViewModel.FileContentState.PdfSuccess ||
                            fileState is MainViewModel.FileContentState.DocxSuccess ||
                            fileState is MainViewModel.FileContentState.TextSuccess ||
                            (activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv"))
                        if (canSearchInDoc) {
                            IconButton(onClick = { isSearchInDocumentOpen = !isSearchInDocumentOpen }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Find in document",
                                    tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val activeExtension = activeFile?.extension?.lowercase().orEmpty()
                        val isTextState = fileState is MainViewModel.FileContentState.TextSuccess ||
                            activeExtension in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv")
                        val canCustomizeAppearance = activeExtension in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql")
                        val canShowOutline = activeExtension in setOf("md", "pdf", "docx")
                        val canUseImmersive = activeExtension in setOf("pdf", "docx", "txt", "md", "png", "jpg", "jpeg", "webp", "gif", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv")
                        if (isTextState) {
                            IconButton(onClick = {
                                val textToCopy = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                    ?: runCatching { activeFile?.let { java.io.File(it.path).readText() } }.getOrNull()
                                if (!textToCopy.isNullOrEmpty()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(activeFile?.name ?: "File Content", textToCopy)
                                    clipboard?.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy all text",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (canCustomizeAppearance) {
                            IconButton(onClick = { isWebRenderActive = !isWebRenderActive }) {
                                Icon(
                                    imageVector = if (isWebRenderActive) Icons.Outlined.Code else Icons.Outlined.Language,
                                    contentDescription = if (isWebRenderActive) "Switch to code/text view" else "Switch to rendered preview"
                                )
                            }
                            IconButton(onClick = {
                                // Route text/Markdown into the native themed reader before opening appearance.
                                isWebRenderActive = false
                                showAppearanceDialog = true
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Appearance")
                            }
                        }
                        if (canShowOutline) {
                            IconButton(onClick = { showOutlineSheet = true }) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Document outline")
                            }
                        }
                        if (canUseImmersive) {
                            IconButton(onClick = { isBarsVisible = !isBarsVisible }) {
                                Icon(
                                    imageVector = if (isBarsVisible) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                                    contentDescription = if (isBarsVisible) "Enter immersive focus mode" else "Exit immersive focus mode"
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share file") },
                                    onClick = {
                                        showMenu = false
                                        activeFile?.let { fileEntity ->
                                            shareFile(context, File(fileEntity.path), fileEntity.extension)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share file"
                                        )
                                    }
                                )

                                val isPdf = activeFile?.extension?.lowercase() == "pdf"
                                val isDocx = activeFile?.extension?.lowercase() == "docx"
                                if (isPdf || isDocx) {
                                    DropdownMenuItem(
                                        text = { Text("Go to Page ($currentPdfPage / $totalPdfPages)") },
                                        onClick = {
                                            showMenu = false
                                            showJumpDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.InsertDriveFile,
                                                contentDescription = "Go to Page"
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isPdfNightMode) "Light Mode" else "Dark Mode") },
                                        onClick = {
                                            showMenu = false
                                            isPdfNightMode = !isPdfNightMode
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isPdfNightMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                                contentDescription = "Toggle Dark Mode"
                                             )
                                        }
                                    )
                                    if (isPdf) {
                                        DropdownMenuItem(
                                            text = { Text("Open in System Viewer") },
                                            onClick = {
                                                showMenu = false
                                                activeFile?.let { fileEntity ->
                                                    openPdfInBrowser(context, java.io.File(fileEntity.path))
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Language,
                                                    contentDescription = "Open in System Viewer"
                                                )
                                            }
                                        )
                                    }
                                }

                                val isTextFile = activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv")
                                if (isTextFile) {
                                    DropdownMenuItem(
                                        text = { Text("Go to Line...") },
                                        onClick = {
                                            showMenu = false
                                            showJumpToLineDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FormatLineSpacing,
                                                contentDescription = "Go to Line"
                                            )
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Print / Export as PDF") },
                                    onClick = {
                                        showMenu = false
                                        activeFile?.let { entity ->
                                            val ext = entity.extension.lowercase()
                                            if (ext in setOf("png", "jpg", "jpeg", "webp", "gif")) {
                                                com.example.utils.FilePrintHelper.printImage(context, java.io.File(entity.path), entity.name)
                                            } else {
                                                val content = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                                    ?: runCatching { java.io.File(entity.path).readText() }.getOrNull()
                                                    ?: ""
                                                com.example.utils.FilePrintHelper.printTextOrMarkdown(context, content, entity.name)
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Print,
                                            contentDescription = "Print or Export as PDF"
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Properties") },
                                    onClick = {
                                        showMenu = false
                                        showPropertiesDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "Properties"
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Remove from history", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        activeFile?.let { fileEntity ->
                                            viewModel.deleteRecentFile(fileEntity)
                                            handleBack()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from history",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            val isPdf = fileState is MainViewModel.FileContentState.PdfSuccess
            val isImage = fileState is MainViewModel.FileContentState.ImageSuccess
            val isText = fileState is MainViewModel.FileContentState.TextSuccess ||
                (activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv"))

            val showFloatingBar = isPdf || isImage || isText

            if (showFloatingBar) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isBarsVisible,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (isPdf) {
                                    // Page indicator / Jump button
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { showJumpDialog = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MenuBook,
                                                contentDescription = "Page indicator",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$currentPdfPage / ${totalPdfPages.coerceAtLeast(1)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Find in PDF
                                    IconButton(
                                        onClick = { isSearchInDocumentOpen = !isSearchInDocumentOpen },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in document",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Night / Light Mode toggle pill
                                    IconButton(
                                        onClick = { isPdfNightMode = !isPdfNightMode },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPdfNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle night mode",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isPdfNightMode) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Share File
                                    IconButton(
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (isImage) {
                                    // Image counter
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Image,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Photo Viewer",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Print Image
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                com.example.utils.FilePrintHelper.printImage(context, java.io.File(it.path), it.name)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print photo",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Share Image
                                    IconButton(
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share image",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (isText) {
                                    // Find in Text
                                    IconButton(
                                        onClick = { isSearchInDocumentOpen = !isSearchInDocumentOpen },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in document",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Jump to line in Text/Code
                                    IconButton(
                                        onClick = { showJumpToLineDialog = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.FormatLineSpacing,
                                            contentDescription = "Jump to line",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Print / Export as PDF
                                    IconButton(
                                        onClick = {
                                            val textToPrint = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                                ?: runCatching { activeFile?.let { java.io.File(it.path).readText() } }.getOrNull()
                                                ?: ""
                                            com.example.utils.FilePrintHelper.printTextOrMarkdown(context, textToPrint, activeFile?.name ?: "Document")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print or export as PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Copy all text
                                    IconButton(
                                        onClick = {
                                            val textToCopy = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                                ?: runCatching { activeFile?.let { java.io.File(it.path).readText() } }.getOrNull()
                                            if (!textToCopy.isNullOrEmpty()) {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText(activeFile?.name ?: "File Content", textToCopy)
                                                clipboard?.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy all text",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    val isEditable = (fileState as? MainViewModel.FileContentState.TextSuccess)?.editable == true
                                    if (isEditable) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { onNavigateToEditor() }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Edit",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Share Text
                                    IconButton(
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share file",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("file_preview_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (isBarsVisible) {
                        it.padding(innerPadding)
                    } else {
                        it
                    }
                }
        ) {
            // Floating In-Viewer Search Bar Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearchInDocumentOpen,
                enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(44.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (inViewerSearchQuery.isEmpty()) {
                                Text(
                                    "Find in document...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = inViewerSearchQuery,
                                onValueChange = { inViewerSearchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (inViewerSearchQuery.isNotEmpty()) {
                            val matchLabel = if (totalMatchesCount > 0) {
                                val pageInfo = if (pdfSearchResults.isNotEmpty()) " (p.${pdfSearchResults.getOrNull(currentMatchIndex)?.pageNumber ?: 1})" else ""
                                "${currentMatchIndex + 1}/$totalMatchesCount$pageInfo"
                            } else "0 matches"

                            Text(
                                text = matchLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Previous Match
                            IconButton(
                                onClick = {
                                    if (pdfSearchResults.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex - 1 + pdfSearchResults.size) % pdfSearchResults.size
                                        scrollToPage = pdfSearchResults[currentMatchIndex].pageNumber - 1
                                    } else if (textMatches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex - 1 + textMatches.size) % textMatches.size
                                    }
                                },
                                enabled = totalMatchesCount > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(18.dp))
                            }

                            // Next Match
                            IconButton(
                                onClick = {
                                    if (pdfSearchResults.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex + 1) % pdfSearchResults.size
                                        scrollToPage = pdfSearchResults[currentMatchIndex].pageNumber - 1
                                    } else if (textMatches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex + 1) % textMatches.size
                                    }
                                },
                                enabled = totalMatchesCount > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", modifier = Modifier.size(18.dp))
                            }
                        }

                        IconButton(
                            onClick = {
                                isSearchInDocumentOpen = false
                                inViewerSearchQuery = ""
                                pdfSearchResults = emptyList()
                                totalMatchesCount = 0
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            when (val state = fileState) {
                is MainViewModel.FileContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.component.PremiumLoadingIndicator(text = "Loading file...")
                    }
                }
                is MainViewModel.FileContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preview Error",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is MainViewModel.FileContentState.ArchivePasswordRequired -> {
                    var inputPassword by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { onBackClick() },
                        title = { Text("Password Required") },
                        text = {
                            Column {
                                Text("This archive is password protected.", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputPassword,
                                    onValueChange = { inputPassword = it },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.openFile(state.file, inputPassword)
                            }) {
                                Text("Open")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onBackClick() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                is MainViewModel.FileContentState.TextSuccess -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val notice = state.readOnlyReason
                            ?: if (state.truncated) {
                                "Showing the first ${state.content.length} characters of a ${formatFileSize(state.totalBytes)} file. " +
                                    "Read-only, so the rest of the file can't be overwritten."
                            } else null
                        if (notice != null) NoticeBanner(notice)
                        if (isHtmlFile && isWebRenderActive && !htmlScriptsEnabled) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                                ) {
                                    Text(
                                        "Scripts are off for safety.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { htmlScriptsEnabled = true }) { Text("Run scripts") }
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isWebRenderActive) {
                                WebViewPreview(
                                    fileEntity = state.file,
                                    allowScripts = htmlScriptsEnabled,
                                    searchQuery = inViewerSearchQuery,
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (state.file.extension.lowercase() == "md") {
                                MarkdownPreview(
                                    markdownText = state.content,
                                    fileEntity = state.file,
                                    fontSizeSetting = settings.fontSize,
                                    appearanceTheme = appearanceTheme,
                                    appearanceFontSize = appearanceFontSize,
                                    scrollToHeadingIndex = markdownOutlineJumpIndex,
                                    onOutlineJumpHandled = { markdownOutlineJumpIndex = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CodePreview(
                                    codeText = state.content,
                                    fileEntity = state.file,
                                    fontSizeSetting = settings.fontSize,
                                    appearanceTheme = appearanceTheme,
                                    appearanceFontSize = appearanceFontSize,
                                    wrapSetting = settings.wordWrap,
                                    showLineNumbers = settings.showLineNumbers,
                                    scrollToLine = scrollToLineNumber,
                                    onScrollToLineHandled = { scrollToLineNumber = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                is MainViewModel.FileContentState.CsvSuccess -> {
                    CsvPreview(
                        csvRows = state.rows,
                        fileEntity = state.file,
                        viewModel = viewModel,
                        editable = state.editable,
                        notice = state.readOnlyReason
                            ?: if (state.truncated) "Large file: showing the first ${state.rows.size} rows. Read-only, so nothing is lost on save." else null,
                        onDiscardAndExit = { handleBack() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MainViewModel.FileContentState.ZipSuccess -> {
                    ZipPreview(
                        zipRoot = state.root,
                        fileEntity = state.file,
                        expandedPaths = expandedZipPaths,
                        onToggleExpand = { path ->
                            viewModel.toggleZipPathExpanded(path)
                        },
                        onZipEntryClick = { node, pwd ->
                            viewModel.openZipEntry(state.file, node, pwd)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MainViewModel.FileContentState.ImageSuccess -> {
                    ImagePreview(
                        fileEntity = state.file,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { isBarsVisible = !isBarsVisible }
                    )
                }
                is MainViewModel.FileContentState.PdfSuccess -> {
                    PdfPreview(
                        fileEntity = state.file,
                        isNightMode = isPdfNightMode,
                        scrollToPage = scrollToPage,
                        onScrollToPageHandled = { scrollToPage = null },
                        onLinkClicked = { _ -> },
                        modifier = Modifier.fillMaxSize(),
                        onSingleTap = { isBarsVisible = !isBarsVisible },
                        onPageChanged = { page, total ->
                            currentPdfPage = page
                            totalPdfPages = total
                        },
                        onBackClick = onBackClick
                    )
                }
                is MainViewModel.FileContentState.DocxSuccess -> {
                    DocxPreviewWebView(
                        base64Docx = state.base64Data,
                        fileEntity = state.file,
                        isNightMode = isPdfNightMode,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onPageChanged = { page, total ->
                            currentPdfPage = page
                            totalPdfPages = total
                        },
                        scrollToPage = scrollToPage,
                        onScrollProgressHandled = { scrollToPage = null },
                        scrollToHeadingIndex = docxOutlineJumpIndex,
                        scrollToHeadingTitle = docxOutlineJumpTitle,
                        onOutlineJumpHandled = {
                            docxOutlineJumpIndex = null
                            docxOutlineJumpTitle = null
                        }
                    )
                }
                is MainViewModel.FileContentState.PptxSuccess -> {
                    PptxPreviewWebView(
                        base64Pptx = state.base64Data,
                        fileEntity = state.file,
                        isNightMode = isPdfNightMode,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MainViewModel.FileContentState.MediaSuccess -> {
                    if (state.isAudio) {
                        AudioPlayer(
                            fileEntity = state.file,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VideoPlayer(
                            fileEntity = state.file,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is MainViewModel.FileContentState.BinarySuccess -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val header = listOfNotNull(state.label?.let { "Detected: $it" }, state.note).joinToString(" \u2022 ")
                        if (header.isNotEmpty()) NoticeBanner(header)
                        HexViewer(
                            hexRows = state.hexRows,
                            asciiRows = state.asciiRows,
                            fileEntity = state.file,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
                is MainViewModel.FileContentState.SvgSuccess -> SvgViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.SqliteSuccess -> SqliteViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.FontSuccess -> FontViewer(state.file, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.Model3dSuccess -> Model3dViewer(state.mesh, state.label, Modifier.fillMaxSize())
                is MainViewModel.FileContentState.HtmlSuccess -> HtmlStringViewer(state.html, Modifier.fillMaxSize())
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No file selected", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

        if (!isBarsVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    shadowElevation = 5.dp,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        .clickable { isBarsVisible = true; handleBack() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .clickable { isBarsVisible = true }
                ) {
                    Text("Tap to exit focus", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
        }
    }

    // Glassmorphism Modal Bottom Sheet for File Properties
    if (showPropertiesDialog && activeFile != null) {
        com.example.ui.component.FilePropertiesBottomSheet(
            fileEntity = activeFile,
            onDismissRequest = { showPropertiesDialog = false }
        )
    }

    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Viewer appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    listOf("Claude Glass", "Nord Dark", "Warm Parchment").forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                appearanceTheme = theme
                                viewerPreferences.edit().putString("theme", theme).apply()
                            }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appearanceTheme == theme,
                                onClick = {
                                    appearanceTheme = theme
                                    viewerPreferences.edit().putString("theme", theme).apply()
                                }
                            )
                            Column {
                                Text(theme, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when (theme) {
                                        "Nord Dark" -> "Cool, low-glare dark palette"
                                        "Warm Parchment" -> "Soft paper tones"
                                        else -> "Adaptive, warm glass"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Font size", style = MaterialTheme.typography.labelLarge)
                            Text("$appearanceFontSize sp", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                enabled = appearanceFontSize > 11,
                                onClick = {
                                    appearanceFontSize = (appearanceFontSize - 1).coerceAtLeast(11)
                                    viewerPreferences.edit().putInt("font_size_sp", appearanceFontSize).apply()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) { Text("−") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                enabled = appearanceFontSize < 22,
                                onClick = {
                                    appearanceFontSize = (appearanceFontSize + 1).coerceAtMost(22)
                                    viewerPreferences.edit().putInt("font_size_sp", appearanceFontSize).apply()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) { Text("+") }
                        }
                    }
                    Text("Saved automatically • 11–22 sp", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAppearanceDialog = false }) { Text("Done") } }
        )
    }

    if (showOutlineSheet) {
        ModalBottomSheet(onDismissRequest = { showOutlineSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Outline / Table of contents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                val outlineExtension = activeFile?.extension?.lowercase().orEmpty()
                if (outlineItems.isEmpty()) {
                    Text(
                        text = when (outlineExtension) {
                            "md" -> "No Markdown headings found. Use #, ##, or ### headings to create an outline."
                            "docx" -> "No Word heading styles found. Apply Heading 1–6 styles in Word to create an outline."
                            "pdf" -> "PDF pages are loading, or the document has no readable pages."
                            else -> "No outline available."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                        itemsIndexed(outlineItems) { _, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    when (outlineExtension) {
                                        "md" -> {
                                            isWebRenderActive = false
                                            markdownOutlineJumpIndex = item.targetIndex
                                        }
                                        "docx" -> {
                                            docxOutlineJumpIndex = item.targetIndex
                                            docxOutlineJumpTitle = item.title
                                        }
                                        "pdf" -> scrollToPage = item.targetIndex
                                    }
                                    showOutlineSheet = false
                                }.padding(
                                    start = ((item.level - 1).coerceIn(0, 5) * 12).dp,
                                    end = 8.dp, top = 11.dp, bottom = 11.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(item.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showJumpToLineDialog) {
        var lineInput by remember { mutableStateOf("") }
        val textLinesCount = remember(fileState) {
            val content = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
            content?.count { it == '\n' }?.plus(1) ?: 1000
        }
        AlertDialog(
            onDismissRequest = { showJumpToLineDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val lNum = lineInput.toIntOrNull()
                        if (lNum != null && lNum > 0) {
                            scrollToLineNumber = lNum
                        } else {
                            android.widget.Toast.makeText(context, "Invalid line number", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showJumpToLineDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToLineDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Go to Line") },
            text = {
                Column {
                    Text("Enter line number (1 to $textLinesCount):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lineInput,
                        onValueChange = { lineInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pNum = pageInput.toIntOrNull()
                        if (pNum != null && pNum in 1..totalPdfPages) {
                            scrollToPage = pNum - 1
                        } else {
                            android.widget.Toast.makeText(context, "Invalid page number", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $totalPdfPages):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (pendingLinkToOpen != null) {
        AlertDialog(
            onDismissRequest = { pendingLinkToOpen = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLinkToOpen?.let { url ->
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Failed to open link", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        pendingLinkToOpen = null
                    }
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLinkToOpen = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Open Link") },
            text = { Text("Do you want to open this link in your browser?\n\n$pendingLinkToOpen") }
        )
    }
}

@Composable
fun PropertyItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}


// Helper: Fast and robust code syntax highlighter
@Composable
fun MarkdownPreview(
    markdownText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    modifier: Modifier = Modifier,
    appearanceTheme: String = "Claude Glass",
    appearanceFontSize: Int? = null,
    scrollToHeadingIndex: Int? = null,
    onOutlineJumpHandled: () -> Unit = {}
) {
    val isNightMode = androidx.compose.foundation.isSystemInDarkTheme()
    var generatedHtml by remember(markdownText, isNightMode, fontSizeSetting, appearanceTheme, appearanceFontSize) { mutableStateOf<String?>(null) }
    var isWebViewLoading by remember(markdownText, appearanceTheme, appearanceFontSize) { mutableStateOf(true) }
    var markdownWebViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    LaunchedEffect(scrollToHeadingIndex, markdownWebViewRef, isWebViewLoading) {
        if (scrollToHeadingIndex != null && markdownWebViewRef != null && !isWebViewLoading) {
            markdownWebViewRef?.evaluateJavascript(
                "if (typeof window.scrollToOutline === 'function') { window.scrollToOutline($scrollToHeadingIndex); }",
                null
            )
            onOutlineJumpHandled()
        }
    }

    LaunchedEffect(markdownText, isNightMode, fontSizeSetting, appearanceTheme, appearanceFontSize) {
        val htmlContent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val parser = org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
            val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
            val rawHtml = com.example.core.text.HtmlSanitizer.sanitize(
                org.intellij.markdown.html.HtmlGenerator(markdownText, parsedTree, org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()).generateHtml(),
                keepImages = true
            )
            var headingOrdinal = 0
            val outlinedHtml = rawHtml.replace(Regex("<h([1-6])([^>]*)>")) { match ->
                val ordinal = headingOrdinal++
                val attrs = match.groupValues[2]
                if (Regex("\\bdata-fileclaw-outline-index\\s*=").containsMatchIn(attrs)) match.value
                else "<h" + match.groupValues[1] + attrs + " data-fileclaw-outline-index=\"" + ordinal + "\">"
            }

            val fontSize = (appearanceFontSize ?: when (fontSizeSetting) {
                "Small" -> 14
                "Large" -> 20
                else -> 16
            }).coerceIn(11, 22).toString() + "px"
            val nord = appearanceTheme == "Nord Dark"
            val parchment = appearanceTheme == "Warm Parchment"

            // Claude Theme Colors (EXACT from File Claw - MD Preview Theme Fix Prompt)
            val bgColor = when { nord -> "#2e3440"; parchment -> "#f4ecd8"; isNightMode -> "#151514"; else -> "#faf9f5" } // canvas
            val textPrimary = when { nord -> "#eceff4"; parchment -> "#29261f"; isNightMode -> "#f5f5f3"; else -> "#141413" } // ink
            val textBody = when { nord -> "#d8dee9"; parchment -> "#40392f"; isNightMode -> "#d1cfc7"; else -> "#3d3d3a" } // body
            val textBodyStrong = when { nord -> "#eceff4"; parchment -> "#332d24"; isNightMode -> "#faf9f5"; else -> "#252523" } // body_strong
            val textSecondary = when { nord -> "#aab4c5"; parchment -> "#756b5b"; isNightMode -> "#a39f93"; else -> "#6c6a64" } // muted
            val textSecondarySoft = when { nord -> "#9aa7bd"; parchment -> "#8a7c66"; isNightMode -> "#8e8779"; else -> "#8e8b82" } // muted_soft
            val accent = when { nord -> "#88c0d0"; parchment -> "#a85f3e"; isNightMode -> "#e08567"; else -> "#cc785c" } // primary (coral)
            val accentActive = when { nord -> "#8fbcbb"; parchment -> "#8e4e32"; isNightMode -> "#f09a7d"; else -> "#a9583e" } // primary_active
            val border = when { nord -> "#434c5e"; parchment -> "#d8ccb2"; isNightMode -> "#2d2b28"; else -> "#e6dfd8" } // hairline
            val borderSoft = when { nord -> "#3b4252"; parchment -> "#e7dcc5"; isNightMode -> "#211f1d"; else -> "#ebe6df" } // hairline_soft
            val cardBg = when { nord -> "#3b4252"; parchment -> "#eadfc7"; isNightMode -> "#2a2723"; else -> "#efe9de" } // surface_card
            val quoteBg = when { nord -> "#343c4d"; parchment -> "#ede2cb"; isNightMode -> "#1c1a18"; else -> "#f5f0e8" } // surface_soft
            val inlineCodeBg = when { nord -> "#434c5e"; parchment -> "#e6d8b9"; isNightMode -> "#2d2823"; else -> "#e8e0d2" } // surface_cream_strong
            val codeBlockOuterBg = when { nord -> "#242933"; parchment -> "#292722"; isNightMode -> "#0d0d0c"; else -> "#181715" } // surface_dark
            val codeBlockInnerBg = when { nord -> "#2e3440"; parchment -> "#34302a"; isNightMode -> "#161513"; else -> "#1f1e1b" } // surface_dark_soft
            val codeBlockHeaderBg = when { nord -> "#3b4252"; parchment -> "#413a30"; isNightMode -> "#1d1b19"; else -> "#252320" } // surface_dark_elevated
            val codeBlockText = when { nord -> "#eceff4"; parchment -> "#f4ecd8"; isNightMode -> "#f5f5f3"; else -> "#faf9f5" } // on_dark
            val codeBlockButtons = when { nord -> "#aab4c5"; parchment -> "#c7bba2"; isNightMode -> "#a09d96"; else -> "#8e8b82" } // on_dark_soft

            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                    :root {
                        --bg-color: $bgColor;
                        --text-primary: $textPrimary;
                        --text-body: $textBody;
                        --text-body-strong: $textBodyStrong;
                        --text-secondary: $textSecondary;
                        --text-secondary-soft: $textSecondarySoft;
                        --accent: $accent;
                        --accent-active: $accentActive;
                        --border: $border;
                        --border-soft: $borderSoft;
                        --card-bg: $cardBg;
                        --quote-bg: $quoteBg;
                        --inline-code-bg: $inlineCodeBg;
                        --code-block-outer: $codeBlockOuterBg;
                        --code-block-inner: $codeBlockInnerBg;
                        --code-block-header: $codeBlockHeaderBg;
                        --code-text: $codeBlockText;
                        --code-buttons: $codeBlockButtons;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-body);
                        font-size: $fontSize;
                        line-height: 1.55;
                        padding: 16px;
                        margin: 0;
                        word-wrap: break-word;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: var(--text-primary);
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 700;
                    }
                    h1 {
                        font-size: 28px;
                        border-bottom: 2px solid var(--border);
                        padding-bottom: 8px;
                    }
                    h2 { font-size: 22px; }
                    h3 { font-size: 18px; font-weight: 600; }
                    p {
                        margin-top: 0;
                        margin-bottom: 16px;
                    }
                    li {
                        margin: 8px 0;
                    }
                    ul, ol {
                        padding-left: 24px;
                    }
                    a {
                        color: var(--accent);
                        text-decoration: none;
                    }
                    a:hover {
                        color: var(--accent-active);
                        text-decoration: underline;
                    }
                    code {
                        font-family: "JetBrains Mono", ui-monospace, Consolas, Monaco, monospace;
                        background-color: var(--inline-code-bg);
                        color: var(--accent);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    pre {
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        overflow: visible;
                    }
                    .code-block-container {
                        background-color: var(--code-block-outer);
                        border-radius: 12px;
                        margin-bottom: 16px;
                        overflow: hidden;
                        border: 1px solid var(--border);
                    }
                    .code-header {
                        background-color: var(--code-block-header);
                        padding: 8px 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .code-lang {
                        color: var(--text-secondary-soft);
                        font-size: 12px;
                        font-family: monospace;
                        text-transform: uppercase;
                    }
                    .copy-btn {
                        background: transparent;
                        border: 1px solid var(--code-buttons);
                        color: var(--text-secondary-soft);
                        padding: 4px 12px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    .copy-btn:hover {
                        background: rgba(255, 255, 255, 0.1);
                    }
                    .code-content {
                        background-color: var(--code-block-inner);
                        padding: 16px;
                        overflow-x: auto;
                    }
                    .code-content code {
                        background-color: transparent;
                        color: var(--code-text);
                        padding: 0;
                        font-size: $fontSize;
                        line-height: 1.6;
                    }
                    blockquote {
                        margin: 0 0 16px 0;
                        padding: 12px 20px;
                        color: var(--text-body);
                        border-left: 4px solid var(--accent);
                        background-color: var(--quote-bg);
                        border-radius: 0 8px 8px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin-bottom: 16px;
                        display: block;
                        overflow-x: auto;
                        border: 1px solid var(--border);
                    }
                    th, td {
                        border: 1px solid var(--border);
                        padding: 12px;
                    }
                    th {
                        background-color: var(--quote-bg);
                        font-weight: 600;
                        color: var(--text-body-strong);
                    }
                    tr:nth-child(even) {
                        background-color: var(--bg-color);
                    }
                    tr:nth-child(odd) {
                        background-color: var(--bg-color);
                    }
                    hr {
                        border: 0;
                        border-top: 2px solid var(--border);
                        margin: 24px 0;
                    }
                    img {
                        max-width: 100%;
                        border-radius: 8px;
                    }
                </style>
                <script>
                    window.scrollToOutline = function(index) {
                        var target = document.querySelector('[data-fileclaw-outline-index="' + index + '"]');
                        if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    };
                    function copyCode(btn, codeId) {
                        var codeText = document.getElementById(codeId).innerText;
                        if(window.Android) {
                            window.Android.copyToClipboard(codeText);
                            var originalText = btn.innerText;
                            btn.innerText = 'Copied!';
                            setTimeout(function() {
                                btn.innerText = originalText;
                            }, 2000);
                        }
                    }
                    
                    document.addEventListener("DOMContentLoaded", function() {
                        var pres = document.querySelectorAll('pre');
                        pres.forEach(function(pre, index) {
                            if (pre.parentNode.className === 'code-content') return;
                            
                            var code = pre.querySelector('code');
                            var lang = 'Code';
                            if (code && code.className) {
                                var match = code.className.match(/language-(\w+)/);
                                if (match) lang = match[1];
                            }
                            
                            var codeId = 'code-' + index;
                            if (code) { code.id = codeId; } else { pre.id = codeId; }
                            
                            var container = document.createElement('div');
                            container.className = 'code-block-container';
                            
                            var header = document.createElement('div');
                            header.className = 'code-header';
                            
                            var langSpan = document.createElement('span');
                            langSpan.className = 'code-lang';
                            langSpan.innerText = lang;
                            
                            var copyBtn = document.createElement('button');
                            copyBtn.className = 'copy-btn';
                            copyBtn.innerText = 'Copy';
                            copyBtn.onclick = function() { copyCode(this, codeId); };
                            
                            header.appendChild(langSpan);
                            header.appendChild(copyBtn);
                            
                            var content = document.createElement('div');
                            content.className = 'code-content';
                            
                            pre.parentNode.insertBefore(container, pre);
                            container.appendChild(header);
                            container.appendChild(content);
                            content.appendChild(pre);
                        });
                    });
                </script>
            </head>
            <body>
                $outlinedHtml
            </body>
            </html>
            """.trimIndent()
        }
        generatedHtml = htmlContent
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        if (generatedHtml != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }
                        setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
                        setInitialScale(0)
                        markdownWebViewRef = this
                        tag = generatedHtml
                        addJavascriptInterface(WebAppInterface(context), "Android")
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                isWebViewLoading = false
                            }
                        }
                        
                        loadDataWithBaseURL(null, generatedHtml!!, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    markdownWebViewRef = webView
                    if (webView.tag != generatedHtml) {
                        webView.tag = generatedHtml
                        webView.loadDataWithBaseURL(null, generatedHtml!!, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (generatedHtml == null || isWebViewLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.component.PremiumLoadingIndicator(text = "Preparing Document...")
            }
        }
    }
}

class WebAppInterface(private val context: android.content.Context) {
    @android.webkit.JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Code", text)
        clipboard.setPrimaryClip(clip)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun CodePreview(
    codeText: String,
    fileEntity: RecentFileEntity,
    fontSizeSetting: String,
    wrapSetting: Boolean,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier,
    scrollToLine: Int? = null,
    onScrollToLineHandled: () -> Unit = {},
    appearanceTheme: String = "Claude Glass",
    appearanceFontSize: Int? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isAllCopied by remember { mutableStateOf(false) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(isAllCopied) {
        if (isAllCopied) {
            kotlinx.coroutines.delay(1800)
            isAllCopied = false
        }
    }

    Column(modifier = modifier) {
        // Tag Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeText))
                    isAllCopied = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAllCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Entire Code",
                        modifier = Modifier.size(14.dp),
                        tint = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAllCopied) "Copied!" else "Copy All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllCopied) Color(0xFF427A5B) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "WebView Render",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val htmlContent = remember(codeText, isDark, fontSizeSetting, appearanceTheme, appearanceFontSize, wrapSetting, showLineNumbers) {
                val escapedCode = codeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val bgColor = when (appearanceTheme) {
                    "Nord Dark" -> "#2e3440"
                    "Warm Parchment" -> "#f4ecd8"
                    else -> if (isDark) "#151514" else "#faf9f5"
                }
                val textColor = when (appearanceTheme) {
                    "Nord Dark" -> "#d8dee9"
                    "Warm Parchment" -> "#40392f"
                    else -> if (isDark) "#d1cfc7" else "#3d3d3a"
                }
                val fSize = (appearanceFontSize ?: when (fontSizeSetting) {
                    "Small" -> 12
                    "Large" -> 18
                    else -> 14
                }).coerceIn(11, 22).toString() + "px"
                val wWrap = if (wrapSetting) "pre-wrap" else "pre"
                val wWord = if (wrapSetting) "break-word" else "normal"
                val theme = when (appearanceTheme) {
                    "Nord Dark" -> "nord.min.css"
                    "Warm Parchment" -> "vs.min.css"
                    else -> if (isDark) "vs2015.min.css" else "vs.min.css"
                }
                
                val lineNumbersCss = if (showLineNumbers) """
                    .hljs {
                        counter-reset: linenumber;
                    }
                    .hljs-line {
                        display: block;
                        counter-increment: linenumber;
                    }
                    .hljs-line::before {
                        content: counter(linenumber);
                        display: inline-block;
                        width: 3em;
                        margin-right: 1em;
                        text-align: right;
                        color: #888;
                        user-select: none;
                    }
                """.trimIndent() else ""
                
                val jsLineNumbers = """
                    const codeBlock = document.querySelector('code');
                    if (codeBlock) {
                        const lines = codeBlock.innerHTML.split('\n');
                        codeBlock.innerHTML = lines.map((line, idx) => '<span id="line-' + (idx + 1) + '" class="hljs-line">' + line + '</span>').join('\n');
                    }
                    window.scrollToLine = function(lineNum) {
                        const el = document.getElementById('line-' + lineNum);
                        if (el) {
                            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            el.style.backgroundColor = 'rgba(229, 169, 60, 0.4)';
                            el.style.borderRadius = '4px';
                            setTimeout(() => { el.style.backgroundColor = ''; }, 2500);
                        }
                    };
                """.trimIndent()

                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                <style>
                  body { 
                    background-color: $bgColor; 
                    color: $textColor;
                    margin: 0; padding: 16px;
                    font-size: $fSize;
                  }
                  pre {
                    margin: 0;
                    white-space: $wWrap;
                    word-wrap: $wWord;
                    font-family: monospace;
                  }
                  pre, code { font-size: $fSize !important; line-height: 1.55; }
                  $lineNumbersCss
                </style>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/$theme">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                </head>
                <body>
                <pre><code class="language-${fileEntity.extension.lowercase()}">$escapedCode</code></pre>
                <script>
                  hljs.highlightAll();
                  $jsLineNumbers
                </script>
                </body>
                </html>
                """.trimIndent()
            }

            var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

            LaunchedEffect(scrollToLine) {
                if (scrollToLine != null) {
                    webViewRef?.evaluateJavascript("if (typeof window.scrollToLine === 'function') { window.scrollToLine($scrollToLine); }", null)
                    onScrollToLineHandled()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (webView.tag != htmlContent) {
                        webView.tag = htmlContent
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}

// 3. SPREADSHEET CSV SCREEN COMPONENT
@Composable
fun CsvPreview(
    csvRows: List<List<String>>,
    fileEntity: RecentFileEntity,
    viewModel: MainViewModel,
    editable: Boolean,
    notice: String?,
    onDiscardAndExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNightMode = androidx.compose.foundation.isSystemInDarkTheme()

    // Edits are held here and written only when the user taps Save. The old code overwrote the file on
    // every keystroke, which for .xlsx also flattened the workbook to one sheet of plain values.
    var pendingRows by remember { mutableStateOf<List<List<String>>?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    LaunchedEffect(csvRows) { pendingRows = null }

    androidx.activity.compose.BackHandler(enabled = pendingRows != null) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save your edits to ${fileEntity.name} before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRows?.let { viewModel.saveSpreadsheetFile(fileEntity, it) }
                    showDiscardDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        pendingRows = null
                        showDiscardDialog = false
                        onDiscardAndExit()
                    }) { Text("Discard") }
                }
            }
        )
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            val dirty = pendingRows
            if (!editable) {
                Text(
                    text = "View only",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            } else if (dirty != null) {
                TextButton(onClick = { viewModel.saveSpreadsheetFile(fileEntity, dirty) }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Edit Mode",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (notice != null) NoticeBanner(notice)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Optimized Compose Excel View with x-spreadsheet via WebView
        val base64Data = remember(csvRows) {
            val jsonArray = org.json.JSONArray()
            for (row in csvRows) {
                val rowArray = org.json.JSONArray()
                for (cell in row) {
                    rowArray.put(cell)
                }
                jsonArray.put(rowArray)
            }
            android.util.Base64.encodeToString(jsonArray.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        // The page and scripts come from file:///android_asset/, which is always readable.
                        allowFileAccess = false
                        allowContentAccess = false
                        domStorageEnabled = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                    }

                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onDataChanged(jsonStr: String) {
                            if (!editable) return
                            try {
                                val parser = org.json.JSONArray(jsonStr)
                                val newRows = mutableListOf<List<String>>()
                                for (i in 0 until parser.length()) {
                                    val rowArray = parser.getJSONArray(i)
                                    val row = mutableListOf<String>()
                                    for (j in 0 until rowArray.length()) {
                                        row.add(rowArray.getString(j))
                                    }
                                    newRows.add(row)
                                }
                                pendingRows = newRows
                            } catch (e: Exception) {
                                // Malformed payload from the page: keep the last good pending state.
                            }
                        }
                    }, "Android")

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            view?.evaluateJavascript("loadData('$base64Data');", null)
                        }
                    }

                    val bgColor = if (isNightMode) "#151514" else "#faf9f5"
                    val htmlContent = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                            <link rel="stylesheet" href="file:///android_asset/xspreadsheet/xspreadsheet.css">
                            <script src="file:///android_asset/xspreadsheet/xspreadsheet.js"></script>
                            <style>
                                body, html { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background-color: $bgColor; }
                                #x-spreadsheet-demo { height: 100%; width: 100%; }
                            </style>
                        </head>
                        <body>
                            <div id="x-spreadsheet-demo"></div>
                            <script>
                                var s = x_spreadsheet('#x-spreadsheet-demo', {
                                    mode: '${if (editable) "edit" else "read"}',
                                    showToolbar: ${if (editable) "true" else "false"},
                                    showGrid: true,
                                    view: {
                                        height: () => document.documentElement.clientHeight,
                                        width: () => document.documentElement.clientWidth,
                                    }
                                });
                                
                                function loadData(base64Str) {
                                    try {
                                        var jsonStr = decodeURIComponent(escape(window.atob(base64Str)));
                                        var rowsData = JSON.parse(jsonStr);
                                        var xRows = {};
                                        for(var i=0; i<rowsData.length; i++) {
                                            var row = rowsData[i];
                                            xRows[i] = { cells: {} };
                                            for(var j=0; j<row.length; j++) {
                                                xRows[i].cells[j] = { text: row[j] || "" };
                                            }
                                        }
                                        s.loadData([{
                                            name: 'Sheet1',
                                            rows: xRows
                                        }]);
                                        
                                        s.change(data => {
                                            var maxRow = 0;
                                            var maxCol = 0;
                                            for (var r in data.rows) {
                                                if (r === 'len') continue;
                                                var ri = parseInt(r);
                                                if (ri > maxRow) maxRow = ri;
                                                var row = data.rows[r];
                                                if (row && row.cells) {
                                                    for (var c in row.cells) {
                                                        var ci = parseInt(c);
                                                        if (ci > maxCol) maxCol = ci;
                                                    }
                                                }
                                            }
                                            var resultRows = [];
                                            for (var i = 0; i <= maxRow; i++) {
                                                var rowArr = [];
                                                for (var j = 0; j <= maxCol; j++) {
                                                    if (data.rows[i] && data.rows[i].cells && data.rows[i].cells[j]) {
                                                        rowArr.push(data.rows[i].cells[j].text || "");
                                                    } else {
                                                        rowArr.push("");
                                                    }
                                                }
                                                resultRows.push(rowArr);
                                            }
                                            Android.onDataChanged(JSON.stringify(resultRows));
                                        });
                                    } catch(e) {
                                        console.error(e);
                                    }
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                }
            }
        )
    }
}

// 4. TREE-LISTING ARCHIVE PREVIEW COMPONENT
@Composable
fun ZipPreview(
    zipRoot: FileManager.ZipNode,
    fileEntity: RecentFileEntity,
    expandedPaths: Set<String>,
    onToggleExpand: (String) -> Unit,
    onZipEntryClick: (FileManager.ZipNode, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep list state
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Automatically expand the root ZIP node if not expanded
    LaunchedEffect(zipRoot) {
        if (!expandedPaths.contains("")) {
            onToggleExpand("")
        }
    }

    Column(modifier = modifier) {
        // ZIP info row banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = fileEntity.extension.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Read-only",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nested Header card
            item(contentType = "ZipHeader") {
                ClaudeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFBF0EC),
                            modifier = Modifier.size(45.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = zipRoot.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val totalFilesCount = countTotalFiles(zipRoot)
                            Text(
                                text = "$totalFilesCount files • ${formatFileSize(fileEntity.size)} uncompressed",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recursive flat structure listing to handle animations correctly in LazyColumn
            val nodesList = mutableListOf<NodeWithDepth>()
            buildNodesList(zipRoot, 0, expandedPaths, nodesList)

            items(nodesList, key = { it.node.path + "_" + it.depth }, contentType = { "ZipNode" }) { (node, depth) ->
                val isExpanded = expandedPaths.contains(node.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                        .clickable {
                            if (node.isDirectory) {
                                onToggleExpand(node.path)
                            } else {
                                onZipEntryClick(node, "")
                            }
                        }
                        .padding(start = (depth * 20).dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = node.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp)) // indentation padding
                        Icon(
                            imageVector = Icons.Outlined.InsertDriveFile,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = node.name,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatFileSize(node.size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NodeWithDepth(val node: FileManager.ZipNode, val depth: Int)

fun countTotalFiles(root: FileManager.ZipNode): Int {
    var count = 0
    fun traverse(node: FileManager.ZipNode) {
        if (!node.isDirectory) {
            count++
        }
        node.children.forEach { traverse(it) }
    }
    traverse(root)
    return count
}

fun buildNodesList(
    node: FileManager.ZipNode,
    currentDepth: Int,
    expandedPaths: Set<String>,
    result: MutableList<NodeWithDepth>
) {
    // We omit the root node itself since it is already rendered in the beautiful header card!
    if (node.path.isNotEmpty()) {
        result.add(NodeWithDepth(node, currentDepth))
    }

    val isExpanded = expandedPaths.contains(node.path)
    if (isExpanded || node.path.isEmpty()) {
        node.children.forEach { child ->
            buildNodesList(child, if (node.path.isEmpty()) 0 else currentDepth + 1, expandedPaths, result)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImagePreview(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val parentDir = remember(fileEntity) { java.io.File(fileEntity.path).parentFile }
    val imageFiles = remember(parentDir) {
        parentDir?.listFiles()?.filter { 
            val ext = it.extension.lowercase()
            ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
        }?.sortedBy { it.name } ?: listOf(java.io.File(fileEntity.path))
    }
    
    val initialIndex = remember(imageFiles, fileEntity) {
        val idx = imageFiles.indexOfFirst { it.absolutePath == fileEntity.path }
        if (idx == -1) 0 else idx
    }
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageFiles.size }
    )
    
    val currentFile = imageFiles.getOrNull(pagerState.currentPage) ?: java.io.File(fileEntity.path)
    
    // The history save requirement "history me sirf folder save kro or path me last previewed pictures jis per click krne per us folder ki last dekhi hue image lload ho"
    // Since MainViewModel automatically saves the file when openFile is called, we don't need to save every swipe to the history,
    // but the user says "path me last previewed pictures jis per click krne per us folder ki last dekhi hue image lload ho"
    // So maybe we can trigger a history update when the page settles, updating the same DB record? 
    // For now, let's just show the images. We can use a LaunchedEffect to update history silently if needed.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != initialIndex) {
            val dao = com.example.data.AppDatabase.getDatabase(context).recentFileDao()
            val newFile = imageFiles[pagerState.currentPage]
            val existing = dao.getRecentFileByPath(fileEntity.path)
            if (existing != null) {
                dao.deleteRecentFileByPath(existing.path)
                dao.insertRecentFile(existing.copy(
                    path = newFile.absolutePath, 
                    name = newFile.name, 
                    extension = newFile.extension.lowercase(), 
                    size = newFile.length(),
                    lastOpened = System.currentTimeMillis(),
                    id = 0 // Auto-generate new id
                ))
            } else {
                dao.insertRecentFile(RecentFileEntity(
                    path = newFile.absolutePath,
                    name = newFile.name,
                    extension = newFile.extension.lowercase(),
                    size = newFile.length(),
                    lastOpened = System.currentTimeMillis()
                ))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val file = imageFiles[page]
            val zoomSpec = remember {
                me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 15f)
            }
            val zoomableState = me.saket.telephoto.zoomable.rememberZoomableState(zoomSpec = zoomSpec)
            val zoomableImageState = me.saket.telephoto.zoomable.rememberZoomableImageState(zoomableState = zoomableState)

            me.saket.telephoto.zoomable.coil.ZoomableAsyncImage(
                model = file,
                contentDescription = "Image preview",
                modifier = Modifier.fillMaxSize(),
                state = zoomableImageState,
                onClick = { onTap() }
            )
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PdfPreview(
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    scrollToPage: Int?,
    onScrollToPageHandled: () -> Unit,
    onLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit = {},
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val originalFile = remember(fileEntity) { java.io.File(fileEntity.path) }
    var isReady by remember { mutableStateOf(false) }
    var requirePassword by remember { mutableStateOf(false) }
    var pdfPassword by remember { mutableStateOf("") }
    var submittedPassword by remember { mutableStateOf<String?>(null) }
    var loadTrigger by remember { mutableStateOf(0) }
    var passwordError by remember { mutableStateOf(false) }
    var pdfErrorMessage by remember { mutableStateOf<String?>(null) }

    if (requirePassword) {
        AlertDialog(
            onDismissRequest = { onBackClick() },
            title = { Text("Password Required") },
            text = {
                Column {
                    Text("This PDF is password protected.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pdfPassword,
                        onValueChange = { pdfPassword = it; passwordError = false },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = passwordError,
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError) {
                        Text("Incorrect password", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    submittedPassword = pdfPassword
                    requirePassword = false
                    loadTrigger++ // retry loading
                }) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { onBackClick() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF181715) else Color(0xFFF4F4F9))
    ) {
        if (pdfErrorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "PDF Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Could not open PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pdfErrorMessage ?: "The PDF file appears corrupted or has an invalid structure.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBackClick) {
                    Text("Go Back")
                }
            }
        } else {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    com.github.barteksc.pdfviewer.PDFView(context, null).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        minZoom = 1.0f
                        midZoom = 3.5f
                        maxZoom = 10.0f
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { pdfView ->
                    pdfView.minZoom = 1.0f
                    val bookmarkPrefs = pdfView.context.getSharedPreferences("pdf_bookmarks", android.content.Context.MODE_PRIVATE)
                    val bookmarkKey = "bookmark_${fileEntity.path.hashCode()}"
                    val savedPage = bookmarkPrefs.getInt(bookmarkKey, 0)

                    if ((!isReady && originalFile.exists()) || loadTrigger > 0) {
                        val initialPage = scrollToPage ?: savedPage
                        val configurator = pdfView.fromFile(originalFile)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .defaultPage(initialPage)
                            .onPageChange { page, pageCount ->
                                bookmarkPrefs.edit().putInt(bookmarkKey, page).apply()
                                onPageChanged(page + 1, pageCount)
                            }
                            .enableAnnotationRendering(true)
                            .password(submittedPassword)
                            .scrollHandle(com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle(pdfView.context))
                            .enableAntialiasing(true)
                            .spacing(0)
                            .autoSpacing(false)
                            .pageFitPolicy(com.github.barteksc.pdfviewer.util.FitPolicy.WIDTH)
                            .nightMode(false)
                            .onLoad {
                                isReady = true
                                requirePassword = false
                                passwordError = false
                                loadTrigger = 0
                                pdfErrorMessage = null
                                applySmartPdfNightMode(pdfView, isNightMode)
                                if (savedPage > 0 && scrollToPage == null) {
                                    android.widget.Toast.makeText(pdfView.context, "Resumed at page ${savedPage + 1}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                if (scrollToPage != null) {
                                    onScrollToPageHandled()
                                }
                            }
                            .onError { t ->
                                if (t is com.shockwave.pdfium.PdfPasswordException || t.message?.contains("Password required") == true) {
                                    requirePassword = true
                                    if (!submittedPassword.isNullOrEmpty()) {
                                        passwordError = true
                                    }
                                } else {
                                    pdfErrorMessage = t.localizedMessage ?: "This PDF file is corrupted or cannot be read."
                                    t.printStackTrace()
                                }
                            }
                            .onTap {
                                onSingleTap()
                                true
                            }
                            
                        configurator.load()
                        if (loadTrigger > 0) loadTrigger = 0
                    } else if (isReady && scrollToPage != null) {
                        pdfView.jumpTo(scrollToPage, true)
                        onScrollToPageHandled()
                    }
                    
                    if (isReady) {
                        applySmartPdfNightMode(pdfView, isNightMode)
                    }
                }
            )
            
            if (!isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.component.PremiumLoadingIndicator(text = "Loading PDF...")
                }
            }
        }
    }
}

/**
 * Smart hue-preserving dark mode for PDFView:
 * Inverts white paper to dark charcoal (#1e1e1e) and black text to off-white (#e1e1e1),
 * while preserving natural skin tones and photographic colors so photos don't look like negative ghosts.
 */
private fun applySmartPdfNightMode(pdfView: com.github.barteksc.pdfviewer.PDFView, isNight: Boolean) {
    try {
        pdfView.setNightMode(false)
        val paintField = com.github.barteksc.pdfviewer.PDFView::class.java.getDeclaredField("paint")
        paintField.isAccessible = true
        val paint = paintField.get(pdfView) as? android.graphics.Paint
        if (paint != null) {
            paint.colorFilter = if (isNight) {
                val matrix = floatArrayOf(
                     0.25f, -0.70f, -0.315f, 0f, 225f,
                    -0.35f,  0.05f, -0.465f, 0f, 225f,
                    -0.45f, -0.45f,  0.135f, 0f, 225f,
                     0.00f,  0.00f,  0.000f, 1f,   0f
                )
                android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(matrix))
            } else null
            pdfView.invalidate()
        }
    } catch (e: Exception) {
        pdfView.setNightMode(isNight)
    }
}

@Composable
fun AudioPlayer(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }

    LaunchedEffect(fileEntity) {
        try {
            val mp = MediaPlayer().apply {
                setDataSource(fileEntity.path)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            mediaPlayer = mp
            duration = mp.duration
            
            while (true) {
                val mpActive = mediaPlayer
                if (mpActive != null) {
                    try {
                        if (mpActive.isPlaying) {
                            currentPos = mpActive.currentPosition
                        }
                    } catch (e: Exception) {
                        break
                    }
                } else {
                    break
                }
                delay(500)
            }
        } catch (e: Exception) {
            // handle error
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (ignored: Exception) {}
                release()
            }
            mediaPlayer = null
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = fileEntity.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar / Slider
        Slider(
            value = currentPos.toFloat(),
            onValueChange = { newVal ->
                mediaPlayer?.seekTo(newVal.toInt())
                currentPos = newVal.toInt()
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPos), fontSize = 12.sp, color = Color.Gray)
            Text(text = formatTime(duration), fontSize = 12.sp, color = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Play / Pause FAB
        FloatingActionButton(
            onClick = {
                val mp = mediaPlayer ?: return@FloatingActionButton
                try {
                    if (isPlaying) {
                        mp.pause()
                        isPlaying = false
                    } else {
                        mp.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause"
            )
        }
    }
}

@Composable
fun VideoPlayer(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = fileEntity.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoPath(fileEntity.path)
                    val controller = android.widget.MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    start()
                }
            },
            modifier = Modifier.fillMaxWidth().height(260.dp)
        )
    }
}

@Composable
fun HexViewer(
    hexRows: List<String>,
    asciiRows: List<String>,
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = "BINARY / HEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${formatFileSize(fileEntity.size)} • Modified ${formatElapsedTime(fileEntity.lastOpened)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E)) // Dark code editor styled background for hex contrast
                .padding(8.dp)
        ) {
            items(hexRows.size, key = { it }, contentType = { "HexRow" }) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).animateItemPlacement(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hexRows[index],
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFD4D4D4)
                    )
                    Text(
                        text = "| ${if (index < asciiRows.size) asciiRows[index] else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF9CDCFE),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun shareFile(context: android.content.Context, file: File, extension: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            "png", "jpg", "jpeg", "webp" -> "image/*"
            "mp3", "wav", "m4a" -> "audio/*"
            "mp4", "mkv" -> "video/*"
            else -> "text/plain"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share File"))
    } catch (e: Exception) {
        e.printStackTrace()
        // Reliable fallback sharing flow using plain file path text
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "File Path: ${file.absolutePath}")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share File Path"))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

@Composable
fun WebViewPreview(
    fileEntity: RecentFileEntity,
    modifier: Modifier = Modifier,
    allowScripts: Boolean = false,
    searchQuery: String = "",
    onSingleTap: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx
    val bgColor = if (isDark) com.example.ui.theme.ClaudeOnyx else Color.White

    var base64Data by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileEntity) {
        if (fileEntity.extension.lowercase() == "pdf") {
            withContext(Dispatchers.IO) {
                try {
                    val file = java.io.File(fileEntity.path)
                    val bytes = file.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    base64Data = base64
                } catch (e: Exception) {
                    loadError = e.message
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val context = LocalContext.current
        val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

        if (fileEntity.extension.lowercase() == "pdf" && base64Data == null && loadError == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.example.ui.component.PremiumLoadingIndicator(text = "Loading PDF...")
            }
        } else if (loadError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $loadError", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        // HTML files are untrusted: scripts start off (see allowScripts), no bridge object is
                        // exposed to them, and navigation away from local files is blocked.
                        val htmlPage = fileEntity.extension.lowercase().let { it == "html" || it == "htm" }
                        settings.javaScriptEnabled = !htmlPage
                        settings.domStorageEnabled = !htmlPage
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        // file:///android_asset is always readable; real file access is only for local HTML.
                        settings.allowFileAccess = htmlPage
                        settings.allowContentAccess = false

                        if (!htmlPage) {
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun performSingleTap() {
                                    mainHandler.post {
                                        onSingleTap()
                                    }
                                }
                            }, "Android")
                        }

                        setBackgroundColor(if (isDark) 0xFF181715.toInt() else 0xFFFAF9F5.toInt())
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView,
                                request: android.webkit.WebResourceRequest
                            ): Boolean = request.url.scheme != "file"
                        }
                        webChromeClient = android.webkit.WebChromeClient()
                    }
                },
                update = { webView ->
                    if (searchQuery.isNotEmpty()) {
                        webView.findAllAsync(searchQuery)
                    } else {
                        webView.clearMatches()
                    }
                    val ext = fileEntity.extension.lowercase()
                    if (ext == "html" || ext == "htm") {
                        loadLocalFileOnce(webView, fileEntity.path, allowScripts)
                    } else if (ext == "pdf") {
                        try {
                            val base64Pdf = base64Data ?: ""
                            val htmlContent = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                                    <script src="file:///android_asset/pdfjs/build/pdf.js"></script>
                                    <style>
                                        body {
                                            margin: 0;
                                            padding: 12px;
                                            background-color: ${if (isDark) "#181715" else "#FAF9F5"};
                                            display: flex;
                                            flex-direction: column;
                                            align-items: center;
                                            font-family: -apple-system, sans-serif;
                                            user-select: text !important;
                                            -webkit-user-select: text !important;
                                        }
                                        .page-container {
                                            position: relative;
                                            margin-bottom: 20px;
                                            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                                            background-color: white;
                                            border-radius: 6px;
                                            overflow: hidden;
                                            max-width: 100%;
                                            display: flex;
                                            justify-content: center;
                                        }
                                        canvas {
                                            display: block;
                                            max-width: 100%;
                                            height: auto !important;
                                        }
                                        .text-layer {
                                            position: absolute;
                                            left: 0;
                                            top: 0;
                                            right: 0;
                                            bottom: 0;
                                            overflow: hidden;
                                            background: transparent;
                                            pointer-events: auto;
                                        }
                                        .text-layer > span {
                                            color: rgba(0,0,0,0);
                                            position: absolute;
                                            white-space: pre;
                                            cursor: text;
                                            transform-origin: 0% 0%;
                                            pointer-events: auto;
                                            user-select: text !important;
                                            -webkit-user-select: text !important;
                                        }
                                        #loading {
                                            margin-top: 40px;
                                            color: ${if (isDark) "#CCCCCC" else "#333333"};
                                            font-size: 15px;
                                            font-weight: 500;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div id="loading">Preparing PDF Browser Preview...</div>
                                    <div id="viewer"></div>
    
                                    <script>
                                        function transformMatrix(m1, m2) {
                                            return [
                                                m1[0] * m2[0] + m1[2] * m2[1],
                                                m1[1] * m2[0] + m1[3] * m2[1],
                                                m1[0] * m2[2] + m1[2] * m2[3],
                                                m1[1] * m2[2] + m1[3] * m2[3],
                                                m1[0] * m2[4] + m1[2] * m2[5] + m1[4],
                                                m1[1] * m2[4] + m1[3] * m2[5] + m1[5]
                                            ];
                                        }
    
                                        function base64ToUint8Array(base64) {
                                            var raw = window.atob(base64);
                                            var rawLength = raw.length;
                                            var array = new Uint8Array(new ArrayBuffer(rawLength));
                                            for(var i = 0; i < rawLength; i++) {
                                                array[i] = raw.charCodeAt(i);
                                            }
                                            return array;
                                        }
    
                                        document.addEventListener('click', function(e) {
                                            var selection = window.getSelection().toString();
                                            if (!selection) {
                                                if (typeof Android !== 'undefined' && Android.performSingleTap) {
                                                    Android.performSingleTap();
                                                }
                                            }
                                        });
    
                                        var pdfData = base64ToUint8Array("$base64Pdf");
                                        pdfjsLib.GlobalWorkerOptions.workerSrc = 'file:///android_asset/pdfjs/build/pdf.worker.js';
    
                                        var loadingTask = pdfjsLib.getDocument({data: pdfData});
                                        loadingTask.promise.then(function(pdf) {
                                            var loadingEl = document.getElementById('loading');
                                            if (loadingEl) loadingEl.style.display = 'none';
                                            var viewer = document.getElementById('viewer');
                                            for (var pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                                                renderPage(pdf, pageNum, viewer);
                                            }
                                        }, function (reason) {
                                            document.getElementById('loading').innerText = 'Error loading PDF: ' + reason.message;
                                        });
    
                                        function renderPage(pdf, pageNum, container) {
                                            pdf.getPage(pageNum).then(function(page) {
                                                var scale = 1.5;
                                                var viewport = page.getViewport({scale: scale});
    
                                                var pageDiv = document.createElement('div');
                                                pageDiv.className = 'page-container';
                                                pageDiv.style.width = viewport.width + 'px';
                                                pageDiv.style.height = viewport.height + 'px';
                                                
                                                var canvas = document.createElement('canvas');
                                                var context = canvas.getContext('2d');
                                                canvas.height = viewport.height;
                                                canvas.width = viewport.width;
    
                                                pageDiv.appendChild(canvas);
                                                container.appendChild(pageDiv);
    
                                                var renderContext = {
                                                    canvasContext: context,
                                                    viewport: viewport
                                                };
                                                
                                                var renderTask = page.render(renderContext);
                                                renderTask.promise.then(function() {
                                                    return page.getTextContent();
                                                }).then(function(textContent) {
                                                    var textLayerDiv = document.createElement('div');
                                                    textLayerDiv.className = 'text-layer';
                                                    pageDiv.appendChild(textLayerDiv);
                                                    
                                                    textContent.items.forEach(function(item) {
                                                        var tx = transformMatrix(
                                                            viewport.transform,
                                                            item.transform
                                                        );
                                                        
                                                        var span = document.createElement('span');
                                                        span.textContent = item.str;
                                                        span.style.fontFamily = item.fontName;
                                                        var fontHeight = item.height * scale;
                                                        span.style.fontSize = fontHeight + 'px';
                                                        
                                                        var textWidth = item.width * scale;
                                                        span.style.width = textWidth + 'px';
                                                        
                                                        span.style.left = tx[4] + 'px';
                                                        span.style.top = (tx[5] - fontHeight) + 'px';
                                                        
                                                        textLayerDiv.appendChild(span);
                                                    });
                                                });
                                            });
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                        } catch (e: Exception) {
                            webView.loadDataWithBaseURL(null, "<html><body><h3>Error: ${e.message}</h3></body></html>", "text/html", "UTF-8", null)
                        }
                    } else {
                    try {
                        val content = java.io.File(fileEntity.path).readText()
                        val htmlContent = if (ext == "md") {
                            """
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                            <style>
                                body {
                                    background-color: ${if (isDark) "#181715" else "#FAF9F5"};
                                    color: ${if (isDark) "#FAF9F5" else "#141413"};
                                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                                    padding: 16px;
                                    line-height: 1.6;
                                    margin: 0;
                                    -webkit-text-size-adjust: none;
                                }
                                h1, h2, h3, h4, h5, h6 {
                                    color: ${if (isDark) "#E8A55A" else "#CC785C"};
                                    margin-top: 24px;
                                    margin-bottom: 12px;
                                }
                                pre {
                                    background-color: ${if (isDark) "#252320" else "#EFE9DE"};
                                    padding: 12px;
                                    border-radius: 6px;
                                    overflow-x: auto;
                                    font-family: ui-monospace, monospace;
                                    font-size: 13px;
                                }
                                code {
                                    background-color: ${if (isDark) "#252320" else "#EFE9DE"};
                                    padding: 2px 4px;
                                    border-radius: 4px;
                                    font-family: ui-monospace, monospace;
                                }
                                blockquote {
                                    border-left: 4px solid ${if (isDark) "#E8A55A" else "#CC785C"};
                                    margin: 0;
                                    padding-left: 16px;
                                    color: ${if (isDark) "#A09D96" else "#6C6A64"};
                                    font-style: italic;
                                }
                                table {
                                    border-collapse: collapse;
                                    width: 100%;
                                    margin: 16px 0;
                                }
                                th, td {
                                    border: 1px solid ${if (isDark) "#2D2C2A" else "#E6DFD8"};
                                    padding: 8px;
                                    text-align: left;
                                }
                                th {
                                    background-color: ${if (isDark) "#252320" else "#EFE9DE"};
                                }
                                ul, ol {
                                    padding-left: 20px;
                                }
                            </style>
                            </head>
                            <body>
                                ${formatSimpleMarkdownToHtml(content)}
                            </body>
                            </html>
                            """.trimIndent()
                        } else {
                            """
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes">
                            <style>
                                body {
                                    background-color: ${if (isDark) "#181715" else "#FAF9F5"};
                                    color: ${if (isDark) "#FAF9F5" else "#141413"};
                                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
                                    padding: 16px;
                                    white-space: pre-wrap;
                                    word-wrap: break-word;
                                    line-height: 1.6;
                                    font-size: 14px;
                                    margin: 0;
                                    -webkit-text-size-adjust: none;
                                }
                            </style>
                            </head>
                            <body>${escapeHtml(content)}</body>
                            </html>
                            """.trimIndent()
                        }
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    } catch (e: Exception) {
                        loadLocalFileOnce(webView, fileEntity.path, allowScripts)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        }
    }
}

/** Loads a local file only when the file or script setting changes, instead of on every recomposition. */
private fun loadLocalFileOnce(webView: android.webkit.WebView, path: String, allowScripts: Boolean) {
    webView.settings.javaScriptEnabled = allowScripts
    val url = android.net.Uri.fromFile(java.io.File(path)).toString()
    val key = "$url|$allowScripts"
    if (webView.tag != key) {
        webView.tag = key
        webView.loadUrl(url)
    }
}

fun formatSimpleMarkdownToHtml(mdText: String): String {
    val lines = mdText.lines()
    val htmlBuilder = StringBuilder()
    var inList = false
    var inCodeBlock = false
    val codeBlockContent = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                htmlBuilder.append("<pre><code>")
                htmlBuilder.append(escapeHtml(codeBlockContent.toString()))
                htmlBuilder.append("</code></pre>\n")
                codeBlockContent.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockContent.append(line).append("\n")
            continue
        }

        val trimmed = line.trim()
        
        // Handle list items
        val listStart = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")
        if (listStart) {
            if (!inList) {
                htmlBuilder.append("<ul>\n")
                inList = true
            }
            val content = trimmed.substring(2)
            htmlBuilder.append("<li>").append(formatInlineMarkdownToHtml(content)).append("</li>\n")
            continue
        } else {
            if (inList) {
                htmlBuilder.append("</ul>\n")
                inList = false
            }
        }

        // Handle headings
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmed.getOrNull(level) == ' ') {
                val content = trimmed.substring(level + 1)
                htmlBuilder.append("<h$level>").append(formatInlineMarkdownToHtml(content)).append("</h$level>\n")
                continue
            }
        }

        // Handle horizontal rules
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            htmlBuilder.append("<hr/>\n")
            continue
        }

        // Handle blockquotes
        if (trimmed.startsWith(">")) {
            val content = trimmed.substring(1).trim()
            htmlBuilder.append("<blockquote>").append(formatInlineMarkdownToHtml(content)).append("</blockquote>\n")
            continue
        }

        // Empty lines
        if (trimmed.isEmpty()) {
            continue
        }

        // Default to paragraph
        htmlBuilder.append("<p>").append(formatInlineMarkdownToHtml(line)).append("</p>\n")
    }

    if (inList) {
        htmlBuilder.append("</ul>\n")
    }
    if (inCodeBlock) {
        htmlBuilder.append("<pre><code>")
        htmlBuilder.append(escapeHtml(codeBlockContent.toString()))
        htmlBuilder.append("</code></pre>\n")
    }

    return htmlBuilder.toString()
}

fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

fun formatInlineMarkdownToHtml(text: String): String {
    var formatted = escapeHtml(text)
    
    // Bold: **text** or __text__
    val boldRegex = """\*\*(.*?)\*\*""".toRegex()
    formatted = boldRegex.replace(formatted) { "<strong>${it.groupValues[1]}</strong>" }
    
    // Italic: *text* or _text_
    val italicRegex = """\*(.*?)\*""".toRegex()
    formatted = italicRegex.replace(formatted) { "<em>${it.groupValues[1]}</em>" }
    
    // Inline code: `code`
    val codeRegex = """`(.*?)`""".toRegex()
    formatted = codeRegex.replace(formatted) { "<code>${it.groupValues[1]}</code>" }
    
    return formatted
}

private fun openPdfInBrowser(context: android.content.Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Open with Browser / Viewer"))
    } catch (e: Exception) {
         try {
             val uri = androidx.core.content.FileProvider.getUriForFile(
                 context,
                 "${context.packageName}.fileprovider",
                 file
             )
             val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                 setDataAndType(uri, "text/html")
                 addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                 addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
             }
             context.startActivity(android.content.Intent.createChooser(intent, "Open in Browser"))
         } catch (ex: Exception) {
             android.widget.Toast.makeText(context, "No app available to open PDF: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
         }
    }
}


@Composable
fun DocxPreviewWebView(
    base64Docx: String,
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    scrollToPage: Int? = null,
    onScrollProgressHandled: () -> Unit = {},
    scrollToHeadingIndex: Int? = null,
    scrollToHeadingTitle: String? = null,
    onOutlineJumpHandled: () -> Unit = {}
) {
    var isWebViewLoading by remember(base64Docx) { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var pureKotlinFallback by remember { mutableStateOf<com.example.core.format.DocxExtractor.ExtractedDocx?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(scrollToPage, webViewRef) {
        if (scrollToPage != null && webViewRef != null) {
            val pageNum = scrollToPage + 1
            webViewRef?.evaluateJavascript("if (typeof window.scrollToPage === 'function') { window.scrollToPage($pageNum); }", null)
            onScrollProgressHandled()
        }
    }
    LaunchedEffect(scrollToHeadingIndex, scrollToHeadingTitle, webViewRef, isWebViewLoading) {
        if (scrollToHeadingIndex != null && webViewRef != null && !isWebViewLoading) {
            val escapedTitle = org.json.JSONObject.quote(scrollToHeadingTitle.orEmpty())
            webViewRef?.evaluateJavascript(
                "if (typeof window.scrollToHeading === 'function') { window.scrollToHeading($scrollToHeadingIndex, $escapedTitle); }",
                null
            )
            onOutlineJumpHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        if (pureKotlinFallback != null) {
            // High-fidelity Pure Kotlin Offline Text Viewer Fallback
            val extracted = pureKotlinFallback!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Clean Document View • ${extracted.wordCount} words • ${extracted.paragraphCount} paragraphs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (extracted.paragraphs.isEmpty()) {
                    Text(
                        text = "No text content found in this DOCX file.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    for (paragraph in extracted.paragraphs) {
                        Text(
                            text = paragraph,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewRef = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
                        
                        addJavascriptInterface(object : Any() {
                            @android.webkit.JavascriptInterface
                            fun getDocxBase64(): String = base64Docx

                            @android.webkit.JavascriptInterface
                            fun onDocumentLoaded(totalPages: Int) {
                                post { 
                                    isWebViewLoading = false
                                    onPageChanged(1, totalPages)
                                }
                            }
                            @android.webkit.JavascriptInterface
                            fun onPageChanged(currentPage: Int, totalPages: Int) {
                                post { 
                                    onPageChanged(currentPage, totalPages)
                                }
                            }
                            @android.webkit.JavascriptInterface
                            fun onDocumentError(error: String) {
                                post { 
                                    isWebViewLoading = false
                                    android.util.Log.w("WebViewDocx", "Switching to pure Kotlin DOCX reader: $error")
                                    val ext = com.example.core.format.DocxExtractor.extractText(java.io.File(fileEntity.path))
                                    if (ext.paragraphs.isNotEmpty()) {
                                        pureKotlinFallback = ext
                                    } else {
                                        android.widget.Toast.makeText(context, "DOCX Notice: $error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            @android.webkit.JavascriptInterface
                            fun exitWithoutSaving() {
                                post { 
                                    viewModel.closeCurrentFile()
                                }
                            }
                        }, "Android")
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                view?.evaluateJavascript("loadDocx(window.Android.getDocxBase64(), $isNightMode);", null)
                            }
                            override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                android.util.Log.e("WebViewDocx", "Error: ${error?.description}")
                                val ext = com.example.core.format.DocxExtractor.extractText(java.io.File(fileEntity.path))
                                if (ext.paragraphs.isNotEmpty()) {
                                    post { pureKotlinFallback = ext; isWebViewLoading = false }
                                }
                            }
                            override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                android.util.Log.e("WebViewDocx", "HTTP Error: ${errorResponse?.statusCode}")
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                android.util.Log.d("WebViewDocx", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }
                        loadUrl("file:///android_asset/editor/docx_editor.html")
                    }
                },
                update = { webView ->
                    // Keep appearance changes in sync without rebuilding the document.
                    webView.evaluateJavascript(
                        "document.body.classList.toggle('dark', $isNightMode); " +
                            "document.querySelectorAll('.docx').forEach(function(page) { " +
                            "page.style.backgroundColor = " + (if (isNightMode) "'#1e1e1d'" else "'#ffffff'") + "; " +
                            "page.style.color = " + (if (isNightMode) "'#f5f5f3'" else "'#141413'") + "; });",
                        null
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isWebViewLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.component.PremiumLoadingIndicator(text = "Rendering Document...")
                }
            }
        }
    }
}

@Composable
fun PptxPreviewWebView(
    base64Pptx: String,
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isWebViewLoading by remember(base64Pptx) { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
                    
                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onDocumentLoaded(totalPages: Int) {
                            post { 
                                isWebViewLoading = false
                            }
                        }
                        @android.webkit.JavascriptInterface
                        fun onDocumentError(error: String) {
                            post { 
                                isWebViewLoading = false
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        @android.webkit.JavascriptInterface
                        fun exitWithoutSaving() {
                            post {
                                viewModel.closeCurrentFile()
                            }
                        }
                    }, "Android")
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            view?.evaluateJavascript("loadPptx('$base64Pptx', $isNightMode);", null)
                        }
                        override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("WebViewPptx", "Error: ${error?.description}")
                        }
                        override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            android.util.Log.e("WebViewPptx", "HTTP Error: ${errorResponse?.statusCode}")
                        }
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("WebViewPptx", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }
                    loadUrl("file:///android_asset/editor/pptx_editor.html")
                }
            },
            update = { webView ->
                // No-op
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isWebViewLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.component.PremiumLoadingIndicator(text = "Loading Presentation...")
            }
        }
    }
}

