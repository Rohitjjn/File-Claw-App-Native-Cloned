package com.example.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HeadingItem(
    val level: Int,
    val title: String,
    val lineNumber: Int,
    /** Character offset of the heading in the source, or -1 when only the line is known. */
    val sourceOffset: Int = -1
)

object DocumentOutlineParser {
    /**
     * Parses document headings from markdown/text lines.
     * Recognizes #, ##, ###, ####, etc., or standard title underlines (===, ---).
     */
    fun parseHeadings(content: String): List<HeadingItem> {
        val lines = content.lines()
        val headings = mutableListOf<HeadingItem>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                if (level in 1..6 && line.length > level && line[level] == ' ') {
                    val title = line.substring(level).trim()
                    if (title.isNotEmpty()) {
                        headings.add(HeadingItem(level = level, title = title, lineNumber = i + 1))
                    }
                }
            } else if (i + 1 < lines.size) {
                // Check setext-style underline
                val nextLine = lines[i + 1].trim()
                if (nextLine.length >= 3 && nextLine.all { it == '=' }) {
                    headings.add(HeadingItem(level = 1, title = line, lineNumber = i + 1))
                } else if (nextLine.length >= 3 && nextLine.all { it == '-' }) {
                    headings.add(HeadingItem(level = 2, title = line, lineNumber = i + 1))
                }
            }
        }
        return headings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentOutlineBottomSheet(
    headings: List<HeadingItem>,
    documentTitle: String,
    emptyHint: String,
    onDismissRequest: () -> Unit,
    onSelectHeading: (HeadingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var filter by remember { mutableStateOf("") }
    val visible = remember(headings, filter) {
        val q = filter.trim()
        if (q.isEmpty()) headings else headings.filter { it.title.contains(q, ignoreCase = true) }
    }
    val minLevel = remember(headings) { headings.minOfOrNull { it.level } ?: 1 }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("document_outline_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Toc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Document outline",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (headings.isEmpty()) documentTitle else "${headings.size} sections \u2022 $documentTitle",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close outline", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (headings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No sections found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptyHint,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                if (headings.size > 8) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        placeholder = { Text("Filter sections") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visible) { item ->
                        val depth = (item.level - minLevel).coerceIn(0, 5)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (depth == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (depth * 14).dp)
                                .clickable {
                                    onSelectHeading(item)
                                    onDismissRequest()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (depth) {
                                        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        1 -> Color(0xFFE5A93C).copy(alpha = 0.18f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    }
                                ) {
                                    Text(
                                        text = "H${item.level}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (depth) {
                                            0 -> MaterialTheme.colorScheme.primary
                                            1 -> Color(0xFFB7791F)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        },
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.title,
                                    fontSize = if (depth == 0) 14.sp else 13.sp,
                                    fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "L${item.lineNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
