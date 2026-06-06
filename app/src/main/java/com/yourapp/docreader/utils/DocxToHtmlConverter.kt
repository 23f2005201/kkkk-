package com.yourapp.docreader.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import java.io.InputStream
import java.util.zip.ZipEntry

object DocxToHtmlConverter {

    suspend fun convertDocxToHtml(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val htmlBuilder = StringBuilder()
        
        // Inject custom CSS to make it look clean, responsive, and mobile-friendly
        htmlBuilder.append("<html><head><style>")
        htmlBuilder.append("body { font-family: 'Roboto', sans-serif; padding: 20px; color: #333333; line-height: 1.6; }")
        htmlBuilder.append("p { margin-bottom: 14px; font-size: 16px; }")
        htmlBuilder.append("h1, h2, h3 { color: #111111; margin-top: 24px; }")
        htmlBuilder.append("</style></head><body>")

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                
                while (entry != null) {
                    // document.xml contains the actual text data inside a docx structure
                    if (entry.name == "word/document.xml") {
                        val reader = BufferedReader(InputStreamReader(zipInputStream))
                        var line: String?
                        val xmlContent = StringBuilder()
                        
                        while (reader.readLine().also { line = it } != null) {
                            xmlContent.append(line)
                        }
                        
                        // Parse the raw XML tags safely into simple HTML elements
                        val parsedHtml = parseXmlToHtml(xmlContent.toString())
                        htmlBuilder.append(parsedHtml)
                        break
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            htmlBuilder.append("<p style='color:red;'>Error loading document formatting.</p>")
        }

        htmlBuilder.append("</body></html>")
        return@withContext htmlBuilder.toString()
    }

    private fun parseXmlToHtml(xml: String): String {
        val result = StringBuilder()
        
        // Regular expressions to target Word's paragraph structure <w:p> and text runs <w:t>
        val paragraphRegex = "<w:p\\b[^>]*>(.*?)</w:p>".toRegex()
        val textRegex = "<w:t\\b[^>]*>(.*?)</w:t>".toRegex()

        val matches = paragraphRegex.findAll(xml)
        for (match in matches) {
            val paragraphContent = match.groupValues[1]
            val textMatches = textRegex.findAll(paragraphContent)
            
            val paragraphText = StringBuilder()
            for (textMatch in textMatches) {
                paragraphText.append(textMatch.groupValues[1])
            }

            if (paragraphText.isNotEmpty()) {
                // Wrap plain text run into clean HTML paragraph
                result.append("<p>").append(paragraphText).append("</p>")
            }
        }
        return result.toString()
    }
}
suspend fun convertTxtToHtml(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<html><head><style>")
    htmlBuilder.append("body { font-family: 'Roboto', sans-serif; padding: 20px; color: #333333; line-height: 1.6; font-size: 16px; }")
    htmlBuilder.append("</style></head><body>")

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Escape basic HTML characters to avoid rendering glitches, then wrap in a paragraph
                    val safeLine = line?.replace("&", "&amp;")?.replace("<", "&lt;")?.replace(">", "&gt;")
                    if (safeLine {
                        if (safeLine.isBlank()) {
                            htmlBuilder.append("<br/>") // Keep empty line spaces
                        } else {
                            htmlBuilder.append("<p>").append(safeLine).append("</p>")
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        htmlBuilder.append("<p style='color:red;'>Error reading text file.</p>")
    }

    htmlBuilder.append("</body></html>")
    return@withContext htmlBuilder.toString()
}

/**
 * Basic local EPUB extractor. It opens the EPUB zip container, finds the main 
 * XHTML/HTML book text chapters, and bundles them into a single string.
 */
suspend fun convertEpubToHtml(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<html><head><style>")
    htmlBuilder.append("body { font-family: 'Roboto', sans-serif; padding: 20px; color: #222222; line-height: 1.7; font-size: 18px; }")
    htmlBuilder.append("p { margin-bottom: 16px; }")
    htmlBuilder.append("</style></head><body>")

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry: ZipEntry? = zipInputStream.nextEntry

            while (entry != null) {
                // EPUB chapters are usually stored in .xhtml, .html, or .htm files inside the zip
                if (entry.name.endsWith(".xhtml") || entry.name.endsWith(".html") || entry.name.endsWith(".htm")) {
                    val reader = BufferedReader(InputStreamReader(zipInputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Stripping out full <html> or <head> tags inside chapters to prevent nested HTML conflicts
                        val cleanLine = line?.replace("<?xml.*?>".toRegex(), "")
                            ?.replace("<html.*?>".toRegex(), "")
                            ?.replace("</html>".toRegex(), "")
                            ?.replace("<body.*?>".toRegex(), "")
                            ?.replace("</body>".toRegex(), "")
                        htmlBuilder.append(cleanLine)
                    }
                }
                entry = zipInputStream.nextEntry
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        htmlBuilder.append("<p style='color:red;'>Error reading eBook content.</p>")
    }

    htmlBuilder.append("</body></html>")
    return@withContext htmlBuilder.toString()
}
