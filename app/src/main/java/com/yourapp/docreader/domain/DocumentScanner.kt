package com.yourapp.docreader.domain

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.yourapp.docreader.data.DocumentModel
import com.yourapp.docreader.data.DocType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentScanner(private val context: Context) {

    suspend fun scanDeviceForDocuments(): List<DocumentModel> = withContext(Dispatchers.IO) {
        val documentList = mutableListOf<DocumentModel>()
        
        // Query the files table in MediaStore
        val collection = MediaStore.Files.getContentUri("external")
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        // Select files based on typical document extensions
        val selection = ("(" + MediaStore.Files.FileColumns.DATA + " LIKE '%.pdf'" +
                " OR " + MediaStore.Files.FileColumns.DATA + " LIKE '%.docx'" +
                " OR " + MediaStore.Files.FileColumns.DATA + " LIKE '%.xlsx'" +
                " OR " + MediaStore.Files.FileColumns.DATA + " LIKE '%.pptx'" +
                " OR " + MediaStore.Files.FileColumns.DATA + " LIKE '%.epub'" +
                " OR " + MediaStore.Files.FileColumns.DATA + " LIKE '%.txt')")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC" // Latest files first
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown Document"
                val path = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateColumn)
                
                val contentUri = ContentUris.withAppendedId(collection, id)
                val type = getDocType(path)

                if (type != DocType.UNKNOWN) {
                    documentList.add(
                        DocumentModel(id, name, path, size, dateModified, type, contentUri)
                    )
                }
            }
        }
        return@withContext documentList
    }

    private fun getDocType(path: String): DocType {
        val extension = path.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "pdf" -> DocType.PDF
            "docx" -> DocType.DOCX
            "xlsx" -> DocType.XLSX
            "pptx" -> DocType.PPTX
            "epub" -> DocType.EPUB
            "txt" -> DocType.TXT
            else -> DocType.UNKNOWN
        }
    }
}
