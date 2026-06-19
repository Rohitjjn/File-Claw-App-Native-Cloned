import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:app_links/app_links.dart';
import 'package:flutter/services.dart';
import '../models/file_item.dart';
import '../main.dart';

class IntentHandlerService {
  IntentHandlerService._();
  static final instance = IntentHandlerService._();

  final _appLinks = AppLinks();
  StreamSubscription<Uri>? _sub;
  static const MethodChannel _channel = MethodChannel('files_claw_content_resolver');

  void init() {
    _sub = _appLinks.uriLinkStream.listen((Uri? uri) {
      if (uri != null) {
        _handleUri(uri);
      }
    });
  }

  void dispose() {
    _sub?.cancel();
  }

  Future<void> _handleUri(Uri uri) async {
    String path = '';

    if (uri.scheme == 'content') {
      try {
        final result = await _channel.invokeMethod<String>('resolveContentUri', {'uri': uri.toString()});
        if (result != null) {
          path = result;
        }
      } catch (e) {
        debugPrint('Error resolving content URI: $e');
      }
    } else {
      try {
         path = uri.toFilePath();
      } catch (_) {
         path = uri.toString();
      }
    }

    if (path.isNotEmpty) {
      final file = File(path);
      final size = file.existsSync() ? file.lengthSync() : 0;
      final fileItem = FileItem.create(
        id: path,
        name: path.split('/').last,
        path: path,
        sizeInBytes: size
      );

      await Future.delayed(const Duration(milliseconds: 500));
      if (navigatorKey.currentState != null) {
        navigatorKey.currentState!.pushNamed('/preview', arguments: fileItem);
      }
    }
  }
}
