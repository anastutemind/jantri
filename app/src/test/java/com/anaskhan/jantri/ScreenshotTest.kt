package com.anaskhan.jantri

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.anaskhan.jantri.core.Settings
import com.anaskhan.jantri.ui.DuasScreen
import com.anaskhan.jantri.ui.HomeScreen
import com.anaskhan.jantri.ui.MoonScreen
import com.anaskhan.jantri.ui.JantriTheme
import com.anaskhan.jantri.ui.SettingsScreen
import com.anaskhan.jantri.ui.TableScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Renders the real composables through the real Android runtime (Robolectric native
 * graphics) at phone dimensions and writes the PNGs out. These are the store screenshots;
 * nothing here is drawn by hand, it is the app's own UI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h731dp-xxhdpi")
class ScreenshotTest {

    private val outDir = File(System.getenv("JANTRI_SHOTS") ?: "build/screenshots")
    private val settings = Settings(ramadanStartOverride = "2026-02-19")

    private fun shoot(name: String, content: @Composable () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    JantriTheme(dark = false) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { content() }
                    }
                }
                shadowOf(Looper.getMainLooper()).idle()
                val metrics = activity.resources.displayMetrics
                val w = metrics.widthPixels
                val h = metrics.heightPixels
                val root: View = activity.window.decorView
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, w, h)
                shadowOf(Looper.getMainLooper()).idle()
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, w, h)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                outDir.mkdirs()
                FileOutputStream(File(outDir, "$name.png")).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test
    fun home() = shoot("01-today") {
        HomeScreen(
            settings = settings,
            onOpenSettings = {},
            onOpenTable = {},
            clock = { LocalDateTime.of(2026, 3, 4, 17, 52) },
            ticking = false,
        )
    }

    @Test
    fun table() = shoot("02-jantri") {
        TableScreen(settings = settings, onOpenSettings = {}, todayOverride = LocalDate.of(2026, 3, 4))
    }

    @Test
    fun duas() = shoot("03-duas") { DuasScreen() }

    @Test
    fun moon() = shoot("05-moon") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(420))) {
            MoonScreen(
                settings = settings,
                today = LocalDate.of(2026, 11, 9),
                onUseRamadanStart = {},
            )
        }
    }

    @Test
    fun moonVerdict() = shoot("06-moon-city") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(1780))) {
            MoonScreen(
                settings = settings,
                today = LocalDate.of(2026, 11, 9),
                onUseRamadanStart = {},
            )
        }
    }

    @Test
    fun timings() = shoot("04-timing") {
        SettingsScreen(settings = settings, onChange = {}, onRequestNotifications = {})
    }
}
