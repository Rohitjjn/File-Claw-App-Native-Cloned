package com.example.ui.screens

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.component.DocumentOutlineBottomSheet
import com.example.ui.component.AppearanceBottomSheet
import com.example.ui.component.FilePropertiesBottomSheet
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
import com.example.core.io.TextLoader
import com.example.core.io.TextWindowReader
import com.example.core.text.TextFileSearchEngine
import com.example.core.text.TextFileSearchMatch
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode

import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation

private const val MAX_TEXT_PRINT_CHARS = 2_000_000

private suspend fun readBoundedText(file: File, charsetName: String): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val loaded = TextLoader.load(file, charsetName, maxChars = MAX_TEXT_PRINT_CHARS)
            check(!loaded.truncated) { "This file exceeds the bounded $MAX_TEXT_PRINT_CHARS-character limit for this operation." }
            loaded.text
        }
    }

private fun copyTextToClipboard(context: android.content.Context, title: String, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(title, text))
    android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
}

private fun showTextOperationError(context: android.content.Context, error: Throwable) {
    android.widget.Toast.makeText(
        context,
        error.message ?: "Could not prepare the complete file",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

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
    val isDarkTheme = MaterialTheme.colorScheme.background == com.example.ui.theme.ClaudeOnyx

    var isBarsVisible by remember { mutableStateOf(true) }

    val isHtmlFile = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm")
    }

    var isWebRenderActive by remember(activeFile) {
        mutableStateOf(true)
    }

    // HTML files are untrusted input: scripts stay off until the user opts in for this file.
    var htmlScriptsEnabled by remember(activeFile) { mutableStateOf(false) }

    val canBeRenderedInWeb = remember(activeFile) {
        activeFile?.extension?.lowercase() in setOf("html", "htm", "js", "ts", "css", "xml", "txt", "json", "md")
    }
    
    var currentPdfPage by remember { mutableStateOf(1) }
    var totalPdfPages by remember { mutableStateOf(0) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    var isPdfNightMode by remember(activeFile?.path) { mutableStateOf(isDarkTheme) }
    var scrollToPage by remember { mutableStateOf<Int?>(null) }
    var pendingLinkToOpen by remember { mutableStateOf<String?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var showDocxZoomDialog by remember { mutableStateOf(false) }
    var scrollToLineNumber by remember { mutableStateOf<Int?>(null) }
    var showOutlineBottomSheet by remember { mutableStateOf(false) }
    var showAppearanceBottomSheet by remember { mutableStateOf(false) }
    var docxZoomFactor by remember(activeFile?.path) { mutableFloatStateOf(1.0f) }
    var docxZoomInput by remember(activeFile?.path) { mutableStateOf("100") }
    var searchTriggerNext by remember { mutableIntStateOf(0) }
    var searchTriggerPrev by remember { mutableIntStateOf(0) }

    var isSearchInDocumentOpen by rememberSaveable { mutableStateOf(false) }
    var inViewerSearchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var totalMatchesCount by remember { mutableIntStateOf(0) }
    var searchResultsTruncated by remember { mutableStateOf(false) }
    var isPdfSearchLoading by remember { mutableStateOf(false) }
    var pdfSearchError by remember { mutableStateOf<String?>(null) }
    var pdfSearchSnapshot by remember { mutableStateOf(PdfSearchSnapshot()) }
    var docxPreviewWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var docxPreviewReady by remember(activeFile?.path) { mutableStateOf(false) }
    var fullPreviewText by remember(activeFile?.path) { mutableStateOf<String?>(null) }
    var largeTextSearchMatches by remember(activeFile?.path) { mutableStateOf<List<TextFileSearchMatch>>(emptyList()) }
    var largeTextSearchCompletedQuery by remember(activeFile?.path) { mutableStateOf("") }
    var isLargeTextSearchLoading by remember(activeFile?.path) { mutableStateOf(false) }
    var largeTextSearchError by remember(activeFile?.path) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeFile?.path) {
        isBarsVisible = true
        inViewerSearchQuery = ""
        isSearchInDocumentOpen = false
        currentMatchIndex = 0
        totalMatchesCount = 0
        searchResultsTruncated = false
        isPdfSearchLoading = false
        pdfSearchError = null
        pdfSearchSnapshot = PdfSearchSnapshot()
        docxPreviewReady = false
        docxPreviewWebView = null
        largeTextSearchMatches = emptyList()
        largeTextSearchCompletedQuery = ""
        isLargeTextSearchLoading = false
        largeTextSearchError = null
        fullPreviewText = null
        searchTriggerNext = 0
        searchTriggerPrev = 0
    }

    val context = LocalContext.current
    val printScope = rememberCoroutineScope()

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

    val isLargeTextSearchable = activeFile?.extension?.lowercase() in setOf("txt", "md") &&
        (fileState as? MainViewModel.FileContentState.TextSuccess)?.truncated == true

    LaunchedEffect(activeFile?.path, fileState) {
        val textState = fileState as? MainViewModel.FileContentState.TextSuccess
        val extension = activeFile?.extension?.lowercase()
        fullPreviewText = if (textState != null && extension in setOf("txt", "md")) textState.content else null
    }

    LaunchedEffect(activeFile?.path, inViewerSearchQuery, isLargeTextSearchable, fileState) {
        val textState = fileState as? MainViewModel.FileContentState.TextSuccess
        if (!isLargeTextSearchable || textState == null) {
            largeTextSearchMatches = emptyList()
            largeTextSearchCompletedQuery = ""
            isLargeTextSearchLoading = false
            largeTextSearchError = null
            return@LaunchedEffect
        }

        val query = inViewerSearchQuery.trim()
        if (query.isEmpty()) {
            largeTextSearchMatches = emptyList()
            largeTextSearchCompletedQuery = ""
            isLargeTextSearchLoading = false
            largeTextSearchError = null
            fullPreviewText = textState.content
            return@LaunchedEffect
        }

        largeTextSearchMatches = emptyList()
        largeTextSearchCompletedQuery = ""
        largeTextSearchError = null
        isLargeTextSearchLoading = true
        fullPreviewText = textState.content
        kotlinx.coroutines.delay(180)
        val searchContext = kotlin.coroutines.coroutineContext
        try {
            val charset = java.nio.charset.Charset.forName(textState.charsetName)
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TextFileSearchEngine.search(
                    java.io.File(textState.file.path),
                    query,
                    charset = charset,
                    shouldContinue = { searchContext.isActive }
                )
            }
            largeTextSearchMatches = result.matches
            largeTextSearchCompletedQuery = query
            totalMatchesCount = result.matchCount
            searchResultsTruncated = result.truncated
            currentMatchIndex = 0
            isLargeTextSearchLoading = false
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            largeTextSearchError = error.message ?: "Could not search the complete file"
            isLargeTextSearchLoading = false
            totalMatchesCount = 0
        }
    }

    LaunchedEffect(activeFile?.path, inViewerSearchQuery, largeTextSearchCompletedQuery, largeTextSearchMatches, currentMatchIndex) {
        if (!isLargeTextSearchable || inViewerSearchQuery.isBlank() ||
            largeTextSearchCompletedQuery != inViewerSearchQuery.trim() || largeTextSearchMatches.isEmpty()
        ) return@LaunchedEffect

        val textState = fileState as? MainViewModel.FileContentState.TextSuccess ?: return@LaunchedEffect
        val match = largeTextSearchMatches.getOrNull(currentMatchIndex) ?: return@LaunchedEffect
        try {
            val charset = java.nio.charset.Charset.forName(textState.charsetName)
            val window = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TextWindowReader.read(
                    java.io.File(textState.file.path),
                    match.byteOffset,
                    maxBytes = 250_000,
                    charset = charset
                )
            }
            fullPreviewText = window.text
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            largeTextSearchError = error.message ?: "Could not open the selected search result"
        }
    }

    val isFullTextLoading = isLargeTextSearchable && inViewerSearchQuery.isNotBlank() && isLargeTextSearchLoading

    val handleBack = {
        val parentPath = activeFile?.parentZipPath
        if (parentPath != null) {
            viewModel.openParentZip(parentPath)
        } else {
            onBackClick()
        }
    }

    androidx.activity.compose.BackHandler {
        handleBack()
    }

    val activity = context as? android.app.Activity
    val originalWindowBars = remember(activity) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            Triple(window.statusBarColor, window.navigationBarColor, controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars)
        }
    }
    val originalSystemBarsBehavior = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).systemBarsBehavior }
    }
    val originalDecorBackground = remember(activity) { activity?.window?.decorView?.background }
    val originalCutoutMode = remember(activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            activity?.window?.attributes?.layoutInDisplayCutoutMode
        } else null
    }
    SideEffect {
        val window = activity?.window
        if (window != null) {
            // The activity is edge-to-edge; keep the preview behind the system insets in focus mode.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val isDarkCanvas = when (activeFile?.extension?.lowercase()) {
                "pdf", "docx", "doc" -> isPdfNightMode
                else -> isDarkTheme
            }
            val canvasColor = when (activeFile?.extension?.lowercase()) {
                "pdf" -> if (isPdfNightMode) android.graphics.Color.rgb(24, 24, 24) else android.graphics.Color.rgb(240, 240, 240)
                "docx", "doc" -> if (isPdfNightMode) android.graphics.Color.rgb(21, 21, 20) else android.graphics.Color.rgb(250, 249, 245)
                else -> if (isDarkTheme) android.graphics.Color.rgb(24, 23, 21) else android.graphics.Color.rgb(250, 249, 245)
            }
            window.decorView.setBackgroundColor(canvasColor)
            insetsController.isAppearanceLightStatusBars = !isDarkCanvas
            insetsController.isAppearanceLightNavigationBars = !isDarkCanvas
            if (!isBarsVisible) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && originalCutoutMode != null) {
                    window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = originalCutoutMode }
                }
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                originalWindowBars?.let { (status, navigation, appearance) ->
                    window.statusBarColor = status
                    window.navigationBarColor = navigation
                    insetsController.isAppearanceLightStatusBars = appearance.first
                    insetsController.isAppearanceLightNavigationBars = appearance.second
                }
                if (originalSystemBarsBehavior != null) {
                    insetsController.systemBarsBehavior = originalSystemBarsBehavior
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && originalCutoutMode != null) {
                    window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = originalCutoutMode }
                }
                window.decorView.background = originalDecorBackground
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBarsVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                modifier = if (isBarsVisible) Modifier.statusBarsPadding() else Modifier
            ) {
                val titleText = fileState.fileOrNull?.name ?: "Preview"
                
                var showMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current

                ClaudeAppBar(
                    title = titleText,
                    onNavIconClick = handleBack,
                    navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        val canSearchInDoc = activeFile?.extension?.lowercase() in setOf("pdf", "docx", "txt", "md")
                        if (canSearchInDoc) {
                            IconButton(onClick = { isSearchInDocumentOpen = !isSearchInDocumentOpen }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Find in document",
                                    tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val extLower = activeFile?.extension?.lowercase() ?: ""
                        val hasOutline = extLower in setOf("md", "markdown", "docx", "doc", "txt", "pdf")
                        if (hasOutline) {
                            IconButton(onClick = { showOutlineBottomSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Toc,
                                    contentDescription = "Document Outline",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Focus Mode / Immersive Toggle
                        IconButton(onClick = { isBarsVisible = !isBarsVisible }) {
                            Icon(
                                imageVector = if (isBarsVisible) Icons.Outlined.Fullscreen else Icons.Outlined.FullscreenExit,
                                contentDescription = "Toggle Focus Mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isTextState = fileState is MainViewModel.FileContentState.TextSuccess ||
                            (activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv"))
                        if (isTextState) {
                            IconButton(onClick = {
                                val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                val entity = activeFile
                                if (textState != null && !textState.truncated) {
                                    copyTextToClipboard(context, entity?.name ?: "File Content", textState.content)
                                } else if (textState != null && entity != null) {
                                    printScope.launch {
                                        readBoundedText(java.io.File(entity.path), textState.charsetName)
                                            .onSuccess { copyTextToClipboard(context, entity.name, it) }
                                            .onFailure { showTextOperationError(context, it) }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy all text",
                                    tint = MaterialTheme.colorScheme.onSurface
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
                                val isDocx = activeFile?.extension?.lowercase() in setOf("docx", "doc")
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

                                if (hasOutline) {
                                    DropdownMenuItem(
                                        text = { Text("Document Outline") },
                                        onClick = {
                                            showMenu = false
                                            showOutlineBottomSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Toc,
                                                contentDescription = "Document Outline"
                                            )
                                        }
                                    )
                                }

                                val isTextFile = activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv")
                                if (isTextFile) {
                                    DropdownMenuItem(
                                        text = { Text("Viewer Appearance") },
                                        onClick = {
                                            showMenu = false
                                            showAppearanceBottomSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Palette,
                                                contentDescription = "Viewer Appearance"
                                            )
                                        }
                                    )

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
                                            when {
                                                ext == "pdf" -> {
                                                    com.example.utils.FilePrintHelper.printPdf(context, java.io.File(entity.path), entity.name)
                                                }
                                                ext in setOf("docx", "doc") -> {
                                                    val renderedWebView = docxPreviewWebView
                                                    if (docxPreviewReady && renderedWebView != null) {
                                                        com.example.utils.FilePrintHelper.printWebView(context, renderedWebView, entity.name)
                                                    } else {
                                                        com.example.utils.FilePrintHelper.printDocx(context, java.io.File(entity.path), entity.name)
                                                    }
                                                }
                                                ext == "md" || ext == "txt" -> {
                                                    val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                                    printScope.launch {
                                                        val result = if (textState != null && !textState.truncated) {
                                                            Result.success(textState.content)
                                                        } else {
                                                            readBoundedText(
                                                                java.io.File(entity.path),
                                                                textState?.charsetName ?: "UTF-8"
                                                            )
                                                        }
                                                        result.onSuccess { fullContent ->
                                                            if (ext == "md") {
                                                                com.example.utils.FilePrintHelper.printMarkdown(context, fullContent, entity.name)
                                                            } else {
                                                                com.example.utils.FilePrintHelper.printTextOrMarkdown(context, fullContent, entity.name)
                                                            }
                                                        }.onFailure { error ->
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                error.message ?: "Could not prepare this file for printing",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                                ext in setOf("png", "jpg", "jpeg", "webp", "gif") -> {
                                                    com.example.utils.FilePrintHelper.printImage(context, java.io.File(entity.path), entity.name)
                                                }
                                                else -> {
                                                    // The four document formats have bounded/dedicated branches above; preserve legacy fallback for other types.
                                                    val content = (fileState as? MainViewModel.FileContentState.TextSuccess)?.content
                                                        ?: runCatching { java.io.File(entity.path).readText() }.getOrNull() ?: ""
                                                    com.example.utils.FilePrintHelper.printTextOrMarkdown(context, content, entity.name)
                                                }
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
            val isDocx = fileState is MainViewModel.FileContentState.DocxSuccess || (activeFile?.extension?.lowercase() in setOf("docx", "doc"))
            val isImage = fileState is MainViewModel.FileContentState.ImageSuccess
            val isText = fileState is MainViewModel.FileContentState.TextSuccess ||
                (activeFile?.extension?.lowercase() in setOf("txt", "md", "json", "xml", "html", "htm", "js", "ts", "css", "log", "yaml", "yml", "ini", "conf", "sh", "py", "java", "kt", "c", "cpp", "h", "sql", "csv", "tsv"))

            val showFloatingBar = isPdf || isDocx || isImage || isText

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

                                    // Print / Save as PDF
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                com.example.utils.FilePrintHelper.printPdf(context, java.io.File(it.path), it.name)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Print,
                                            contentDescription = "Print or Save as PDF",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
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
                                } else if (isDocx) {
                                    // DOCX Zoom Out Button
                                    IconButton(
                                        onClick = {
                                            docxZoomFactor = (docxZoomFactor - 0.25f).coerceAtLeast(0.25f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ZoomOut,
                                            contentDescription = "Zoom Out",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Zoom percentage chip
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable {
                                            docxZoomInput = (docxZoomFactor * 100).toInt().toString()
                                            showDocxZoomDialog = true
                                        }
                                    ) {
                                        Text(
                                            text = "${(docxZoomFactor * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // DOCX Zoom In Button
                                    IconButton(
                                        onClick = {
                                            docxZoomFactor = (docxZoomFactor + 0.25f).coerceAtMost(5.0f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ZoomIn,
                                            contentDescription = "Zoom In",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Find in DOCX
                                    IconButton(
                                        onClick = { isSearchInDocumentOpen = !isSearchInDocumentOpen },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find in DOCX",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSearchInDocumentOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // DOCX Night Mode toggle
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

                                    // Print DOCX
                                    IconButton(
                                        onClick = {
                                            activeFile?.let {
                                                val renderedWebView = docxPreviewWebView
                                                if (docxPreviewReady && renderedWebView != null) {
                                                    com.example.utils.FilePrintHelper.printWebView(context, renderedWebView, it.name)
                                                } else {
                                                    com.example.utils.FilePrintHelper.printDocx(context, java.io.File(it.path), it.name)
                                                }
                                            }
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

                                    // Share DOCX
                                    IconButton(
                                        onClick = { activeFile?.let { shareFile(context, File(it.path), it.extension) } },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share document",
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
                                            val ext = activeFile?.extension?.lowercase() ?: ""
                                            val entity = activeFile
                                            val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                            if (entity != null) {
                                                printScope.launch {
                                                    val result = if (textState != null && !textState.truncated) {
                                                        Result.success(textState.content)
                                                    } else {
                                                        readBoundedText(
                                                            java.io.File(entity.path),
                                                            textState?.charsetName ?: "UTF-8"
                                                        )
                                                    }
                                                    result.onSuccess { textToPrint ->
                                                        if (ext == "md") {
                                                            com.example.utils.FilePrintHelper.printMarkdown(context, textToPrint, entity.name)
                                                        } else {
                                                            com.example.utils.FilePrintHelper.printTextOrMarkdown(context, textToPrint, entity.name)
                                                        }
                                                    }.onFailure { error ->
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            error.message ?: "Could not prepare this file for printing",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
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
                                            val textState = fileState as? MainViewModel.FileContentState.TextSuccess
                                            val entity = activeFile
                                            if (textState != null && !textState.truncated) {
                                                copyTextToClipboard(context, entity?.name ?: "File Content", textState.content)
                                            } else if (textState != null && entity != null) {
                                                printScope.launch {
                                                    readBoundedText(java.io.File(entity.path), textState.charsetName)
                                                        .onSuccess { copyTextToClipboard(context, entity.name, it) }
                                                        .onFailure { showTextOperationError(context, it) }
                                                }
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
        val usesReaderGestures = activeFile?.extension?.lowercase() in setOf("pdf", "doc", "docx", "txt", "md")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!usesReaderGestures) {
                        Modifier.pointerInput(activeFile?.path) {
                            detectTapGestures(onDoubleTap = { isBarsVisible = !isBarsVisible })
                        }
                    } else Modifier
                )
                .background(MaterialTheme.colorScheme.background)
                .let {
                    if (isBarsVisible) it.padding(innerPadding) else it
                }
        ) {
            // Subtle Floating Back & Exit Pill when !isBarsVisible (Focus mode)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isBarsVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { isBarsVisible = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FullscreenExit,
                                contentDescription = "Exit Focus Mode",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Floating In-Viewer Search Bar Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearchInDocumentOpen && isBarsVisible,
                enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(if (isBarsVisible) Modifier.statusBarsPadding() else Modifier)
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
                                onValueChange = {
                                    inViewerSearchQuery = it
                                    currentMatchIndex = 0
                                    totalMatchesCount = 0
                                    searchResultsTruncated = false
                                    pdfSearchError = null
                                    largeTextSearchMatches = emptyList()
                                    largeTextSearchCompletedQuery = ""
                                    largeTextSearchError = null
                                    if (fileState is MainViewModel.FileContentState.PdfSuccess) {
                                        isPdfSearchLoading = it.isNotBlank()
                                        pdfSearchSnapshot = PdfSearchSnapshot()
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (inViewerSearchQuery.isNotEmpty()) {
                            val isPdfSearch = fileState is MainViewModel.FileContentState.PdfSuccess
                            val extension = activeFile?.extension?.lowercase()
                            val navigableMatchCount = when {
                                isPdfSearch -> pdfSearchSnapshot.matches.size
                                isLargeTextSearchable -> largeTextSearchMatches.size
                                extension in setOf("docx", "doc") -> minOf(totalMatchesCount, 5_000)
                                extension in setOf("txt", "md") -> minOf(totalMatchesCount, 1_000)
                                else -> totalMatchesCount
                            }
                            val pageInfo = if (isPdfSearch) {
                                pdfSearchSnapshot.matches.getOrNull(currentMatchIndex)?.pageNumber?.let { " (p.$it)" }.orEmpty()
                            } else ""
                            val matchLabel = when {
                                isPdfSearchLoading -> "Searching…"
                                isFullTextLoading -> "Searching full file…"
                                (pdfSearchError != null && isPdfSearch) || (largeTextSearchError != null && isLargeTextSearchable) -> "Search unavailable"
                                totalMatchesCount > 0 && searchResultsTruncated -> "${currentMatchIndex + 1}/$navigableMatchCount of $totalMatchesCount$pageInfo"
                                totalMatchesCount > 0 -> "${currentMatchIndex + 1}/$totalMatchesCount$pageInfo"
                                else -> "0 matches"
                            }

                            Text(
                                text = matchLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (isPdfSearch) {
                                        val matches = pdfSearchSnapshot.matches
                                        if (matches.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
                                            scrollToPage = matches[currentMatchIndex].pageNumber - 1
                                        }
                                    } else if (isLargeTextSearchable) {
                                        if (largeTextSearchMatches.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex - 1 + largeTextSearchMatches.size) % largeTextSearchMatches.size
                                        }
                                    } else {
                                        searchTriggerPrev++
                                    }
                                },
                                enabled = totalMatchesCount > 0 && !isPdfSearchLoading,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    if (isPdfSearch) {
                                        val matches = pdfSearchSnapshot.matches
                                        if (matches.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex + 1) % matches.size
                                            scrollToPage = matches[currentMatchIndex].pageNumber - 1
                                        }
                                    } else if (isLargeTextSearchable) {
                                        if (largeTextSearchMatches.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex + 1) % largeTextSearchMatches.size
                                        }
                                    } else {
                                        searchTriggerNext++
                                    }
                                },
                                enabled = totalMatchesCount > 0 && !isPdfSearchLoading,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", modifier = Modifier.size(18.dp))
                            }
                        }

                        IconButton(
                            onClick = {
                                isSearchInDocumentOpen = false
                                inViewerSearchQuery = ""
                                pdfSearchSnapshot = PdfSearchSnapshot()
                                isPdfSearchLoading = false
                                searchResultsTruncated = false
                                totalMatchesCount = 0
                                currentMatchIndex = 0
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
                        val completeContent = fullPreviewText ?: state.content
                        val supportsFullFileSearch = state.file.extension.lowercase() in setOf("txt", "md")
                        val notice = state.readOnlyReason ?: when {
                            state.truncated && supportsFullFileSearch && isFullTextLoading -> "Searching the complete ${formatFileSize(state.totalBytes)} file…"
                            state.truncated && supportsFullFileSearch && largeTextSearchError != null -> "Full-file search failed: ${largeTextSearchError.orEmpty()}"
                            state.truncated && supportsFullFileSearch -> "Preview is bounded to protect memory; in-document search scans the complete ${formatFileSize(state.totalBytes)} file and opens matches in bounded excerpts (first 10,000 results are navigable)."
                            state.truncated -> "Preview is limited to the first ${state.content.length} characters to protect memory."
                            else -> null
                        }
                        if (notice != null && isBarsVisible) NoticeBanner(notice)
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
                            when (state.file.extension.lowercase()) {
                                "md" -> MarkdownPreview(
                                    markdownText = completeContent,
                                    fileEntity = state.file,
                                    fontSizeSetting = settings.fontSize,
                                    isNightMode = isDarkTheme,
                                    searchQuery = inViewerSearchQuery,
                                    searchTriggerNext = searchTriggerNext,
                                    searchTriggerPrev = searchTriggerPrev,
                                    onSearchResultCount = { count, index, truncated ->
                                        if (!isLargeTextSearchable) {
                                            totalMatchesCount = count
                                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                                            searchResultsTruncated = truncated
                                        }
                                    },
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    modifier = Modifier.fillMaxSize()
                                )
                                "txt" -> PlainTextPreview(
                                    content = completeContent,
                                    isDarkMode = isDarkTheme,
                                    fontSizeSetting = settings.fontSize,
                                    searchQuery = inViewerSearchQuery,
                                    searchTriggerNext = searchTriggerNext,
                                    searchTriggerPrev = searchTriggerPrev,
                                    onSearchResultCount = { count, index, truncated ->
                                        if (!isLargeTextSearchable) {
                                            totalMatchesCount = count
                                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                                            searchResultsTruncated = truncated
                                        }
                                    },
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> WebViewPreview(
                                    fileEntity = state.file,
                                    allowScripts = htmlScriptsEnabled,
                                    searchQuery = inViewerSearchQuery,
                                    onSingleTap = { isBarsVisible = !isBarsVisible },
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
                        searchQuery = inViewerSearchQuery,
                        selectedMatchIndex = currentMatchIndex,
                        scrollToPage = scrollToPage,
                        onScrollToPageHandled = { scrollToPage = null },
                        onSearchSnapshot = { snapshot, searching ->
                            pdfSearchSnapshot = snapshot
                            totalMatchesCount = snapshot.count
                            searchResultsTruncated = snapshot.truncated
                            isPdfSearchLoading = searching
                            pdfSearchError = null
                            if (!searching) currentMatchIndex = 0
                        },
                        onSearchError = { message ->
                            pdfSearchError = message
                            isPdfSearchLoading = false
                        },
                        modifier = Modifier.fillMaxSize(),
                        onSingleTap = { isBarsVisible = !isBarsVisible },
                        onPageChanged = { page, total ->
                            currentPdfPage = page
                            totalPdfPages = total
                        },
                        onBackClick = handleBack
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
                        searchQuery = inViewerSearchQuery,
                        searchTriggerNext = searchTriggerNext,
                        searchTriggerPrev = searchTriggerPrev,
                        onSearchResultCount = { count, index, truncated ->
                            totalMatchesCount = count
                            currentMatchIndex = if (count > 0) index.coerceAtLeast(0) else 0
                            searchResultsTruncated = truncated
                        },
                        zoomFactor = docxZoomFactor,
                        onWebViewCreated = { docxPreviewWebView = it },
                        onDocumentReady = { docxPreviewReady = true },
                        onSingleTap = { isBarsVisible = !isBarsVisible }
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

    // Glassmorphism Modal Bottom Sheet for File Properties
    if (showPropertiesDialog && activeFile != null) {
        com.example.ui.component.FilePropertiesBottomSheet(
            fileEntity = activeFile,
            onDismissRequest = { showPropertiesDialog = false }
        )
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

    if (showDocxZoomDialog) {
        AlertDialog(
            onDismissRequest = { showDocxZoomDialog = false },
            title = { Text("Document zoom") },
            text = {
                Column {
                    Text("${(docxZoomFactor * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = docxZoomInput,
                        onValueChange = { value -> docxZoomInput = value.filter { it.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("Exact zoom (%)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Slider(
                        value = docxZoomFactor,
                        onValueChange = {
                            docxZoomFactor = it
                            docxZoomInput = (it * 100).toInt().toString()
                        },
                        valueRange = 0.25f..5.0f,
                        steps = 18
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("25%", style = MaterialTheme.typography.labelSmall)
                        Text("500%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    docxZoomInput.toIntOrNull()?.let { percent ->
                        docxZoomFactor = percent.coerceIn(25, 500) / 100f
                    }
                    showDocxZoomDialog = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    docxZoomFactor = 1.0f
                    docxZoomInput = "100"
                }) { Text("Reset 100%") }
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
    isNightMode: Boolean,
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onSingleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var generatedHtml by remember(markdownText, isNightMode, fontSizeSetting) { mutableStateOf<String?>(null) }

    LaunchedEffect(markdownText, isNightMode, fontSizeSetting) {
        val htmlContent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val parser = org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
            val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
            val rawHtml = com.example.core.text.HtmlSanitizer.sanitize(
                org.intellij.markdown.html.HtmlGenerator(markdownText, parsedTree, org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()).generateHtml(),
                keepImages = true
            )

            val fontSize = when (fontSizeSetting) {
                "Small" -> "14px"
                "Large" -> "20px"
                else -> "16px"
            }

            // Claude Theme Colors (EXACT from File Claw - MD Preview Theme Fix Prompt)
            val bgColor = if (isNightMode) "#151514" else "#faf9f5" // canvas
            val textPrimary = if (isNightMode) "#f5f5f3" else "#141413" // ink
            val textBody = if (isNightMode) "#d1cfc7" else "#3d3d3a" // body
            val textBodyStrong = if (isNightMode) "#faf9f5" else "#252523" // body_strong
            val textSecondary = if (isNightMode) "#a39f93" else "#6c6a64" // muted
            val textSecondarySoft = if (isNightMode) "#8e8779" else "#8e8b82" // muted_soft
            val accent = if (isNightMode) "#e08567" else "#cc785c" // primary (coral)
            val accentActive = if (isNightMode) "#f09a7d" else "#a9583e" // primary_active
            val border = if (isNightMode) "#2d2b28" else "#e6dfd8" // hairline
            val borderSoft = if (isNightMode) "#211f1d" else "#ebe6df" // hairline_soft
            val cardBg = if (isNightMode) "#2a2723" else "#efe9de" // surface_card
            val quoteBg = if (isNightMode) "#1c1a18" else "#f5f0e8" // surface_soft
            val inlineCodeBg = if (isNightMode) "#2d2823" else "#e8e0d2" // surface_cream_strong
            val codeBlockOuterBg = if (isNightMode) "#0d0d0c" else "#181715" // surface_dark
            val codeBlockInnerBg = if (isNightMode) "#161513" else "#1f1e1b" // surface_dark_soft
            val codeBlockHeaderBg = if (isNightMode) "#1d1b19" else "#252320" // surface_dark_elevated
            val codeBlockText = if (isNightMode) "#f5f5f3" else "#faf9f5" // on_dark
            val codeBlockButtons = if (isNightMode) "#a09d96" else "#8e8b82" // on_dark_soft

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
                        font-size: 14px;
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
                    .search-highlight { background: #ffe082; color: #17130a; border-radius: 2px; }
                    .search-highlight-active { background: #ff9800; color: #17130a; outline: 2px solid #e65100; }
                    @media print { .code-header, .copy-btn { display: none !important; } .code-block-container { break-inside: avoid; } }
                </style>
                $DocumentPreviewScript
            </head>
            <body>
                <main id="document-content">$rawHtml</main>
            </body>
            </html>
            """.trimIndent()
        }
        generatedHtml = htmlContent
    }

    SearchableDocumentWebView(
        html = generatedHtml,
        backgroundColor = if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt(),
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        onSingleTap = onSingleTap,
        loadingMessage = "Preparing Document...",
        modifier = modifier
    )
}

@Composable
private fun PlainTextPreview(
    content: String,
    isDarkMode: Boolean,
    fontSizeSetting: String,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var generatedHtml by remember(content, isDarkMode, fontSizeSetting) { mutableStateOf<String?>(null) }
    LaunchedEffect(content, isDarkMode, fontSizeSetting) {
        val fontSizePx = when (fontSizeSetting) {
            "Small" -> 13
            "Large" -> 18
            else -> 15
        }
        generatedHtml = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            buildPlainTextPreviewHtml(content, isDarkMode, fontSizePx)
        }
    }
    SearchableDocumentWebView(
        html = generatedHtml,
        backgroundColor = if (isDarkMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt(),
        searchQuery = searchQuery,
        searchTriggerNext = searchTriggerNext,
        searchTriggerPrev = searchTriggerPrev,
        onSearchResultCount = onSearchResultCount,
        onSingleTap = onSingleTap,
        loadingMessage = "Preparing Text...",
        modifier = modifier
    )
}

@Composable
private fun SearchableDocumentWebView(
    html: String?,
    backgroundColor: Int,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    onSingleTap: () -> Unit,
    loadingMessage: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestSearchResultCallback = androidx.compose.runtime.rememberUpdatedState(onSearchResultCount)
    val latestSingleTapCallback = androidx.compose.runtime.rememberUpdatedState(onSingleTap)
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var lastIssuedNext by remember { mutableIntStateOf(0) }
    var lastIssuedPrev by remember { mutableIntStateOf(0) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { viewContext ->
            android.webkit.WebView(viewContext).apply {
                webViewRef = this
                setBackgroundColor(backgroundColor)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                addJavascriptInterface(
                    WebAppInterface(
                        viewContext,
                        onSearchResultsChanged = { count, index, truncated -> latestSearchResultCallback.value(count, index, truncated) },
                        onTapRequested = { latestSingleTapCallback.value() }
                    ),
                    "Android"
                )
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.post {
                            isPageReady = true
                            isLoading = false
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: android.webkit.WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        return !(url.startsWith("about:") || url.startsWith("data:"))
                    }
                }
            }
        },
        update = { webView ->
            if (html != null && webView.tag != html) {
                webView.tag = html
                isPageReady = false
                isLoading = true
                webView.setBackgroundColor(backgroundColor)
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(webViewRef, isPageReady, searchQuery, searchTriggerNext, searchTriggerPrev) {
        val webView = webViewRef
        if (webView != null && isPageReady) {
            val nextDelta = (searchTriggerNext - lastIssuedNext).coerceAtLeast(0)
            val prevDelta = (searchTriggerPrev - lastIssuedPrev).coerceAtLeast(0)
            val encodedQuery = org.json.JSONObject.quote(searchQuery)
            val script = buildString {
                append("searchWord($encodedQuery);")
                repeat(nextDelta) { append("nextSearchMatch();") }
                repeat(prevDelta) { append("prevSearchMatch();") }
            }
            webView.evaluateJavascript(script, null)
            lastIssuedNext = searchTriggerNext
            lastIssuedPrev = searchTriggerPrev
        } else if (searchTriggerNext < lastIssuedNext) {
            lastIssuedNext = searchTriggerNext
            lastIssuedPrev = searchTriggerPrev
        }
    }

    if (html == null || isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(Color(backgroundColor)),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.component.PremiumLoadingIndicator(text = loadingMessage)
        }
    }
}

class WebAppInterface(
    private val context: android.content.Context,
    private val onSearchResultsChanged: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    private val onTapRequested: () -> Unit = {}
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @android.webkit.JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Code", text))
        mainHandler.post {
            android.widget.Toast.makeText(context, "Code block copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @android.webkit.JavascriptInterface
    fun onSearchResultCount(count: Int, currentIndex: Int, truncated: Boolean) {
        mainHandler.post { onSearchResultsChanged(count, currentIndex, truncated) }
    }

    @android.webkit.JavascriptInterface
    fun onDocumentTap() {
        mainHandler.post { onTapRequested() }
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
    onScrollToLineHandled: () -> Unit = {}
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
            val htmlContent = remember(codeText, isDark, fontSizeSetting, wrapSetting, showLineNumbers) {
                val escapedCode = codeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val bgColor = if (isDark) "#1e1e1e" else "#ffffff"
                val textColor = if (isDark) "#d4d4d4" else "#000000"
                val fSize = when (fontSizeSetting) {
                    "Small" -> "12px"
                    "Large" -> "18px"
                    else -> "14px"
                }
                val wWrap = if (wrapSetting) "pre-wrap" else "pre"
                val wWord = if (wrapSetting) "break-word" else "normal"
                val theme = if (isDark) "vs2015.min.css" else "vs.min.css"
                
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
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
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


private fun isBundledPdfReaderUri(uri: android.net.Uri): Boolean {
    val path = uri.path.orEmpty()
    return uri.scheme == "https" &&
        uri.host == "appassets.androidplatform.net" &&
        uri.port in setOf(-1, 443) &&
        uri.userInfo == null &&
        (path.startsWith("/assets/pdfjs/") || path == "/search-pdf/document.pdf")
}

@Composable
fun PdfPreview(
    fileEntity: RecentFileEntity,
    isNightMode: Boolean,
    searchQuery: String = "",
    selectedMatchIndex: Int = 0,
    scrollToPage: Int?,
    onScrollToPageHandled: () -> Unit,
    onSearchSnapshot: (PdfSearchSnapshot, Boolean) -> Unit = { _, _ -> },
    onSearchError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit = {},
    onPageChanged: (Int, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val pdfFile = remember(fileEntity.path) { File(fileEntity.path) }
    val bookmarkKey = remember(fileEntity.path) { "bookmark_${fileEntity.path.hashCode()}" }
    val bookmarkPrefs = remember(context) { context.getSharedPreferences("pdf_bookmarks", android.content.Context.MODE_PRIVATE) }
    val savedPage = remember(fileEntity.path) { bookmarkPrefs.getInt(bookmarkKey, 0) }
    val assetLoader = remember(context, fileEntity.path) {
        val defaultAssetsHandler = androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context)
        androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    if (path.endsWith(".mjs", ignoreCase = true)) {
                        return runCatching {
                            android.webkit.WebResourceResponse(
                                "application/javascript",
                                "UTF-8",
                                context.assets.open(path)
                            )
                        }.getOrNull()
                    }
                    return defaultAssetsHandler.handle(path)
                }
            })
            .addPathHandler("/search-pdf/", object : androidx.webkit.WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    if (path != "document.pdf" || !pdfFile.isFile) return null
                    return runCatching {
                        android.webkit.WebResourceResponse(
                            "application/pdf",
                            "binary",
                            200,
                            "OK",
                            mapOf("Content-Length" to pdfFile.length().toString(), "Accept-Ranges" to "none"),
                            java.io.FileInputStream(pdfFile)
                        )
                    }.getOrNull()
                }
            })
            .build()
    }

    var webViewRef by remember(fileEntity.path) { mutableStateOf<android.webkit.WebView?>(null) }
    var isJavascriptReady by remember(fileEntity.path) { mutableStateOf(false) }
    var isPdfReady by remember(fileEntity.path) { mutableStateOf(false) }
    var requirePassword by remember(fileEntity.path) { mutableStateOf(false) }
    var passwordError by remember(fileEntity.path) { mutableStateOf(false) }
    var pdfPassword by remember(fileEntity.path) { mutableStateOf("") }
    var pdfErrorMessage by remember(fileEntity.path) { mutableStateOf<String?>(null) }

    val latestSearchSnapshot = androidx.compose.runtime.rememberUpdatedState(onSearchSnapshot)
    val latestSearchError = androidx.compose.runtime.rememberUpdatedState(onSearchError)
    val latestPageChanged = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val latestSingleTap = androidx.compose.runtime.rememberUpdatedState(onSingleTap)

    LaunchedEffect(webViewRef, isJavascriptReady, isPdfReady, searchQuery) {
        val webView = webViewRef
        if (webView != null && isJavascriptReady) {
            val query = org.json.JSONObject.quote(searchQuery)
            if (isPdfReady) webView.evaluateJavascript("searchPdf($query);", null)
        }
    }
    LaunchedEffect(webViewRef, isJavascriptReady, isNightMode) {
        if (isJavascriptReady) {
            webViewRef?.evaluateJavascript("setPdfNightMode($isNightMode);", null)
        }
    }
    LaunchedEffect(webViewRef, isJavascriptReady, selectedMatchIndex) {
        if (isJavascriptReady) {
            webViewRef?.evaluateJavascript("showPdfSearchMatch($selectedMatchIndex);", null)
        }
    }
    LaunchedEffect(webViewRef, isPdfReady, scrollToPage) {
        if (isPdfReady && scrollToPage != null) {
            webViewRef?.evaluateJavascript("gotoPdfPage(${scrollToPage + 1});", null)
            onScrollToPageHandled()
        }
    }
    LaunchedEffect(webViewRef, isPdfReady, savedPage) {
        if (isPdfReady && savedPage > 0 && scrollToPage == null) {
            webViewRef?.evaluateJavascript("gotoPdfPage(${savedPage + 1});", null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF191A1B) else Color(0xFFF0F0F0))
    ) {
        if (pdfErrorMessage == null) {
            androidx.compose.runtime.key(fileEntity.path) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { viewContext ->
                        android.webkit.WebView(viewContext).apply {
                            webViewRef = this
                            setBackgroundColor(if (isNightMode) 0xFF191A1B.toInt() else 0xFFF0F0F0.toInt())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            }
                            addJavascriptInterface(
                                PdfWebAppInterface(
                                    context = viewContext,
                                    onReady = { isJavascriptReady = true },
                                    onDocumentLoaded = { pageCount ->
                                        isPdfReady = true
                                        requirePassword = false
                                        passwordError = false
                                        pdfErrorMessage = null
                                        latestPageChanged.value(1, pageCount)
                                    },
                                    onPageChanged = { page, pageCount ->
                                        bookmarkPrefs.edit().putInt(bookmarkKey, (page - 1).coerceAtLeast(0)).apply()
                                        latestPageChanged.value(page, pageCount)
                                    },
                                    onSearchSnapshot = { snapshot, searching -> latestSearchSnapshot.value(snapshot, searching) },
                                    onSearchError = { message -> latestSearchError.value(message) },
                                    onPasswordRequired = { reason ->
                                        passwordError = reason == 2
                                        requirePassword = true
                                    },
                                    onError = { message ->
                                        pdfErrorMessage = message
                                        isPdfReady = false
                                    },
                                    onSingleTap = { latestSingleTap.value() }
                                ),
                                "Android"
                            )
                            webViewClient = object : androidx.webkit.WebViewClientCompat() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): Boolean = !isBundledPdfReaderUri(request.url)

                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView,
                                    url: String
                                ): Boolean = !isBundledPdfReaderUri(android.net.Uri.parse(url))

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    request: android.webkit.WebResourceRequest
                                ): android.webkit.WebResourceResponse? {
                                    return assetLoader.shouldInterceptRequest(request.url)
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView,
                                    url: String
                                ): android.webkit.WebResourceResponse? {
                                    return assetLoader.shouldInterceptRequest(android.net.Uri.parse(url))
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d(
                                        "PdfJsReader",
                                        "${message?.message()} -- line ${message?.lineNumber()} ${message?.sourceId()}"
                                    )
                                    return true
                                }
                            }
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        webView.setBackgroundColor(if (isNightMode) 0xFF191A1B.toInt() else 0xFFF0F0F0.toInt())
                        if (webView.tag != fileEntity.path) {
                            webView.tag = fileEntity.path
                            isJavascriptReady = false
                            isPdfReady = false
                            webView.loadUrl("https://appassets.androidplatform.net/assets/pdfjs/search.html")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (!isPdfReady && pdfErrorMessage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.example.ui.component.PremiumLoadingIndicator(
                    text = if (requirePassword) "Password required" else "Opening PDF..."
                )
            }
        }

        if (pdfErrorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
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
                Text("Could not open PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pdfErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBackClick) { Text("Go Back") }
            }
        }

        if (requirePassword) {
            AlertDialog(
                onDismissRequest = onBackClick,
                title = { Text("Password Required") },
                text = {
                    Column {
                        Text("This PDF is password protected.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pdfPassword,
                            onValueChange = { pdfPassword = it; passwordError = false },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = passwordError,
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError) Text("Incorrect password", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        requirePassword = false
                        webViewRef?.evaluateJavascript("submitPdfPassword(${org.json.JSONObject.quote(pdfPassword)});", null)
                    }) { Text("Open") }
                },
                dismissButton = { TextButton(onClick = onBackClick) { Text("Cancel") } }
            )
        }
    }
}

private class PdfWebAppInterface(
    context: android.content.Context,
    private val onReady: () -> Unit,
    private val onDocumentLoaded: (Int) -> Unit,
    private val onPageChanged: (Int, Int) -> Unit,
    private val onSearchSnapshot: (PdfSearchSnapshot, Boolean) -> Unit,
    private val onSearchError: (String) -> Unit,
    private val onPasswordRequired: (Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onSingleTap: () -> Unit
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val appContext = context.applicationContext

    @android.webkit.JavascriptInterface
    fun onPdfSearchReady(@Suppress("UNUSED_PARAMETER") status: String) {
        mainHandler.post { onReady() }
    }

    @android.webkit.JavascriptInterface
    fun onPdfDocumentLoaded(pageCount: Int) {
        mainHandler.post { onDocumentLoaded(pageCount) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfPageChanged(page: Int, pageCount: Int) {
        mainHandler.post { onPageChanged(page, pageCount) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSearchResults(payload: String) {
        runCatching {
            val json = org.json.JSONObject(payload)
            val jsonMatches = json.optJSONArray("matches") ?: org.json.JSONArray()
            val matches = buildList {
                for (index in 0 until jsonMatches.length()) {
                    val matchJson = jsonMatches.optJSONObject(index) ?: continue
                    add(PdfPageSearchMatch(matchJson.optInt("pageNumber")))
                }
            }
            val snapshot = PdfSearchSnapshot(
                count = json.optInt("count"),
                truncated = json.optBoolean("truncated"),
                matches = matches
            )
            val searching = json.optBoolean("searching")
            mainHandler.post { onSearchSnapshot(snapshot, searching) }
        }.onFailure { error ->
            android.util.Log.w("PdfJsReader", "Could not read search results", error)
        }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSearchError(payload: String) {
        val message = runCatching { org.json.JSONObject(payload).optString("message") }
            .getOrDefault("PDF text search failed")
        mainHandler.post { onSearchError(message) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfPasswordRequired(reason: Int) {
        mainHandler.post { onPasswordRequired(reason) }
    }

    @android.webkit.JavascriptInterface
    fun onPdfError(message: String) {
        mainHandler.post {
            onError(message.ifBlank { "This PDF could not be opened." })
            android.widget.Toast.makeText(appContext, "Could not open PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @android.webkit.JavascriptInterface
    fun onPdfSingleTap() {
        mainHandler.post { onSingleTap() }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val context = LocalContext.current
        val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

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
                } else {
                    // PDF, DOCX, TXT, and Markdown have dedicated preview routes; this fallback handles other text formats.
                    try {
                        val content = java.io.File(fileEntity.path).readText()
                        val htmlContent = """
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
    searchQuery: String = "",
    searchTriggerNext: Int = 0,
    searchTriggerPrev: Int = 0,
    onSearchResultCount: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    zoomFactor: Float = 1.0f,
    onWebViewCreated: (android.webkit.WebView?) -> Unit = {},
    onDocumentReady: () -> Unit = {},
    onSingleTap: () -> Unit = {}
) {
    var isWebViewLoading by remember(base64Docx) { mutableStateOf(true) }
    var webViewRef by remember(base64Docx) { mutableStateOf<android.webkit.WebView?>(null) }
    var pureKotlinFallback by remember(base64Docx) { mutableStateOf<com.example.core.format.DocxExtractor.ExtractedDocx?>(null) }
    var lastIssuedNext by remember(base64Docx) { mutableIntStateOf(0) }
    var lastIssuedPrev by remember(base64Docx) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val latestSearchResultCallback = androidx.compose.runtime.rememberUpdatedState(onSearchResultCount)
    val latestPageChangedCallback = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val latestDocumentReadyCallback = androidx.compose.runtime.rememberUpdatedState(onDocumentReady)
    val latestWebViewCreatedCallback = androidx.compose.runtime.rememberUpdatedState(onWebViewCreated)
    val latestSingleTapCallback = androidx.compose.runtime.rememberUpdatedState(onSingleTap)

    LaunchedEffect(webViewRef, isWebViewLoading, searchQuery, searchTriggerNext, searchTriggerPrev, pureKotlinFallback) {
        val webView = webViewRef
        if (webView != null && !isWebViewLoading && pureKotlinFallback == null) {
            val nextDelta = (searchTriggerNext - lastIssuedNext).coerceAtLeast(0)
            val prevDelta = (searchTriggerPrev - lastIssuedPrev).coerceAtLeast(0)
            val query = org.json.JSONObject.quote(searchQuery)
            val script = buildString {
                append("searchWord($query);")
                repeat(nextDelta) { append("nextSearchMatch();") }
                repeat(prevDelta) { append("prevSearchMatch();") }
            }
            webView.evaluateJavascript(script, null)
            lastIssuedNext = searchTriggerNext
            lastIssuedPrev = searchTriggerPrev
        } else if (searchTriggerNext < lastIssuedNext) {
            lastIssuedNext = searchTriggerNext
            lastIssuedPrev = searchTriggerPrev
        }
    }
    LaunchedEffect(webViewRef, isWebViewLoading, isNightMode, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null) {
            webViewRef?.evaluateJavascript("setDarkMode($isNightMode);", null)
        }
    }
    LaunchedEffect(webViewRef, isWebViewLoading, zoomFactor, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null) {
            webViewRef?.evaluateJavascript("setZoom($zoomFactor);", null)
        }
    }
    LaunchedEffect(webViewRef, isWebViewLoading, scrollToPage, pureKotlinFallback) {
        if (webViewRef != null && !isWebViewLoading && pureKotlinFallback == null && scrollToPage != null) {
            webViewRef?.evaluateJavascript("scrollToPage(${scrollToPage + 1});", null)
            onScrollProgressHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5))) {
        if (pureKotlinFallback != null) {
            val extracted = pureKotlinFallback!!
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .15f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No text content found in this DOCX file.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                } else {
                    PlainTextPreview(
                        content = extracted.paragraphs.joinToString("\n\n"),
                        isDarkMode = isNightMode,
                        fontSizeSetting = "Default",
                        searchQuery = searchQuery,
                        searchTriggerNext = searchTriggerNext,
                        searchTriggerPrev = searchTriggerPrev,
                        onSearchResultCount = onSearchResultCount,
                        onSingleTap = onSingleTap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            androidx.compose.runtime.key(base64Docx) {
                AndroidView(
                    factory = { viewContext ->
                        android.webkit.WebView(viewContext).apply {
                            webViewRef = this
                            latestWebViewCreatedCallback.value(this)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                // Only bundled editor assets need file access; do not grant cross-file or content access.
                                allowFileAccess = true
                                allowContentAccess = false
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(false)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())

                            val pageChangedCallback = latestPageChangedCallback
                            addJavascriptInterface(object : Any() {
                                private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

                                @android.webkit.JavascriptInterface
                                fun getDocxBase64(): String = base64Docx

                                @android.webkit.JavascriptInterface
                                fun onDocumentLoaded(totalPages: Int) {
                                    mainHandler.post {
                                        isWebViewLoading = false
                                        latestDocumentReadyCallback.value()
                                        pageChangedCallback.value(1, totalPages)
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onPageChanged(currentPage: Int, totalPages: Int) {
                                    mainHandler.post { pageChangedCallback.value(currentPage, totalPages) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onSearchResultCount(count: Int, currentIndex: Int, truncated: Boolean) {
                                    mainHandler.post { latestSearchResultCallback.value(count, currentIndex, truncated) }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentTap() {
                                    mainHandler.post { latestSingleTapCallback.value() }
                                }

                                @android.webkit.JavascriptInterface
                                fun onDocumentError(error: String) {
                                    mainHandler.post {
                                        isWebViewLoading = false
                                        android.util.Log.w("WebViewDocx", "Switching to text-only DOCX preview: $error")
                                        val extracted = com.example.core.format.DocxExtractor.extractText(File(fileEntity.path))
                                        if (extracted.paragraphs.isNotEmpty()) {
                                            pureKotlinFallback = extracted
                                        } else {
                                            android.widget.Toast.makeText(context, "DOCX Notice: $error", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun exitWithoutSaving() {
                                    mainHandler.post { viewModel.closeCurrentFile() }
                                }
                            }, "Android")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    view?.evaluateJavascript("loadDocx(window.Android.getDocxBase64(), $isNightMode);", null)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return true
                                    val localEditorAsset = uri.scheme == "file" &&
                                        uri.path?.startsWith("/android_asset/editor/") == true
                                    return !localEditorAsset
                                }

                                override fun onReceivedError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        android.util.Log.e("WebViewDocx", "Error: ${error?.description}")
                                        val extracted = com.example.core.format.DocxExtractor.extractText(File(fileEntity.path))
                                        if (extracted.paragraphs.isNotEmpty()) {
                                            view?.post { pureKotlinFallback = extracted; isWebViewLoading = false }
                                        }
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    android.util.Log.e("WebViewDocx", "HTTP Error: ${errorResponse?.statusCode}")
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d("WebViewDocx", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                    return true
                                }
                            }
                            loadUrl("file:///android_asset/editor/docx_editor.html")
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        latestWebViewCreatedCallback.value(webView)
                        webView.setBackgroundColor(if (isNightMode) 0xFF151514.toInt() else 0xFFfaf9f5.toInt())
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isWebViewLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(if (isNightMode) Color(0xFF151514) else Color(0xFFfaf9f5)),
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

