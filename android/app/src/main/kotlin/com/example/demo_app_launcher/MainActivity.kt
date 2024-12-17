package com.example.demo_app_launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream

class MainActivity: FlutterActivity()
{
    private val CHANNEL = "app_service"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            try {
                when (call.method) {
                    "getInstalledApps" -> {
                        val packageNames = call.argument<List<String>>("packageNames")
                        if (packageNames == null) {
                            result.error("INVALID_ARGUMENT", "Package names list is null", null)
                            return@setMethodCallHandler
                        }
                        val apps = getSpecificApps(packageNames)
                        result.success(apps)
                    }
                    "launchApp" -> {
                        val packageName = call.argument<String>("packageName")
                        launchApplication(packageName)
                        result.success(null)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            } catch (e: Exception) {
                result.error("ERROR", e.message, e.stackTraceToString())
            }
        }
    }

    private fun getSpecificApps(packageNames: List<String>): List<Map<String, Any>> {
        val pm = packageManager
        val apps = mutableListOf<Map<String, Any>>()

        for (packageName in packageNames) {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val icon = appInfo.loadIcon(pm)
                val iconString = encodeIcon(icon)

                val appData = HashMap<String, Any>()
                appData["packageName"] = packageName
                appData["name"] = pm.getApplicationLabel(appInfo).toString()
                appData["icon"] = iconString
                appData["isInstalled"] = true

                apps.add(appData)
            } catch (e: PackageManager.NameNotFoundException) {
                val appData = HashMap<String, Any>()
                appData["packageName"] = packageName
                appData["name"] = packageName
                appData["icon"] = ""
                appData["isInstalled"] = false

                apps.add(appData)
            }
        }
        
        return apps
    }

    /// Encode icon to base64
    private fun encodeIcon(drawable: Drawable): String {
        try {
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 50
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 50
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
            }

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 50, stream)
            val byteArray = stream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP) 
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun launchApplication(packageName: String?) {
        if (packageName == null) return

        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
