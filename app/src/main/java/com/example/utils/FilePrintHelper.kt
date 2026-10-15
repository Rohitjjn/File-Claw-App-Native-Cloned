package com.example.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Matrix
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

/**
 * Native Print and Export helper using Android's PrintManager & PrintDocumentAdapter.
 * Works 100% offline to print or save documents as PDF.
 */
object FilePrintHelper {

    /**
     * Prints the original PDF pages through Android's print framework. It rasterizes one
     * page at a time to keep memory bounded and never treats PDF bytes as plain text.
     */
    fun printPdf(context: Context, pdfFile: File, title: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null || !pdfFile.isFile || !pdfFile.canRead()) {
            android.widget.Toast.makeText(context, "PDF cannot be opened for printing.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val adapter = object : PrintDocumentAdapter() {
            private var pageCount = 0
            private var pageSize = android.util.Size(595, 842)

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled()
                    return
                }
                try {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            pageCount = renderer.pageCount
                            if (pageCount == 0) {
                                callback.onLayoutFailed("This PDF has no printable pages.")
                                return
                            }
                        }
                    }
                    val media = newAttributes.mediaSize
                    if (media != null) pageSize = android.util.Size((media.widthMils * 72 / 1000).coerceAtLeast(1), (media.heightMils * 72 / 1000).coerceAtLeast(1))
                    callback.onLayoutFinished(
                        PrintDocumentInfo.Builder(title)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(pageCount)
                            .build(),
                        oldAttributes != newAttributes
                    )
                } catch (e: Exception) {
                    callback.onLayoutFailed("Unable to read PDF: ${e.localizedMessage ?: "unknown error"}")
                }
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                var document: PdfDocument? = null
                try {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            document = PdfDocument()
                            var outputPageNumber = 0
                            for (pageIndex in 0 until renderer.pageCount) {
                                if (cancellationSignal.isCanceled) {
                                    callback.onWriteCancelled()
                                    return
                                }
                                if (pages.none { pageIndex in it.start..it.end }) continue
                                renderer.openPage(pageIndex).use { sourcePage ->
                                    val targetWidth = pageSize.width.coerceAtLeast(1)
                                    val targetHeight = pageSize.height.coerceAtLeast(1)
                                    val scale = minOf(
                                        targetWidth.toFloat() / sourcePage.width,
                                        targetHeight.toFloat() / sourcePage.height
                                    ).coerceAtMost(1.5f)
                                    val bitmap = Bitmap.createBitmap(
                                        (sourcePage.width * scale).toInt().coerceAtLeast(1),
                                        (sourcePage.height * scale).toInt().coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    try {
                                        sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                        val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, outputPageNumber + 1).create()
                                        val outputPage = document!!.startPage(pageInfo)
                                        val destinationRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
                                        val sourceRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                                        val fitScale = minOf(destinationRect.width() / sourceRect.width(), destinationRect.height() / sourceRect.height())
                                        val matrix = Matrix().apply {
                                            setScale(fitScale, fitScale)
                                            postTranslate(
                                                (destinationRect.width() - sourceRect.width() * fitScale) / 2f,
                                                (destinationRect.height() - sourceRect.height() * fitScale) / 2f
                                            )
                                        }
                                        outputPage.canvas.drawColor(android.graphics.Color.WHITE)
                                        outputPage.canvas.drawBitmap(bitmap, matrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
                                        document!!.finishPage(outputPage)
                                        outputPageNumber++
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                            }
                            if (cancellationSignal.isCanceled) {
                                callback.onWriteCancelled()
                                return
                            }
                            if (outputPageNumber == 0) {
                                callback.onWriteFailed("No selected PDF pages to print.")
                                return
                            }
                            java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                                document!!.writeTo(output)
                            }
                            callback.onWriteFinished(arrayOf(PageRange(0, (outputPageNumber - 1).coerceAtLeast(0))))
                        }
                    }
                } catch (e: Exception) {
                    callback.onWriteFailed("PDF print failed: ${e.localizedMessage ?: "unknown error"}")
                } finally {
                    document?.close()
                }
            }
        }
        try {
            printManager.print(
                title,
                adapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Unable to start PDF printing: ${e.localizedMessage ?: "unknown error"}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

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
