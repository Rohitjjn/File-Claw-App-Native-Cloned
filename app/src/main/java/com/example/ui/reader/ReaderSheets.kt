package com.example.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.InsertLink
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Viewer appearance
// ---------------------------------------------------------------------------------------------

/** Viewer Appearance for Markdown/TXT: zoom 25-500%, page theme, typeface, size and layout. Live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppearanceSheet(
    appearance: ReaderAppearance,
    kind: ReaderKind,
    appFontSetting: String,
    onChange: (ReaderAppearance) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.10f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        modifier = Modifier.testTag("reader_appearance_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            SheetHeader(
                icon = Icons.Outlined.Palette,
                title = "Viewer appearance",
                subtitle = "Changes apply instantly",
                onClose = onDismissRequest,
                trailing = {
                    TextButton(onClick = { onChange(ReaderAppearance()) }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            )

            SectionLabel("Zoom")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(
                    onClick = { onChange(appearance.copy(zoomPercent = ReaderAppearance.previousZoom(appearance.zoomPercent))) },
                    enabled = appearance.zoomPercent > ReaderAppearance.MIN_ZOOM,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out") }
                Slider(
                    value = appearance.zoomPercent.toFloat(),
                    onValueChange = { value -> onChange(appearance.copy(zoomPercent = value.roundToInt())) },
                    valueRange = ReaderAppearance.MIN_ZOOM.toFloat()..ReaderAppearance.MAX_ZOOM.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                FilledTonalIconButton(
                    onClick = { onChange(appearance.copy(zoomPercent = ReaderAppearance.nextZoom(appearance.zoomPercent))) },
                    enabled = appearance.zoomPercent < ReaderAppearance.MAX_ZOOM,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${appearance.zoomPercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Like browser zoom: text and layout scale together",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                listOf(25, 50, 75, 100, 125, 150, 200, 300, 400, 500).forEach { preset ->
                    FilterChip(
                        selected = appearance.zoomPercent == preset,
                        onClick = { onChange(appearance.copy(zoomPercent = preset)) },
                        label = { Text("$preset%") }
                    )
                }
            }

            SectionLabel("Page")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReaderTheme.values().forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        selected = appearance.theme == theme,
                        onClick = { onChange(appearance.copy(theme = theme)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionLabel("Typeface")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                ReaderFont.values().forEach { font ->
                    FilterChip(
                        selected = appearance.font == font,
                        onClick = { onChange(appearance.copy(font = font)) },
                        label = {
                            Text(
                                text = if (font == ReaderFont.AUTO) (if (kind == ReaderKind.MARKDOWN) "Default (Sans)" else "Default (Mono)") else font.label,
                                fontFamily = when (font) {
                                    ReaderFont.SERIF -> FontFamily.Serif
                                    ReaderFont.MONO -> FontFamily.Monospace
                                    else -> FontFamily.SansSerif
                                }
                            )
                        }
                    )
                }
            }

            SectionLabel("Text size")
            val size = appearance.effectiveFontSize(appFontSetting, kind)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(
                    onClick = { onChange(appearance.copy(fontSizePx = (size - 1).coerceAtLeast(ReaderAppearance.MIN_FONT_PX))) },
                    enabled = size > ReaderAppearance.MIN_FONT_PX,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.TextDecrease, contentDescription = "Smaller text") }
                Text(
                    text = "$size px",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                FilledTonalIconButton(
                    onClick = { onChange(appearance.copy(fontSizePx = (size + 1).coerceAtMost(ReaderAppearance.MAX_FONT_PX))) },
                    enabled = size < ReaderAppearance.MAX_FONT_PX,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.TextIncrease, contentDescription = "Larger text") }
                Spacer(Modifier.weight(1f))
                if (appearance.fontSizePx != 0) {
                    TextButton(onClick = { onChange(appearance.copy(fontSizePx = 0)) }) { Text("Use app size") }
                }
            }

            SectionLabel("Line spacing")
            ChipRow(
                options = listOf(1.3f to "Compact", 1.6f to "Normal", 1.9f to "Relaxed", 2.2f to "Airy"),
                isSelected = { abs(appearance.lineSpacing - it) < 0.05f },
                onSelect = { onChange(appearance.copy(lineSpacing = it)) }
            )

            SectionLabel("Margins")
            ChipRow(
                options = listOf(4 to "None", 10 to "Narrow", 16 to "Normal", 32 to "Wide"),
                isSelected = { appearance.marginPx == it },
                onSelect = { onChange(appearance.copy(marginPx = it)) }
            )

            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = "Comfortable line width",
                subtitle = "Keeps lines short on wide screens and tablets",
                checked = appearance.readableWidth,
                onCheckedChange = { onChange(appearance.copy(readableWidth = it)) }
            )
            SwitchRow(
                title = "Justify text",
                subtitle = "Align both edges like a printed page",
                checked = appearance.justify,
                onCheckedChange = { onChange(appearance.copy(justify = it)) }
            )
            if (kind == ReaderKind.MARKDOWN) {
                SwitchRow(
                    title = "Wrap code blocks",
                    subtitle = "Long code lines wrap instead of scrolling sideways",
                    checked = appearance.wrapCode,
                    onCheckedChange = { onChange(appearance.copy(wrapCode = it)) }
                )
            } else {
                SwitchRow(
                    title = "Wrap long lines",
                    subtitle = "Turn off to keep logs and tables on one line",
                    checked = appearance.wrapText,
                    onCheckedChange = { onChange(appearance.copy(wrapText = it)) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ReaderTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val fill = if (theme == ReaderTheme.AUTO) {
        Brush.horizontalGradient(
            0f to Color(ReaderTheme.LIGHT.background), 0.5f to Color(ReaderTheme.LIGHT.background),
            0.5f to Color(ReaderTheme.DARK.background), 1f to Color(ReaderTheme.DARK.background)
        )
    } else {
        Brush.linearGradient(listOf(Color(theme.background), Color(theme.background)))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clickable(onClick = onClick)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(fill)
                .border(
                    if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape
                )
        ) {
            Text(
                text = "Aa",
                fontWeight = FontWeight.Bold,
                color = if (theme == ReaderTheme.AUTO) Color(0xFF8E8B82) else Color(theme.foreground)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = theme.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipRow(options: List<Pair<T, String>>, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        options.forEach { (value, label) ->
            FilterChip(selected = isSelected(value), onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SheetHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Edit history
// ---------------------------------------------------------------------------------------------

/** Every checkpoint of the current editing session; tapping one restores the document to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderHistorySheet(
    state: ReaderEditorState,
    onJump: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val now = remember(state.history, state.historyIndex) { System.currentTimeMillis() }
    val clockFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        modifier = Modifier.testTag("reader_history_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            val changes = (state.history.size - 1).coerceAtLeast(0)
            SheetHeader(
                icon = Icons.Outlined.History,
                title = "Edit history",
                subtitle = if (changes == 0) "No changes yet" else "$changes ${if (changes == 1) "change" else "changes"} \u2022 tap a step to go back to it",
                onClose = onDismissRequest
            )
            Spacer(Modifier.height(12.dp))
            if (changes == 0) {
                Text(
                    text = "Every edit you make appears here, newest first. You can jump back to any step, then forward again.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val entries = remember(state.history) { state.history.mapIndexed { index, entry -> index to entry }.reversed() }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                ) {
                    items(entries, key = { it.first }) { (index, entry) ->
                        val current = index == state.historyIndex
                        val undone = index > state.historyIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                            border = if (current) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            modifier = Modifier.fillMaxWidth().clickable { onJump(index) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = historyIcon(entry.label, index),
                                    contentDescription = null,
                                    tint = if (undone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.label,
                                        fontSize = 14.sp,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                        fontStyle = if (undone) FontStyle.Italic else FontStyle.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (undone) 0.5f else 1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val time = ReaderTextStats.relativeTime(entry.time, now) { clockFormat.format(java.util.Date(it)) }
                                    Text(
                                        text = when {
                                            index == 0 -> "When editing started"
                                            undone -> "$time \u2022 undone"
                                            else -> time
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                                if (index > 0 && entry.delta != 0) {
                                    Text(
                                        text = if (entry.delta > 0) "+${entry.delta}" else "\u2212${-entry.delta}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.delta > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                if (current) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = "Now",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.historyIndex != 0) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { onJump(0) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Go back to the original")
                    }
                }
            }
        }
    }
}

private fun historyIcon(label: String, index: Int): ImageVector = when {
    index == 0 -> Icons.Outlined.InsertDriveFile
    label.startsWith("Deleted") || label.startsWith("Removed space") -> Icons.AutoMirrored.Outlined.Backspace
    label.startsWith("Bold") || label.startsWith("Removed bold") -> Icons.Outlined.FormatBold
    label.startsWith("Italic") || label.startsWith("Removed italic") -> Icons.Outlined.FormatItalic
    label.startsWith("Strikethrough") -> Icons.Outlined.FormatStrikethrough
    label.startsWith("Heading") || label.startsWith("Normal text") -> Icons.Outlined.Title
    label.startsWith("Bulleted") -> Icons.AutoMirrored.Outlined.FormatListBulleted
    label.startsWith("Numbered") -> Icons.Outlined.FormatListNumbered
    label.startsWith("Checklist") || label.startsWith("Checked") || label.startsWith("Unchecked") -> Icons.Outlined.CheckBox
    label.startsWith("Quote") -> Icons.Outlined.FormatQuote
    label.startsWith("Code block") -> Icons.Outlined.DataObject
    label.startsWith("Inline code") -> Icons.Outlined.Code
    label.startsWith("Link") -> Icons.Outlined.InsertLink
    label.startsWith("Divider") -> Icons.Outlined.HorizontalRule
    label.contains("indent") || label.startsWith("Tab") -> Icons.AutoMirrored.Outlined.FormatIndentIncrease
    else -> Icons.Outlined.EditNote
}

// ---------------------------------------------------------------------------------------------
// Edit toolbar (replaces the preview tray in place) and the edit 3-dot menu
// ---------------------------------------------------------------------------------------------

/** Undo, Redo, Tab, Bold, Italic, Save and History, in the same floating tray as preview mode. */
@Composable
fun RowScope.ReaderEditToolbarItems(
    state: ReaderEditorState,
    saving: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTab: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSave: () -> Unit,
    onHistory: () -> Unit
) {
    ToolbarIcon(Icons.AutoMirrored.Outlined.Undo, "Undo", enabled = state.canUndo, onClick = onUndo)
    ToolbarIcon(Icons.AutoMirrored.Outlined.Redo, "Redo", enabled = state.canRedo, onClick = onRedo)
    ToolbarIcon(Icons.AutoMirrored.Outlined.KeyboardTab, "Tab", onClick = onTab)
    ToolbarIcon(Icons.Outlined.FormatBold, "Bold", active = state.bold, onClick = onBold)
    ToolbarIcon(Icons.Outlined.FormatItalic, "Italic", active = state.italic, onClick = onItalic)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !saving, onClick = onSave)
            .testTag("reader_save_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(Icons.Outlined.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    ToolbarIcon(Icons.Outlined.History, "Edit history", onClick = onHistory)
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(19.dp),
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private data class ToolSpec(val label: String, val command: String, val icon: ImageVector? = null, val glyph: String? = null)

/**
 * Content of the 3-dot menu while editing. Rich Markdown gets a formatting grid; plain text
 * (TXT or Markdown source) gets text tools. Call inside a DropdownMenu.
 */
@Composable
fun ReaderEditMenuContent(
    kind: ReaderKind,
    plainMode: Boolean,
    onCommand: (String) -> Unit,
    onInsertLink: () -> Unit,
    onInsertDateTime: () -> Unit,
    onToggleSource: (() -> Unit)?,
    onSaveCopy: () -> Unit,
    onDiscard: () -> Unit
) {
    if (!plainMode) {
        MenuLabel("Text style")
        ToolGrid(
            listOf(
                ToolSpec("Normal", "paragraph", glyph = "\u00b6"),
                ToolSpec("Heading 1", "h1", glyph = "H1"),
                ToolSpec("Heading 2", "h2", glyph = "H2"),
                ToolSpec("Heading 3", "h3", glyph = "H3")
            ),
            onCommand
        )
        MenuLabel("Lists & blocks")
        ToolGrid(
            listOf(
                ToolSpec("Bullets", "ul", Icons.AutoMirrored.Outlined.FormatListBulleted),
                ToolSpec("Numbers", "ol", Icons.Outlined.FormatListNumbered),
                ToolSpec("Checklist", "task", Icons.Outlined.CheckBox),
                ToolSpec("Quote", "quote", Icons.Outlined.FormatQuote)
            ),
            onCommand
        )
        MenuLabel("Insert & format")
        ToolGrid(
            listOf(
                ToolSpec("Code block", "codeblock", Icons.Outlined.DataObject),
                ToolSpec("Inline code", "code", Icons.Outlined.Code),
                ToolSpec("Link", "link", Icons.Outlined.InsertLink),
                ToolSpec("Divider", "hr", Icons.Outlined.HorizontalRule)
            ),
            onCommand = { if (it == "link") onInsertLink() else onCommand(it) }
        )
        ToolGrid(
            listOf(
                ToolSpec("Strike", "strike", Icons.Outlined.FormatStrikethrough),
                ToolSpec("Indent", "indent", Icons.AutoMirrored.Outlined.FormatIndentIncrease),
                ToolSpec("Outdent", "outdent", Icons.AutoMirrored.Outlined.FormatIndentDecrease),
                ToolSpec("Clear", "clear", Icons.Outlined.FormatClear)
            ),
            onCommand
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        if (onToggleSource != null) {
            DropdownMenuItem(
                text = { Text("Edit Markdown source") },
                leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                onClick = onToggleSource
            )
        }
    } else {
        DropdownMenuItem(
            text = { Text("Insert date & time") },
            leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null) },
            onClick = onInsertDateTime
        )
        DropdownMenuItem(
            text = { Text("UPPERCASE selection") },
            leadingIcon = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
            onClick = { onCommand("upper") }
        )
        DropdownMenuItem(
            text = { Text("lowercase selection") },
            leadingIcon = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
            onClick = { onCommand("lower") }
        )
        DropdownMenuItem(
            text = { Text("Title Case selection") },
            leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) },
            onClick = { onCommand("title") }
        )
        DropdownMenuItem(
            text = { Text("Indent lines") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatIndentIncrease, contentDescription = null) },
            onClick = { onCommand("indent") }
        )
        DropdownMenuItem(
            text = { Text("Outdent lines") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatIndentDecrease, contentDescription = null) },
            onClick = { onCommand("outdent") }
        )
        DropdownMenuItem(
            text = { Text("Trim trailing spaces") },
            leadingIcon = { Icon(Icons.Outlined.FormatClear, contentDescription = null) },
            onClick = { onCommand("trim") }
        )
        DropdownMenuItem(
            text = { Text("Tabs to spaces") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.KeyboardTab, contentDescription = null) },
            onClick = { onCommand("tabs") }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        if (kind == ReaderKind.MARKDOWN_SOURCE && onToggleSource != null) {
            DropdownMenuItem(
                text = { Text("Back to rich editor") },
                leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                onClick = onToggleSource
            )
        }
    }
    DropdownMenuItem(
        text = { Text("Select all") },
        leadingIcon = { Icon(Icons.Outlined.SelectAll, contentDescription = null) },
        onClick = { onCommand("selectAll") }
    )
    DropdownMenuItem(
        text = { Text("Save a copy\u2026") },
        leadingIcon = { Icon(Icons.Outlined.FileCopy, contentDescription = null) },
        onClick = onSaveCopy
    )
    DropdownMenuItem(
        text = { Text("Discard changes", color = MaterialTheme.colorScheme.error) },
        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        onClick = onDiscard
    )
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToolGrid(tools: List<ToolSpec>, onCommand: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        tools.forEach { tool ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCommand(tool.command) }
                    .padding(vertical = 8.dp)
            ) {
                if (tool.icon != null) {
                    Icon(tool.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                } else {
                    Text(tool.glyph.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tool.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------------------------

@Composable
fun ReaderSaveDialog(
    fileName: String,
    summary: ChangeSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
        title = { Text("Save changes?") },
        text = {
            Column {
                Text("This will overwrite \u201c$fileName\u201d with your edits.")
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("+${summary.addedLines} lines", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("\u2212${summary.removedLines} lines", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${summary.words} words", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "The previous version is kept as a hidden backup next to the file.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm, modifier = Modifier.testTag("reader_save_confirm")) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep editing") } }
    )
}

@Composable
fun ReaderDiscardDialog(fileName: String, onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Discard changes?") },
        text = { Text("Your unsaved edits to \u201c$fileName\u201d will be lost. The file on disk stays as it was.") },
        confirmButton = {
            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("reader_discard_confirm")
            ) { Text("Discard") }
        },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing") } }
    )
}

@Composable
fun ReaderLinkDialog(onInsert: (url: String, text: String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("https://") }
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.InsertLink, contentDescription = null) },
        title = { Text("Insert link") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text("Text (optional, uses the selection)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsert(url.trim(), label.trim()) },
                enabled = url.trim().length > "https://".length || (url.isNotBlank() && !url.trim().startsWith("https://"))
            ) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReaderSaveCopyDialog(defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.FileCopy, contentDescription = null) },
        title = { Text("Save a copy") },
        text = {
            Column {
                Text(
                    "Writes your edited text to a new file next to the original. The original is not changed.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("File name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save copy") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
