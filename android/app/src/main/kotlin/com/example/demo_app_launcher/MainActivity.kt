package com.example.demo_app_launcher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import android.view.View
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

                apps.add(mapOf(
                    "packageName" to packageName,
                    "name" to pm.getApplicationLabel(appInfo).toString(),
                    "icon" to iconString,
                    "isInstalled" to true
                ))
            } catch (e: PackageManager.NameNotFoundException) {
                apps.add(mapOf(
                    "packageName" to packageName,
                    "name" to packageName,
                    "icon" to "",
                    "isInstalled" to false
                ))
            }
        }
        return apps
    }

    private fun encodeIcon(drawable: Drawable): String {
        try {
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
            }

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
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

    override fun onResume() {
        super.onResume()
        moveTaskToFront()
        checkDefaultLauncher()
    }

    @SuppressLint("ServiceCast")
    private fun moveTaskToFront() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
    }

    override fun onPause() {
        super.onPause()
        // Ngăn chặn việc thoát khỏi ứng dụng
        moveTaskToFront()
    }

    override fun onStop() {
        super.onStop()
        // Khởi động lại activity khi bị dừng
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun checkDefaultLauncher() {
        if (!isDefaultLauncher()) {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        } else {
            moveTaskToFront()
        }
    }

    override fun onBackPressed() {
        // Vô hiệu hóa nút Back
        return
    }
}
