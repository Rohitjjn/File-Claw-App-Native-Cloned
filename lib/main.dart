import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:quick_actions/quick_actions.dart';

import 'services/intent_handler_service.dart';

import 'core/constants/app_constants.dart';
import 'core/themes/app_theme.dart';
import 'features/about/about_screen.dart';
import 'features/editor/presentation/pages/editor_screen.dart';
import 'features/home/presentation/pages/home_screen.dart';
import 'features/preview/presentation/pages/file_preview_screen.dart';
import 'features/search/presentation/pages/search_screen.dart';
import 'features/settings/presentation/pages/settings_screen.dart';
import 'features/settings/presentation/providers/settings_provider.dart';
import 'features/splash/splash_screen.dart';
import 'models/file_item.dart';
import 'services/config_repository.dart';
import 'services/editor_cache_repository.dart';
import 'services/history_repository.dart';

// Global key to control navigation from quick actions
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Pre-warm config so theme is ready before first frame.
  try {
    await ConfigRepository.instance.load();
  } catch (_) {/* fallback to defaults */}

  // Init notifications & prune stale caches in the background.
  // ignore: discarded_futures
  // ignore: discarded_futures
  EditorCacheRepository.instance.pruneStale();

  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  runApp(const ProviderScope(child: FilesClawApp()));
}

class FilesClawApp extends ConsumerStatefulWidget {
  const FilesClawApp({super.key});

  @override
  ConsumerState<FilesClawApp> createState() => _FilesClawAppState();
}

class _FilesClawAppState extends ConsumerState<FilesClawApp> {
  final QuickActions quickActions = const QuickActions();

  @override
  void initState() {
    super.initState();
    _setupQuickActions();
    IntentHandlerService.instance.init();
  }

  @override
  void dispose() {
    IntentHandlerService.instance.dispose();
    super.dispose();
  }

  void _setupQuickActions() {
    quickActions.initialize((String shortcutType) async {
      await Future.delayed(const Duration(milliseconds: 100)); // wait for navigator
      if (shortcutType == 'action_settings') {
        navigatorKey.currentState?.pushNamed('/settings');
      } else if (shortcutType == 'action_history') {
        navigatorKey.currentState?.pushNamed('/home');
      } else if (shortcutType == 'action_last_opened') {
        try {
          // Pre-warm config if needed for HistoryRepository path resolution
          await ConfigRepository.instance.load();
          final hist = await HistoryRepository.instance.load();
          if (hist.isNotEmpty) {
            final last = hist.first;
            navigatorKey.currentState?.pushNamed('/preview', arguments: last);
          } else {
            navigatorKey.currentState?.pushNamed('/home');
          }
        } catch (_) {
          navigatorKey.currentState?.pushNamed('/home');
        }
      }
    });

    quickActions.setShortcutItems(<ShortcutItem>[
      const ShortcutItem(type: 'action_last_opened', localizedTitle: 'Last Opened File', icon: 'icon_file'),
      const ShortcutItem(type: 'action_history', localizedTitle: 'History', icon: 'icon_history'),
      const ShortcutItem(type: 'action_settings', localizedTitle: 'Settings', icon: 'icon_settings'),
    ]);
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    return MaterialApp(
      navigatorKey: navigatorKey,
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      onGenerateRoute: _onGenerateRoute,
      initialRoute: '/',
      builder: (context, child) {
        final brightness = Theme.of(context).brightness;
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: brightness == Brightness.light ? Brightness.dark : Brightness.light,
            statusBarBrightness: brightness,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarIconBrightness: brightness == Brightness.light ? Brightness.dark : Brightness.light,
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
    );
  }

  Route<dynamic>? _onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case '/':
        return _fadeRoute(const SplashScreen(), settings);
      case '/home':
        return _fadeRoute(const HomeScreen(), settings);
      case '/settings':
        return _slideRoute(const SettingsScreen(), settings);
      case '/about':
        return _slideRoute(const AboutScreen(), settings);
      case '/search':
        return _slideRoute(const SearchScreen(), settings);
      case '/preview':
        final file = settings.arguments;
        if (file is! FileItem) {
          return _fadeRoute(const HomeScreen(), settings);
        }
        return _slideRoute(FilePreviewScreen(file: file), settings);
      case '/editor':
        final file = settings.arguments;
        if (file is! FileItem) {
          return _fadeRoute(const HomeScreen(), settings);
        }
        return _slideRoute(EditorScreen(file: file), settings);
    }
    return _fadeRoute(const HomeScreen(), settings);
  }

  PageRouteBuilder _fadeRoute(Widget page, RouteSettings settings) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (_, __, ___) => page,
      transitionDuration: const Duration(milliseconds: 200),
      reverseTransitionDuration: const Duration(milliseconds: 150),
      transitionsBuilder: (_, animation, __, child) =>
          FadeTransition(opacity: animation, child: child),
    );
  }

  PageRouteBuilder _slideRoute(Widget page, RouteSettings settings) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (_, __, ___) => page,
      transitionDuration: const Duration(milliseconds: 230),
      reverseTransitionDuration: const Duration(milliseconds: 180),
      transitionsBuilder: (_, animation, __, child) {
        final offset = Tween<Offset>(
          begin: const Offset(0.05, 0),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.easeOutCubic,
        ));
        return FadeTransition(
          opacity: animation,
          child: SlideTransition(position: offset, child: child),
        );
      },
    );
  }
}
