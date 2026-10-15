package com.example.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** How the reader is being edited. RICH = WYSIWYG Markdown, PLAIN = text editor (TXT or Markdown source). */
enum class ReaderEditMode { RICH, PLAIN }

data class ReaderHistoryEntry(val label: String, val time: Long, val delta: Int)

/** Editor state reported by the page (see reader.js report()). */
data class ReaderEditorState(
    val active: Boolean = false,
    val mode: String = "",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val dirty: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val historyIndex: Int = -1,
    val history: List<ReaderHistoryEntry> = emptyList()
)

/** Loads the reader stylesheet and script from assets once per process. */
object ReaderAssets {
    @Volatile
    private var cached: Pair<String, String>? = null

    fun load(context: Context): Pair<String, String> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: Pair(read(context, "reader/reader.css"), read(context, "reader/reader.js")).also { cached = it }
        }
    }

    private fun read(context: Context, path: String): String =
        context.applicationContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/**
 * Handle for one reader page: issues commands to the page and exposes what the page reports.
 * Create one per document (remember keyed by the file path).
 */
@Stable
class ReaderController {
    internal var webView: WebView? = null

    /** True while a fully initialised page is showing. */
    var isReady by mutableStateOf(false)
        internal set

    /** Increments every time a page finishes initialising (used to re-apply state after reloads). */
    var readyGeneration by mutableIntStateOf(0)
        internal set

    var editorState by mutableStateOf(ReaderEditorState())
        internal set

    @Volatile
    internal var lastScrollRatio: Float = 0f

    private var nextRequestId = 1
    private val pendingContent = HashMap<Int, (String?) -> Unit>()

    private fun js(script: String) {
        webView?.evaluateJavascript(script, null)
    }

    private fun quote(value: String) = ReaderHtml.jsonString(value)

    fun applyAppearance(json: String) = js("window.FC&&FC.applyAppearance($json)")
    fun search(query: String) = js("window.FC&&FC.search(${quote(query)})")
    fun searchNext() = js("window.FC&&FC.searchNext()")
    fun searchPrev() = js("window.FC&&FC.searchPrev()")
    fun scrollToLine(line: Int) = js("window.FC&&FC.scrollToLine(${line.coerceAtLeast(1)})")
    fun scrollToHeading(sourceOffset: Int) = js("window.FC&&FC.scrollToHeading(${sourceOffset.coerceAtLeast(0)})")

    fun enterEdit(plain: Boolean) = js("window.FC&&FC.editor.enter(${if (plain) "'source'" else "'rich'"})")

    /** Leaves edit mode and restores the document as it was (discard). */
    fun exitEdit() = js("window.FC&&FC.editor.exit()")

    /** Leaves edit mode but keeps the edited DOM on screen (used right after a successful save). */
    fun finishEditKeepingChanges() = js("window.FC&&FC.editor.exit(true)")

    fun undo() = js("window.FC&&FC.editor.undo()")
    fun redo() = js("window.FC&&FC.editor.redo()")
    fun tab() = js("window.FC&&FC.editor.tab(false)")
    fun bold() = js("window.FC&&FC.editor.bold()")
    fun italic() = js("window.FC&&FC.editor.italic()")
    fun jumpToHistory(index: Int) = js("window.FC&&FC.editor.jump($index)")

    fun command(name: String, argument: String? = null) {
        val arg = if (argument != null) "," + quote(argument) else ""
        js("window.FC&&FC.editor.command(${quote(name)}$arg)")
    }

    /** Asks the page for the current document text (Markdown or plain text). Null when unavailable. */
    fun requestContent(callback: (String?) -> Unit) {
        val view = webView
        if (view == null || !isReady) {
            callback(null)
            return
        }
        val id = nextRequestId++
        pendingContent[id] = callback
        view.evaluateJavascript("(function(){if(window.FC&&FC.editor){FC.editor.requestContent($id);return 1}return 0})()") { result ->
            if (result != "1") pendingContent.remove(id)?.invoke(null)
        }
    }

    internal fun deliverContent(id: Int, text: String?) {
        pendingContent.remove(id)?.invoke(text)
    }

    internal fun applyEditorJson(json: String) {
        val o = try {
            JSONObject(json)
        } catch (e: Exception) {
            return
        }
        val active = o.optBoolean("active")
        val history = if (!active) {
            emptyList()
        } else if (o.has("history")) {
            val array = o.optJSONArray("history")
            if (array == null) emptyList() else List(array.length()) { i ->
                val h = array.optJSONObject(i)
                ReaderHistoryEntry(h?.optString("label").orEmpty(), h?.optLong("time") ?: 0L, h?.optInt("delta") ?: 0)
            }
        } else {
            editorState.history
        }
        editorState = ReaderEditorState(
            active = active,
            mode = o.optString("mode"),
            canUndo = o.optBoolean("canUndo"),
            canRedo = o.optBoolean("canRedo"),
            dirty = o.optBoolean("dirty"),
            bold = o.optBoolean("bold"),
            italic = o.optBoolean("italic"),
            historyIndex = o.optInt("index", -1),
            history = history
        )
    }

    internal fun markLoading() {
        isReady = false
    }

    internal fun markReady() {
        isReady = true
        readyGeneration += 1
    }
}

/** Events coming from the page. Implemented by [ReaderDocumentView]. */
internal interface ReaderEvents {
    fun onReady()
    fun onSearchResult(count: Int, index: Int, truncated: Boolean)
    fun onTap()
    fun onLink(url: String)
    fun onCopy(text: String)
    fun onEditorState(json: String)
    fun onContent(id: Int, text: String?)
    fun onDraft(text: String)
    fun onScroll(ratio: Float)
    fun onShortcut(name: String)
}

/** JavaScript interface exposed to the page as window.FCBridge. Calls arrive on a binder thread. */
internal class ReaderBridge(private val events: () -> ReaderEvents) {
    private val main = Handler(Looper.getMainLooper())

    private fun post(block: (ReaderEvents) -> Unit) {
        main.post { block(events()) }
    }

    @JavascriptInterface
    fun onReaderReady(info: String) = post { it.onReady() }

    @JavascriptInterface
    fun onSearchResultCount(count: Int, index: Int, truncated: Boolean) = post { it.onSearchResult(count, index, truncated) }

    @JavascriptInterface
    fun onDocumentTap() = post { it.onTap() }

    @JavascriptInterface
    fun onLinkClicked(url: String) = post { it.onLink(url) }

    @JavascriptInterface
    fun copyToClipboard(text: String) = post { it.onCopy(text) }

    @JavascriptInterface
    fun onEditorState(json: String) = post { it.onEditorState(json) }

    @JavascriptInterface
    fun onEditorContent(id: Int, text: String) = post { it.onContent(id, text) }

    @JavascriptInterface
    fun onEditorError(id: Int, message: String) = post { it.onContent(id, null) }

    @JavascriptInterface
    fun onEditorDraft(text: String) = post { it.onDraft(text) }

    @JavascriptInterface
    fun onScrollRatio(ratio: String) = post { it.onScroll(ratio.toFloatOrNull() ?: 0f) }

    @JavascriptInterface
    fun onEditorShortcut(name: String) = post { it.onShortcut(name) }
}

/**
 * The Markdown / TXT document surface used for both reading and in-place editing. The same page
 * becomes editable, so edit mode looks exactly like preview mode.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderDocumentView(
    documentKey: String,
    kind: ReaderKind,
    text: String,
    appearance: ReaderAppearance,
    appDark: Boolean,
    appFontSetting: String,
    tabSize: Int,
    controller: ReaderController,
    editMode: ReaderEditMode?,
    searchQuery: String,
    searchTriggerNext: Int,
    searchTriggerPrev: Int,
    onSearchResultCount: (Int, Int, Boolean) -> Unit,
    onSingleTap: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onDraftChanged: (String) -> Unit,
    onShortcut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolved = remember(appearance, appDark, appFontSetting, kind) { appearance.resolve(appDark, appFontSetting, kind) }
    val latestResolved by rememberUpdatedState(resolved)
    val latestTabSize by rememberUpdatedState(tabSize)
    val latestSearchCallback by rememberUpdatedState(onSearchResultCount)
    val latestTapCallback by rememberUpdatedState(onSingleTap)
    val latestLinkCallback by rememberUpdatedState(onLinkClicked)
    val latestCopyCallback by rememberUpdatedState(onCopyText)
    val latestDraftCallback by rememberUpdatedState(onDraftChanged)
    val latestShortcutCallback by rememberUpdatedState(onShortcut)

    var page by remember(controller) { mutableStateOf<String?>(null) }
    var loadedKey by remember(controller) { mutableStateOf<String?>(null) }

    LaunchedEffect(controller, documentKey, kind, text) {
        val restore = if (loadedKey == documentKey) controller.lastScrollRatio else 0f
        val assets = withContext(Dispatchers.IO) { ReaderAssets.load(context) }
        val html = withContext(Dispatchers.Default) {
            val body = when (kind) {
                ReaderKind.MARKDOWN -> ReaderHtml.markdownBody(text).html
                ReaderKind.TEXT -> ReaderHtml.plainTextBody(text)
                ReaderKind.MARKDOWN_SOURCE -> ReaderHtml.sourceBody(text)
            }
            ReaderHtml.page(kind, body, text, assets.first, assets.second, latestResolved, latestTabSize, restore)
        }
        loadedKey = documentKey
        page = html
    }

    val events = remember(controller) {
        object : ReaderEvents {
            override fun onReady() = controller.markReady()
            override fun onSearchResult(count: Int, index: Int, truncated: Boolean) = latestSearchCallback(count, index, truncated)
            override fun onTap() = latestTapCallback()
            override fun onLink(url: String) = latestLinkCallback(url)
            override fun onCopy(text: String) = latestCopyCallback(text)
            override fun onEditorState(json: String) = controller.applyEditorJson(json)
            override fun onContent(id: Int, text: String?) = controller.deliverContent(id, text)
            override fun onDraft(text: String) = latestDraftCallback(text)
            override fun onScroll(ratio: Float) {
                controller.lastScrollRatio = ratio
            }
            override fun onShortcut(name: String) = latestShortcutCallback(name)
        }
    }

    val latestEvents by rememberUpdatedState(events)

    // Appearance is applied live; the reading position is kept by the page.
    LaunchedEffect(controller, resolved.json, controller.readyGeneration) {
        if (controller.isReady) controller.applyAppearance(resolved.json)
    }
    SideEffect { controller.webView?.setBackgroundColor(resolved.backgroundArgb) }

    // Search: a new query (or a reloaded page) re-runs the search; the triggers step through matches.
    var issuedQuery by remember(controller) { mutableStateOf<String?>(null) }
    var issuedGeneration by remember(controller) { mutableIntStateOf(-1) }
    var issuedNext by remember(controller) { mutableIntStateOf(0) }
    var issuedPrev by remember(controller) { mutableIntStateOf(0) }
    LaunchedEffect(controller, controller.readyGeneration, searchQuery, searchTriggerNext, searchTriggerPrev, editMode) {
        if (!controller.isReady || editMode != null) return@LaunchedEffect
        if (issuedQuery != searchQuery || issuedGeneration != controller.readyGeneration) {
            controller.search(searchQuery)
            issuedQuery = searchQuery
            issuedGeneration = controller.readyGeneration
        } else {
            repeat((searchTriggerNext - issuedNext).coerceAtLeast(0)) { controller.searchNext() }
            repeat((searchTriggerPrev - issuedPrev).coerceAtLeast(0)) { controller.searchPrev() }
        }
        issuedNext = searchTriggerNext
        issuedPrev = searchTriggerPrev
    }

    // Keep the page's edit mode in sync with the screen.
    LaunchedEffect(controller, controller.readyGeneration, editMode) {
        if (!controller.isReady) return@LaunchedEffect
        when (editMode) {
            null -> controller.exitEdit()
            ReaderEditMode.RICH -> controller.enterEdit(plain = false)
            ReaderEditMode.PLAIN -> controller.enterEdit(plain = true)
        }
    }

    Box(modifier = modifier.background(Color(resolved.backgroundArgb))) {
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(latestResolved.backgroundArgb)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    addJavascriptInterface(ReaderBridge { latestEvents }, "FCBridge")
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString().orEmpty()
                            return !(url.startsWith("about:") || url.startsWith("data:"))
                        }
                    }
                    controller.webView = this
                }
            },
            update = { view ->
                controller.webView = view
                val html = page
                if (html != null && view.tag !== html) {
                    view.tag = html
                    controller.markLoading()
                    view.setBackgroundColor(latestResolved.backgroundArgb)
                    view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            onRelease = { view ->
                if (controller.webView === view) controller.webView = null
                view.removeJavascriptInterface("FCBridge")
                view.stopLoading()
                view.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )
        if (controller.readyGeneration == 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(resolved.backgroundArgb)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.component.PremiumLoadingIndicator(
                    text = if (kind == ReaderKind.MARKDOWN) "Preparing document..." else "Preparing text..."
                )
            }
        }
    }
}
