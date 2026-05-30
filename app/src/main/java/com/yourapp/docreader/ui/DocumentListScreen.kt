package com.yourapp.docreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourapp.docreader.data.DocumentModel
import com.yourapp.docreader.domain.DocumentScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(onDocumentClick: (DocumentModel) -> Unit) {
    val context = LocalContext.current
    val scanner = remember { DocumentScanner(context) }
    var documents by remember { mutableStateOf<List<DocumentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Trigger the background storage scan safely when the screen opens
    LaunchedEffect(Unit) {
        documents = scanner.scanDeviceForDocuments()
        isLoading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("All Documents") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (documents.isEmpty()) {
                Text(
                    text = "No documents found on device.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(documents) { doc ->
                        DocumentItem(document = doc, onClick = { onDocumentClick(doc) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    
                    // MONETIZATION TIP: You can inject a custom Admob Native Ad item 
                    // right here inside the LazyColumn every 5-6 items!
                }
            }
        }
    }
}

@Composable
fun DocumentItem(document: DocumentModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple text-based icon badge based on file type
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = document.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1fr)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "${(document.size / 1024)} KB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
