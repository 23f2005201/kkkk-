package com.yourapp.docreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.yourapp.docreader.data.DocumentModel
import com.yourapp.docreader.data.DocType
import com.yourapp.docreader.ui.DocumentListScreen
import com.yourapp.docreader.ui.PdfViewerScreen
import com.yourapp.docreader.ui.theme.DocReaderTheme // Your auto-generated template theme

class MainActivity : ComponentActivity() {
    override fun Bundle? {
        super.onCreate(savedInstanceState)
        setContent {
            DocReaderTheme {
                // Manage which document is currently active/selected
                var selectedDocument by remember { mutableStateOf<DocumentModel?>(null) }

                if (selectedDocument == null) {
                    DocumentListScreen(onDocumentClick = { clickedDoc ->
                        selectedDocument = clickedDoc
                    })
                } else {
                    val doc = selectedDocument!!
                    when (doc.type) {
                        DocType.PDF -> {
                            PdfViewerScreen(
                                document = doc,
                                onBackClick = { selectedDocument = null }
                            )
                        }
                        else -> {
                            // Placeholder for other file format views (DOCX, XLSX, etc.)
                            // For now, it gracefully kicks back to the list screen
                            selectedDocument = null 
                        }
                    }
                }
            }
        }
    }
}
