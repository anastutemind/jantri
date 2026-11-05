package com.anaskhan.jantri

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Renders the shipped launcher drawables so the icon can be judged, not assumed. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconRenderTest {

    private val out = File(System.getenv("JANTRI_SHOTS") ?: "build/screenshots")

    @Test
    fun renderLauncherAssets() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        out.mkdirs()
        for ((name, res) in listOf(
            "fg" to R.drawable.ic_launcher_foreground,
            "icon" to R.mipmap.ic_launcher,
        )) {
            val d = ctx.getDrawable(res) ?: continue
            val bmp = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(0xFF0B3D2E.toInt())
            d.setBounds(0, 0, 432, 432)
            d.draw(c)
            FileOutputStream(File(out, "launcher-$name.png")).use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
