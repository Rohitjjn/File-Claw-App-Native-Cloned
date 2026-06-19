import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/themes/claude_colors.dart';
import '../../../../core/utils/time_format.dart';
import '../../../../core/widgets/claude_dialog.dart';
import '../../../../core/widgets/claude_list_tile.dart';
import '../../../../core/widgets/empty_state.dart';
import '../../../../core/widgets/file_icon.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../models/file_item.dart';
import '../../../history/presentation/providers/history_provider.dart';

/// Claude-style left navigation drawer: header, history list, bottom anchored
/// settings/theme/about rows.
class SidebarDrawer extends ConsumerWidget {
  final FileItem? activeFile;
  final void Function(FileItem) onFileTap;
  final VoidCallback onNewFile;
  final VoidCallback onOpenSettings;
  final VoidCallback onOpenAbout;
  final VoidCallback onSearch;

  const SidebarDrawer({
    super.key,
    this.activeFile,
    required this.onFileTap,
    required this.onNewFile,
    required this.onOpenSettings,
    required this.onOpenAbout,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final history = ref.watch(historyProvider);
    final width = MediaQuery.of(context).size.width * 0.85;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final divider = isDark ? ClaudeColors.darkDivider : ClaudeColors.lightDivider;

    return Drawer(
      width: width,
      backgroundColor:
          isDark ? ClaudeColors.darkSurface : ClaudeColors.lightSurface,
      shape: const RoundedRectangleBorder(),
      child: SafeArea(
        child: Column(
          children: [
            _Header(onNewFile: onNewFile, onSearch: onSearch),
            Container(height: 1, color: divider),
            Expanded(
              child: Column(
                children: [
                  const SectionHeader(label: 'Recent Files'),
                  Expanded(
                    child: history.isEmpty
                        ? const EmptyStateView(
                            icon: Icons.description_outlined,
                            title: 'No recent files',
                            subtitle: 'Open a file to see it here.',
                            iconSize: 56,
                          )
                        : ListView.builder(
                            padding: EdgeInsets.zero,
                            itemCount: history.length,
                            itemBuilder: (context, i) {
                              final item = history[i];
                              return _HistoryTile(
                                item: item,
                                isActive: activeFile?.path == item.path,
                                onTap: () {
                                  Navigator.of(context).pop();
                                  onFileTap(item);
                                },
                                onRemove: () => ref
                                    .read(historyProvider.notifier)
                                    .remove(item.id),
                              );
                            },
                          ),
                  ),
                ],
              ),
            ),
            Container(height: 1, color: divider),
            const _SidebarFooter(),
          ],
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final VoidCallback onNewFile;
  final VoidCallback onSearch;

  const _Header({required this.onNewFile, required this.onSearch});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: ClaudeColors.primary,
              borderRadius: BorderRadius.circular(12),
            ),
            alignment: Alignment.center,
            child: const Icon(Icons.description_outlined,
                color: Colors.white, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  AppConstants.appName,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                ),
                Text(
                  'Your Files',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Search history',
            icon: const Icon(Icons.search, size: 22, color: ClaudeColors.primary),
            onPressed: onSearch,
          ),
          IconButton(
            tooltip: 'New file',
            icon: const Icon(Icons.add_circle_outline, color: ClaudeColors.primary, size: 24),
            onPressed: onNewFile,
          ),
        ],
      ),
    );
  }
}

class _HistoryTile extends StatelessWidget {
  final FileItem item;
  final bool isActive;
  final VoidCallback onTap;
  final VoidCallback onRemove;

  const _HistoryTile({
    required this.item,
    required this.isActive,
    required this.onTap,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return Slidable(
      key: ValueKey(item.id),
      endActionPane: ActionPane(
        motion: const BehindMotion(),
        extentRatio: 0.25,
        children: [
          SlidableAction(
            onPressed: (_) => onRemove(),
            backgroundColor: ClaudeColors.error,
            foregroundColor: Colors.white,
            icon: Icons.close,
            label: 'Remove',
            borderRadius: BorderRadius.circular(12),
            padding: const EdgeInsets.symmetric(horizontal: 8),
          ),
        ],
      ),
      child: ClaudeListTile(
        leading: FileIconBadge(type: item.type, size: 32),
        title: item.name,
        subtitle: TimeFormat.relative(item.lastOpened),
        trailing: IconButton(
          icon: const Icon(Icons.close, size: 18),
          tooltip: 'Remove from history',
          onPressed: onRemove,
        ),
        isActive: isActive,
        onTap: onTap,
        minHeight: 56,
      ),
    );
  }
}

class _SidebarFooter extends StatelessWidget {
  const _SidebarFooter();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Center(
        child: Text(
          'v${AppConstants.appVersion}',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: Theme.of(context).brightness == Brightness.dark
                ? ClaudeColors.darkTextSecondary
                : ClaudeColors.lightTextSecondary,
          ),
        ),
      ),
    );
  }
}

/// Helper to confirm "Clear History" from anywhere.
Future<bool> confirmClearHistory(BuildContext context) {
  return ClaudeConfirmDialog.show(
    context,
    title: 'Clear file history?',
    message:
        'This will remove all entries from the sidebar. The actual files on your device are not affected.',
    confirmLabel: 'Clear',
    destructive: true,
  );
}
