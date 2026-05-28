package com.yourapp.docreader.data

import android.net.Uri

enum class DocType { PDF, DOCX, XLSX, PPTX, EPUB, TXT, UNKNOWN }

data class DocumentModel(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val type: DocType,
    val uri: Uri
)
 
