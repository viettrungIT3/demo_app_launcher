import 'package:flutter/services.dart';

class AppService {
  static const platform = MethodChannel('app_service');

  static const List<String> commonApps = [
    'com.google.android.youtube', // YouTube
    'com.google.android.gm', // Gmail
    'com.google.android.apps.docs', // Google Drive
    'com.google.android.apps.maps', // Google Maps
    'com.google.android.calendar', // Google Calendar
    'com.google.android.apps.photos', // Google Photos
  ];

  Future<List<Map<String, dynamic>>> getInstalledApps() async {
    try {
      final result = await platform.invokeMethod('getInstalledApps', {
        'packageNames': commonApps,
      });

      if (result is List) {
        return result.map((item) => Map<String, dynamic>.from(item)).toList();
      }
      return [];
    } catch (e) {
      print('Error getting installed apps: $e');
      return [];
    }
  }

  Future<void> launchApp(String packageName) async {
    try {
      await platform.invokeMethod('launchApp', {'packageName': packageName});
    } catch (e) {
      print('Error launching app: $e');
    }
  }
}
