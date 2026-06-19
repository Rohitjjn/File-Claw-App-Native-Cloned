import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/themes/claude_colors.dart';
import '../../../../core/utils/time_format.dart';
import '../../../../core/widgets/claude_app_bar.dart';
import '../../../../core/widgets/claude_button.dart';
import '../../../../core/widgets/claude_dialog.dart';
import '../../../../core/widgets/empty_state.dart';
import '../../../../core/widgets/shimmer_loader.dart';
import '../../../../models/file_item.dart';
import '../../../../models/file_type.dart';
import '../../../../core/utils/file_type_detector.dart';
import '../providers/preview_provider.dart';
import '../widgets/archive_tree_view.dart';
import '../widgets/code_preview.dart';
import '../widgets/hex_preview.dart';
import '../widgets/image_preview.dart';
import '../widgets/markdown_preview.dart';
import '../widgets/text_preview.dart';
import '../../../settings/presentation/providers/settings_provider.dart';
import '../../../history/presentation/providers/history_provider.dart';

/// Renders the appropriate preview widget for the supplied [FileItem],
/// switches into editor mode, or surfaces friendly error states.
class FilePreviewScreen extends ConsumerStatefulWidget {
  final FileItem file;
  const FilePreviewScreen({super.key, required this.file});

  @override
  ConsumerState<FilePreviewScreen> createState() => _FilePreviewScreenState();
}

class _FilePreviewScreenState extends ConsumerState<FilePreviewScreen> {
  bool _scrolled = false;
  final ScrollController _scroll = ScrollController();
  bool _notified = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() async {


    });

    _scroll.addListener(() {
      final next = _scroll.hasClients && _scroll.offset > 4;
      if (next != _scrolled && mounted) setState(() => _scrolled = next);
    });
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _maybeNotifyOpened(FileItem file) {
    if (_notified) return;
    _notified = true;


  }

  Future<void> _share() async {
    try {
      await Share.shareXFiles([XFile(widget.file.path)],
          subject: widget.file.name);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not share this file.')),
        );
      }
    }
  }

  Future<void> _removeFromHistory() async {
    final ok = await ClaudeConfirmDialog.show(
      context,
      title: 'Remove from history?',
      message:
          'This only removes the entry from your sidebar. The file on your device is not affected.',
      confirmLabel: 'Remove',
      destructive: true,
    );
    if (!ok) return;
    await ref.read(historyProvider.notifier).remove(widget.file.id);
    if (mounted) Navigator.of(context).pop();
  }

  void _openEditor() {
    if (!widget.file.isEditable) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text(
                'This file type or size is not editable. Opening in read-only preview.')),
      );
      return;
    }
    Navigator.of(context).pushNamed('/editor', arguments: widget.file);
  }

  @override
  Widget build(BuildContext context) {
    final asyncPreview = ref.watch(previewLoaderProvider(widget.file));
    final cfg = ref.watch(settingsProvider);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            ClaudeAppBar(
              showBottomDivider: _scrolled,
              leading: IconButton(
                icon: const Icon(Icons.arrow_back, color: ClaudeColors.primary),
                onPressed: () => Navigator.of(context).maybePop(),
              ),
              title: Text(widget.file.name,
                  maxLines: 1, overflow: TextOverflow.ellipsis),
              actions: [
                if (widget.file.isEditable)
                  IconButton(
                    tooltip: 'Edit',
                    icon: const Icon(Icons.edit_outlined, color: ClaudeColors.primary),
                    onPressed: _openEditor,
                  ),
                PopupMenuButton<String>(
                  tooltip: 'More',
                  icon: const Icon(Icons.more_vert, color: ClaudeColors.primary),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12)),
                  onSelected: (v) async {
                    switch (v) {
                      case 'share':
                        await _share();
                        break;
                      case 'remove':
                        await _removeFromHistory();
                        break;
                      case 'properties':
                        await _showProperties();
                        break;
                    }
                  },
                  itemBuilder: (_) => const [
                    PopupMenuItem(
                        value: 'share',
                        child: ListTile(
                            leading: Icon(Icons.share_outlined),
                            title: Text('Share'))),
                    PopupMenuItem(
                        value: 'properties',
                        child: ListTile(
                            leading: Icon(Icons.info_outline),
                            title: Text('Properties'))),
                    PopupMenuItem(
                        value: 'remove',
                        child: ListTile(
                            leading: Icon(Icons.history_toggle_off_outlined),
                            title: Text('Remove from history'))),
                  ],
                ),
              ],
            ),
            _MetaBar(file: widget.file),
            Expanded(
              child: asyncPreview.when(
                loading: () => const ShimmerLoader(itemCount: 6),
                error: (e, _) => EmptyStateView(
                  icon: Icons.error_outline,
                  title: 'Could not open file',
                  subtitle: e.toString(),
                ),
                data: (data) {
                  if (data.error != null) {
                    return EmptyStateView(
                      icon: Icons.error_outline,
                      title: 'Could not open file',
                      subtitle: data.error!.message,
                      action: ClaudeSecondaryButton(
                        label: 'Remove from history',
                        icon: Icons.history_toggle_off_outlined,
                        onPressed: _removeFromHistory,
                      ),
                    );
                  }
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    _maybeNotifyOpened(data.file);
                  });
                  return _PreviewBody(
                    file: data.file,
                    text: data.text,
                    bytes: data.bytes,
                    archiveRoot: data.archiveRoot,
                    archivePath: data.file.path,
                    showLineNumbers: cfg.showLineNumbers,
                    wordWrap: cfg.wordWrap,
                    fontScale: cfg.fontScale,
                  );
                },
              ),
            ),
            if (asyncPreview.hasValue && asyncPreview.value!.error == null)
              _PreviewActionBar(
                file: widget.file,
                onEdit: widget.file.isEditable ? _openEditor : null,
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _showProperties() async {
    final f = widget.file;
    int? sizeOnDisk;
    try {
      sizeOnDisk = await File(f.path).length();
    } catch (_) {}
    if (!mounted) return;
    showModalBottomSheet(
      context: context,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey.withValues(alpha: 0.4),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text('Properties',
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            _PropertyRow(label: 'Name', value: f.name),
            _PropertyRow(label: 'Path', value: f.path),
            _PropertyRow(label: 'Type', value: f.type.label),
            _PropertyRow(label: 'Extension', value: f.extension.toUpperCase()),
            _PropertyRow(
                label: 'Size',
                value: sizeOnDisk != null
                    ? '${(sizeOnDisk / 1024).toStringAsFixed(1)} KB'
                    : f.formattedSize),
            _PropertyRow(
                label: 'Modified', value: TimeFormat.dateTime(f.lastModified)),
            _PropertyRow(
                label: 'Last opened',
                value: TimeFormat.relative(f.lastOpened)),
          ],
        ),
      ),
    );
  }
}

class _MetaBar extends StatelessWidget {
  final FileItem file;
  const _MetaBar({required this.file});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondary = isDark
        ? ClaudeColors.darkTextSecondary
        : ClaudeColors.lightTextSecondary;
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
      decoration: BoxDecoration(
        color: isDark
            ? ClaudeColors.darkSurfaceMuted
            : ClaudeColors.lightSurfaceMuted,
        border: Border(
          bottom: BorderSide(
            color: isDark ? ClaudeColors.darkBorder : ClaudeColors.lightBorder,
            width: 1,
          ),
        ),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: ClaudeColors.primary.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(6),
            ),
            child: Text(
              file.extension.toUpperCase().isEmpty
                  ? file.type.label.toUpperCase()
                  : file.extension.toUpperCase(),
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: ClaudeColors.primary,
                letterSpacing: 0.5,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              '${file.formattedSize} • Modified ${TimeFormat.relative(file.lastModified)}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 12, color: secondary),
            ),
          ),
          Text(
            file.isEditable ? 'Editable' : 'Read-only',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: file.isEditable ? ClaudeColors.success : secondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _PreviewBody extends StatelessWidget {
  final FileItem file;
  final String? text;
  final List<int>? bytes;
  final dynamic archiveRoot;
  final String archivePath;
  final bool showLineNumbers;
  final bool wordWrap;
  final double fontScale;

  const _PreviewBody({
    required this.file,
    required this.text,
    required this.bytes,
    required this.archiveRoot,
    required this.archivePath,
    required this.showLineNumbers,
    required this.wordWrap,
    required this.fontScale,
  });

  @override
  Widget build(BuildContext context) {
    switch (file.type) {
      case FileType.markdown:
        return MarkdownPreview(content: text ?? '', fontScale: fontScale);
      case FileType.code:
        return CodePreview(
          content: text ?? '',
          language: FileTypeDetector.syntaxLanguageFor(file.name),
          showLineNumbers: showLineNumbers,
          fontScale: fontScale,
        );
      case FileType.text:
      case FileType.spreadsheet:
        return TextPreview(
          content: text ?? '',
          showLineNumbers: showLineNumbers,
          wordWrap: wordWrap,
          fontScale: fontScale,
        );
      case FileType.image:
        if (bytes == null) {
          return const EmptyStateView(
            icon: Icons.image_outlined,
            title: 'Image not available',
          );
        }
        return ImagePreview(bytes: bytes!);
      case FileType.archive:
        if (archiveRoot == null) {
          return const EmptyStateView(
            icon: Icons.folder_zip_outlined,
            title: 'Archive could not be read',
          );
        }
        return ArchiveTreeView(archivePath: archivePath, root: archiveRoot);
      case FileType.pdf:
      case FileType.docx:
      case FileType.unknown:
        if (bytes == null && text != null) {
          return TextPreview(
            content: text!,
            showLineNumbers: showLineNumbers,
            wordWrap: wordWrap,
            fontScale: fontScale,
          );
        }
        if (bytes != null) {
          return HexPreview(bytes: bytes!, fileName: file.name);
        }
        return EmptyStateView(
          icon: Icons.description_outlined,
          title: '${file.type.label} preview',
          subtitle:
              'Built-in preview is not available for this format. Try opening it with another app.',
        );
    }
  }
}

class _PreviewActionBar extends StatelessWidget {
  final FileItem file;
  final VoidCallback? onEdit;
  const _PreviewActionBar({required this.file, this.onEdit});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
      decoration: BoxDecoration(
        color: isDark ? ClaudeColors.darkBackground : ClaudeColors.lightBackground,
        border: Border(
          top: BorderSide(
            color: isDark ? ClaudeColors.darkDivider : ClaudeColors.lightDivider,
            width: 1,
          ),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: ClaudeSecondaryButton(
              label: 'Properties',
              icon: Icons.info_outline,
              onPressed: () {
                _showFileProperties(context, file);
              },
            ),
          ),
          if (onEdit != null) ...[
            const SizedBox(width: 12),
            Expanded(
              child: ClaudePrimaryButton(
                label: 'Edit',
                icon: Icons.edit_outlined,
                onPressed: onEdit,
                height: 48,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

void _showFileProperties(BuildContext context, FileItem file) {
  showModalBottomSheet(
    context: context,
    backgroundColor: Theme.of(context).scaffoldBackgroundColor,
    shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
    builder: (_) => Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.withValues(alpha: 0.4),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text('Properties', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          _PropertyRow(label: 'Name', value: file.name),
          _PropertyRow(label: 'Path', value: file.path),
          _PropertyRow(label: 'Type', value: file.type.label),
          _PropertyRow(label: 'Extension', value: file.extension.toUpperCase()),
          _PropertyRow(label: 'Size', value: file.formattedSize),
          _PropertyRow(
              label: 'Modified',
              value: TimeFormat.dateTime(file.lastModified)),
          _PropertyRow(
              label: 'Last opened',
              value: TimeFormat.relative(file.lastOpened)),
          const SizedBox(height: 4),
          Text(
            'Editor available for files up to ${(AppConstants.maxFileSizeForEdit / (1024 * 1024)).toStringAsFixed(0)} MB.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    ),
  );
}

class _PropertyRow extends StatelessWidget {
  final String label;
  final String value;
  const _PropertyRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondary = isDark
        ? ClaudeColors.darkTextSecondary
        : ClaudeColors.lightTextSecondary;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(label,
                style: TextStyle(color: secondary, fontSize: 13)),
          ),
          Expanded(
            child: SelectableText(
              value,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
    );
  }
}
