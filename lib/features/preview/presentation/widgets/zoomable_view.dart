import 'package:flutter/material.dart';

/// Reusable wrapper to add pinch-to-zoom to text-based preview screens.
class ZoomableView extends StatefulWidget {
  final Widget child;
  const ZoomableView({super.key, required this.child});

  @override
  State<ZoomableView> createState() => _ZoomableViewState();
}

class _ZoomableViewState extends State<ZoomableView> {
  final TransformationController _controller = TransformationController();
  TapDownDetails? _doubleTapDetails;
  bool _panEnabled = false;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onScaleChanged);
  }

  void _onScaleChanged() {
    final scale = _controller.value.getMaxScaleOnAxis();
    final shouldPan = scale > 1.0;
    if (_panEnabled != shouldPan && mounted) {
      setState(() {
        _panEnabled = shouldPan;
      });
    }
  }

  @override
  void dispose() {
    _controller.removeListener(_onScaleChanged);
    _controller.dispose();
    super.dispose();
  }

  void _handleDoubleTapDown(TapDownDetails details) {
    _doubleTapDetails = details;
  }

  void _handleDoubleTap() {
    final position = _doubleTapDetails?.localPosition;

    if (_controller.value.getMaxScaleOnAxis() > 1.0) {
      _controller.value = Matrix4.identity();
    } else {
      final zoom = Matrix4.identity();
      if (position != null) {
        zoom.translate(position.dx, position.dy, 0.0);
        zoom.scale(2.5, 2.5, 1.0);
        zoom.translate(-position.dx, -position.dy, 0.0);
      } else {
        zoom.scale(2.5, 2.5, 1.0);
      }
      _controller.value = zoom;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onDoubleTapDown: _handleDoubleTapDown,
      onDoubleTap: _handleDoubleTap,
      child: InteractiveViewer(
        transformationController: _controller,
        boundaryMargin: const EdgeInsets.all(double.infinity),
        minScale: 0.5,
        maxScale: 10.0,
        panEnabled: _panEnabled,
        scaleEnabled: true,
        constrained: true,
        child: SizedBox(
          width: double.infinity,
          height: double.infinity,
          child: widget.child,
        ),
      ),
    );
  }
}
