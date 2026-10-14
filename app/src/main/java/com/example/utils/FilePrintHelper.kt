package com.example.utils

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

/**
 * Native Print and Export helper using Android's PrintManager & PrintDocumentAdapter.
 * Works 100% offline to print or save documents as PDF.
 */
object FilePrintHelper {

    fun printWebView(
        context: Context,
        webView: WebView,
        jobName: String = "FileClaw_Document"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }


    /**
     * Print an existing PDF without reading its binary bytes as text or rasterizing its pages.
     * The system print service receives a valid PDF, and requested page ranges are preserved.
     */
    fun printPdf(
        context: Context,
        pdfFile: File,
        title: String,
        password: String? = null
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        if (!pdfFile.isFile || !pdfFile.canRead()) {
            android.widget.Toast.makeText(context, "PDF file is not readable.", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        PDFBoxResourceLoader.init(context.applicationContext)
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                try {
                    if (cancellationSignal.isCanceled) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val pageCount = PdfDocumentSearch.loadDocument(pdfFile, password).use { it.numberOfPages }
                    val info = PrintDocumentInfo.Builder("$title.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pageCount)
                        .build()
                    callback.onLayoutFinished(info, oldAttributes != newAttributes)
                } catch (error: Exception) {
                    callback.onLayoutFailed(error.localizedMessage ?: "Could not read PDF pages.")
                }
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                try {
                    if (cancellationSignal.isCanceled) {
                        callback.onWriteCancelled()
                        return
                    }
                    PdfDocumentSearch.loadDocument(pdfFile, password).use { source ->
                        val selected = if (pages.any { it.start == 0 && it.end == Int.MAX_VALUE }) {
                            (0 until source.numberOfPages).toList()
                        } else {
                            pages.flatMap { range ->
                                (range.start.coerceAtLeast(0)..range.end.coerceAtMost(source.numberOfPages - 1)).toList()
                            }.distinct().sorted()
                        }

                        if (selected.isEmpty()) {
                            callback.onWriteFinished(emptyArray())
                            return
                        }

                        PDDocument().use { output ->
                            for (pageIndex in selected) {
                                if (cancellationSignal.isCanceled) {
                                    callback.onWriteCancelled()
                                    return
                                }
                                output.importPage(source.getPage(pageIndex))
                            }
                            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { outputStream ->
                                output.save(outputStream)
                            }
                        }
                        callback.onWriteFinished(arrayOf(PageRange(0, selected.size - 1)))
                    }
                } catch (error: Exception) {
                    callback.onWriteFailed(error.localizedMessage ?: "Could not export PDF.")
                }
            }
        }

        printManager.print(
            title,
            adapter,
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    fun printTextOrMarkdown(
        context: Context,
        content: String,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val tempWebView = WebView(context)
        val escaped = content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        font-size: 13px;
                        line-height: 1.6;
                        padding: 24px;
                        color: #1a1a1a;
                    }
                    h2 {
                        margin-top: 0;
                        color: #111;
                        font-size: 18px;
                    }
                    hr {
                        border: none;
                        border-top: 1px solid #ddd;
                        margin: 12px 0 20px 0;
                    }
                    pre {
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                        font-size: 11px;
                        background: #f8f8f8;
                        padding: 12px;
                        border-radius: 6px;
                    }
                </style>
            </head>
            <body>
                <h2>$title</h2>
                <hr>
                <pre>$escaped</pre>
            </body>
            </html>
        """.trimIndent()

        tempWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                printManager.print(
                    title,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
            }
        }
        tempWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun printImage(
        context: Context,
        imageFile: File,
        title: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val tempWebView = WebView(context)
        val imageUri = android.net.Uri.fromFile(imageFile).toString()
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        margin: 0;
                        padding: 16px;
                        text-align: center;
                        font-family: sans-serif;
                    }
                    h3 {
                        margin-top: 0;
                        color: #333;
                    }
                    img {
                        max-width: 100%;
                        max-height: 90vh;
                        height: auto;
                        object-fit: contain;
                        display: block;
                        margin: 0 auto;
                    }
                </style>
            </head>
            <body>
                <h3>$title</h3>
                <img src="$imageUri" />
            </body>
            </html>
        """.trimIndent()

        tempWebView.settings.apply {
            allowFileAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
        }

        tempWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = tempWebView.createPrintDocumentAdapter(title)
                printManager.print(
                    title,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
            }
        }
        tempWebView.loadDataWithBaseURL("file:///", html, "text/html", "UTF-8", null)
    }
}
