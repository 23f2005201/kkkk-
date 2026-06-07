package com.yourapp.docreader.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yourapp.docreader.data.DocumentModel
import com.yourapp.docreader.utils.DocxToHtmlConverter
import com.yourapp.docreader.data.DocType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeViewerScreen(
    document: DocumentModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Run the conversion in the background when the file opens
    LaunchedEffect(document) {
        htmlContent = DocxToHtmlConverter.convertDocxToHtml(context, document.uri)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = document.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                htmlContent?.let { html ->
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.useWideViewPort = true
                                
                                // Render string data safely as standard UTF-8 HTML
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

LaunchedEffect(document) {
    htmlContent = when (document.type) {
        DocType.DOCX -> DocxToHtmlConverter.convertDocxToHtml(context, document.uri)
        DocType.TXT -> DocxToHtmlConverter.convertTxtToHtml(context, document.uri)
        DocType.EPUB -> DocxToHtmlConverter.convertEpubToHtml(context, document.uri)
        else -> "<html><body><p>Unsupported file type</p></body></html>"
    }
    isLoading = false
}
