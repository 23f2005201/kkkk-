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

/**
 * Converts a spreadsheet (.xlsx) into a responsive, scrollable HTML 
 * grid layout table that mimics Excel architecture.
 */
suspend fun convertXlsxToHtml(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val htmlBuilder = StringBuilder()
    
    // Inject custom stylesheet for spreadsheet rendering
    htmlBuilder.append("<html><head><style>")
    htmlBuilder.append("body { font-family: sans-serif; padding: 10px; background-color: #f4f5f7; }")
    htmlBuilder.append(".table-container { width: 100%; overflow-x: auto; white-space: nowrap; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
    htmlBuilder.append("table { border-collapse: collapse; background-color: #ffffff; min-width: 100%; }")
    htmlBuilder.append("th, td { border: 1px solid #d1d5db; padding: 10px 14px; font-size: 14px; text-align: left; }")
    htmlBuilder.append("tr:nth-child(even) { background-color: #f9fafb; }")
    htmlBuilder.append("th { background-color: #107c41; color: white; font-weight: bold; }") // Excel Green
    htmlBuilder.append("</style></head><body>")
    htmlBuilder.append("<div class='table-container'><table>")

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry: ZipEntry? = zipInputStream.nextEntry
            
            var sharedStrings = listOf<String>()
            val sheetData = StringBuilder()

            // Pass 1: Extract strings database and primary worksheet structural data
            while (entry != null) {
                if (entry.name == "word/document.xml" || entry.name == "xl/sharedStrings.xml") {
                    // Read strings index
                    sharedStrings = parseSharedStrings(zipInputStream)
                }
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    // Extract sheet 1 content
                    val reader = BufferedReader(InputStreamReader(zipInputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sheetData.append(line)
                    }
                }
                entry = zipInputStream.nextEntry
            }

            // Pass 2: Translate grid matrix coordinates into HTML row structures
            if (sheetData.isNotEmpty()) {
                val rowRegex = "<row\\b[^>]*>(.*?)</row>".toRegex()
                val cellRegex = "<c\\b[^>]*>(.*?)</c>".toRegex()
                val valueRegex = "<v>(.*?)</v>".toRegex()

                val rows = rowRegex.findAll(sheetData.toString())
                var isFirstRow = true

                for (rowMatch in rows) {
                    htmlBuilder.append("<tr>")
                    val rowContent = rowMatch.groupValues[1]
                    val cells = cellRegex.findAll(rowContent)

                    for (cellMatch in cells) {
                        val cellContent = cellMatch.groupValues[0] // Full cell tag
                        val valueMatch = valueRegex.find(cellContent)
                        var cellValue = valueMatch?.groupValues?.get(1) ?: ""

                        // Check if the cell references the shared string dictionary index type (t="s")
                        if (cellContent.contains("t=\"s\"") && cellValue.isNotEmpty()) {
                            val index = cellValue.toIntOrNull()
                            if (index != null && index in sharedStrings.indices) {
                                cellValue = sharedStrings[index]
                            }
                        }

                        // Apply header styles to Row 1, standard text styles to subsequent data cells
                        if (isFirstRow) {
                            htmlBuilder.append("<th>").append(cellValue).append("</th>")
                        } else {
                            htmlBuilder.append("<td>").append(cellValue).append("</td>")
                        }
                    }
                    htmlBuilder.append("</tr>")
                    isFirstRow = false
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        htmlBuilder.append("<tr><td>Error loading spreadsheet matrix layout.</td></tr>")
    }

    htmlBuilder.append("</table></div></body></html>")
    return@withContext htmlBuilder.toString()
}

/**
 * Excel optimizes space by collecting all text labels into a single directory layout.
 * This parser indexes that dictionary into an array mapping.
 */
private fun parseSharedStrings(zipStream: ZipInputStream): List<String> {
    val strings = mutableListOf<String>()
    val reader = BufferedReader(InputStreamReader(zipStream))
    val textRegex = "<t\\b[^>]*>(.*?)</t>".toRegex()
    
    var line: String?
    val content = StringBuilder()
    while (reader.readLine().also { line = it } != null) {
        content.append(line)
    }
    
    textRegex.findAll(content.toString()).forEach { match ->
        strings.add(match.groupValues[1])
    }
    return strings
}
