package com.yourapp.docreader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.yourapp.docreader.data.DocumentModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    document: DocumentModel,
    onBackClick: () -> Unit
) {
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
        // AndroidView bridges traditional XML views into Jetpack Compose
        AndroidView(
            factory = { context ->
                PDFView(context, null).apply {
                    // Load the PDF via its system content URI directly
                    fromUri(document.uri)
                        .enableSwipe(true) // Allows vertical or horizontal paging
                        .swipeHorizontal(false)
                        .enableDoubletap(true) // Pinch to zoom capability
                        .defaultPage(0)
                        .load()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
