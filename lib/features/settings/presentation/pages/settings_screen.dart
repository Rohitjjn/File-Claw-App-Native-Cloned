import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/themes/claude_colors.dart';
import '../../../../core/widgets/claude_app_bar.dart';
import '../../../../core/widgets/claude_card.dart';
import '../../../../core/widgets/claude_dialog.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../models/file_type.dart';
import '../../../../services/editor_cache_repository.dart';
import '../../../history/presentation/providers/history_provider.dart';
import '../providers/settings_provider.dart';
/// All app preferences live here. Each row is a Claude-styled card section
/// with 1px borders and ample spacing.
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cfg = ref.watch(settingsProvider);
    final notifier = ref.read(settingsProvider.notifier);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            ClaudeAppBar(
              leading: IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => Navigator.of(context).maybePop(),
              ),
              title: const Text('Settings'),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                children: [
                  // ─── Appearance ─────────────────────────────────
                  const SectionHeader(label: 'Appearance', padding: EdgeInsets.fromLTRB(4, 8, 4, 8)),
                  ClaudeCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        _RadioRow<AppThemeMode>(
                          title: 'Light',
                          value: AppThemeMode.light,
                          group: cfg.themeMode,
                          onChanged: notifier.setThemeMode,
                        ),
                        const _Divider(),
                        _RadioRow<AppThemeMode>(
                          title: 'Dark',
                          value: AppThemeMode.dark,
                          group: cfg.themeMode,
                          onChanged: notifier.setThemeMode,
                        ),
                        const _Divider(),
                        _RadioRow<AppThemeMode>(
                          title: 'System',
                          value: AppThemeMode.system,
                          group: cfg.themeMode,
                          onChanged: notifier.setThemeMode,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  ClaudeCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Font Size',
                            style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15)),
                        const SizedBox(height: 12),
                        _SegmentedRow<double>(
                          options: const [
                            _Seg(0.9, 'Small'),
                            _Seg(1.0, 'Medium'),
                            _Seg(1.1, 'Large'),
                          ],
                          value: cfg.fontScale,
                          onChanged: notifier.setFontScale,
                        ),
                      ],
                    ),
                  ),

                  // ─── Editor ─────────────────────────────────────
                  const SectionHeader(label: 'Editor', padding: EdgeInsets.fromLTRB(4, 24, 4, 8)),
                  ClaudeCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        _SwitchRow(
                          title: 'Show Line Numbers',
                          value: cfg.showLineNumbers,
                          onChanged: notifier.setShowLineNumbers,
                        ),
                        const _Divider(),
                        _SwitchRow(
                          title: 'Word Wrap',
                          value: cfg.wordWrap,
                          onChanged: notifier.setWordWrap,
                        ),
                        const _Divider(),
                        _SwitchRow(
                          title: 'Auto-save Drafts',
                          value: cfg.autoSaveDrafts,
                          onChanged: notifier.setAutoSaveDrafts,
                        ),
                        const _Divider(),
                        _SwitchRow(
                          title: 'Default to Edit on open',
                          value: cfg.defaultOpenMode == OpenMode.edit,
                          onChanged: (v) => notifier.setDefaultOpenMode(
                              v ? OpenMode.edit : OpenMode.preview),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  ClaudeCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Tab Size',
                            style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15)),
                        const SizedBox(height: 12),
                        _SegmentedRow<int>(
                          options: const [
                            _Seg(2, '2 spaces'),
                            _Seg(4, '4 spaces'),
                          ],
                          value: cfg.tabSize,
                          onChanged: notifier.setTabSize,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  ClaudeCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Default Encoding',
                            style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15)),
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: const ['utf-8', 'utf-16', 'ascii', 'iso-8859-1']
                              .map((enc) => _ChoiceChip(
                                    label: enc.toUpperCase(),
                                    selected: cfg.defaultEncoding == enc,
                                    onSelected: () =>
                                        notifier.setDefaultEncoding(enc),
                                  ))
                              .toList(),
                        ),
                      ],
                    ),
                  ),


                  // ─── Storage & History ──────────────────────────
                  const SectionHeader(label: 'Storage & History', padding: EdgeInsets.fromLTRB(4, 24, 4, 8)),
                  ClaudeCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('History Limit',
                            style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15)),
                        const SizedBox(height: 12),
                        _SegmentedRow<int>(
                          options: const [
                            _Seg(10, '10'),
                            _Seg(20, '20'),
                            _Seg(50, '50'),
                            _Seg(100, '100'),
                          ],
                          value: cfg.historyLimit,
                          onChanged: (v) async {
                            await notifier.setHistoryLimit(v);
                            await ref
                                .read(historyProvider.notifier)
                                .applyLimit(v);
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  ClaudeCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        _ActionRow(
                          icon: Icons.history_toggle_off_outlined,
                          title: 'Clear File History',
                          destructive: true,
                          onTap: () async {
                            final ok = await ClaudeConfirmDialog.show(
                              context,
                              title: 'Clear file history?',
                              message:
                                  'Removes every entry from the sidebar. Files on your device are not affected.',
                              confirmLabel: 'Clear',
                              destructive: true,
                            );
                            if (ok) {
                              await ref
                                  .read(historyProvider.notifier)
                                  .clear();
                            }
                          },
                        ),
                        const _Divider(),
                        _ActionRow(
                          icon: Icons.cleaning_services_outlined,
                          title: 'Clear Editor Cache',
                          destructive: true,
                          onTap: () async {
                            final ok = await ClaudeConfirmDialog.show(
                              context,
                              title: 'Clear editor cache?',
                              message:
                                  'Removes all cached drafts and undo history.',
                              confirmLabel: 'Clear',
                              destructive: true,
                            );
                            if (ok) {
                              await EditorCacheRepository.instance.clearAll();
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('Editor cache cleared')),
                                );
                              }
                            }
                          },
                        ),
                      ],
                    ),
                  ),

                  // ─── About ──────────────────────────────────────
                  const SectionHeader(label: 'About', padding: EdgeInsets.fromLTRB(4, 24, 4, 8)),
                  ClaudeCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        _AboutRow(),
                        const _Divider(),
                        _ActionRow(
                          icon: Icons.gavel_outlined,
                          title: 'Open Source Licenses',
                          onTap: () => showLicensePage(
                            context: context,
                            applicationName: AppConstants.appName,
                            applicationVersion: AppConstants.appVersion,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ---- helper widgets ----

class _Divider extends StatelessWidget {
  const _Divider();
  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      height: 1,
      color: isDark ? ClaudeColors.darkDivider : ClaudeColors.lightDivider,
    );
  }
}

class _RadioRow<T> extends StatelessWidget {
  final String title;
  final T value;
  final T group;
  final ValueChanged<T> onChanged;

  const _RadioRow({
    required this.title,
    required this.value,
    required this.group,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondary = isDark ? ClaudeColors.darkTextSecondary : ClaudeColors.lightTextSecondary;
    final selected = value == group;
    return InkWell(
      onTap: () => onChanged(value),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Icon(
              selected
                  ? Icons.radio_button_checked
                  : Icons.radio_button_off,
              color: selected ? ClaudeColors.primary : secondary,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500)),

                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SwitchRow extends StatelessWidget {
  final String title;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _SwitchRow({
    required this.title,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => onChanged(!value),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500)),

                ],
              ),
            ),
            Switch.adaptive(value: value, onChanged: onChanged),
          ],
        ),
      ),
    );
  }
}


class _Seg<T> {
  final T value;
  final String label;
  const _Seg(this.value, this.label);
}

class _SegmentedRow<T> extends StatelessWidget {
  final List<_Seg<T>> options;
  final T value;
  final ValueChanged<T> onChanged;

  const _SegmentedRow({
    required this.options,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final border = isDark ? ClaudeColors.darkBorder : ClaudeColors.lightBorder;
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: border, width: 1),
      ),
      child: Row(
        children: options.map((o) {
          final selected = o.value == value;
          return Expanded(
            child: InkWell(
              onTap: () => onChanged(o.value),
              borderRadius: BorderRadius.circular(10),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 10),
                decoration: BoxDecoration(
                  color: selected
                      ? ClaudeColors.primary.withValues(alpha: 0.12)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(10),
                ),
                alignment: Alignment.center,
                child: Text(
                  o.label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: selected ? ClaudeColors.primary : null,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _ChoiceChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onSelected;
  const _ChoiceChip({
    required this.label,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onSelected,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: selected
              ? ClaudeColors.primary.withValues(alpha: 0.12)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: selected
                ? ClaudeColors.primary
                : (isDark
                    ? ClaudeColors.darkBorder
                    : ClaudeColors.lightBorder),
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w500,
            color: selected ? ClaudeColors.primary : null,
          ),
        ),
      ),
    );
  }
}

class _ActionRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool destructive;
  final VoidCallback onTap;

  const _ActionRow({
    required this.icon,
    required this.title,
    this.destructive = false,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondary = isDark
        ? ClaudeColors.darkTextSecondary
        : ClaudeColors.lightTextSecondary;
    final colour = destructive ? ClaudeColors.error : null;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Icon(icon, size: 20, color: colour),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                        color: colour,
                      )),

                ],
              ),
            ),
            Icon(Icons.chevron_right, color: secondary, size: 20),
          ],
        ),
      ),
    );
  }
}

class _AboutRow extends StatefulWidget {
  @override
  State<_AboutRow> createState() => _AboutRowState();
}

class _AboutRowState extends State<_AboutRow> {
  String _version = AppConstants.appVersion;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final info = await PackageInfo.fromPlatform();
      if (mounted) setState(() => _version = info.version);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondary = isDark
        ? ClaudeColors.darkTextSecondary
        : ClaudeColors.lightTextSecondary;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      child: Row(
        children: [
          const Icon(Icons.info_outline, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${AppConstants.appName} v$_version',
                    style: const TextStyle(
                        fontSize: 15, fontWeight: FontWeight.w500)),
                const SizedBox(height: 2),
                Text(AppConstants.appTagline,
                    style: TextStyle(fontSize: 12.5, color: secondary)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
