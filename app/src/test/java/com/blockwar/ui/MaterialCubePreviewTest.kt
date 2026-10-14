package com.blockwar.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.blockwar.ui.components.MaterialCubePreview
import com.blockwar.ui.components.RealisticAssets
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialCubePreviewTest {
    @Test fun separateCubesShowDistinctMaterialsAndAnimatedForgeCracks() {
        val ids=listOf("neon","gb","cyber","gold","real_aco","real_titanio","real_obsidiana","real_magma","real_gelo","real_cristal","real_cobre","real_madeira")
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        RealisticAssets.load(context,"real_obsidiana")
        val gallery=Bitmap.createBitmap(960,600,Bitmap.Config.ARGB_8888)
        gallery.eraseColor(android.graphics.Color.rgb(12,17,26))
        val native=android.graphics.Canvas(gallery)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=13f}
        val samples=mutableSetOf<Int>()
        ids.forEachIndexed { i,id ->
            val bitmap=Bitmap.createBitmap(220,150,Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap.asImageBitmap()),Size(220f,150f)) {
                MaterialCubePreview.draw(this,id,.25f)
            }
            val pixels=IntArray(220*150);bitmap.getPixels(pixels,0,220,0,0,220,150)
            assertTrue("Material must be visible",pixels.count{android.graphics.Color.alpha(it)>0}>4000)
            assertEquals("No cropping at top",0,pixels.take(220).count{android.graphics.Color.alpha(it)>0})
            samples.add(pixels.contentHashCode())
            native.drawBitmap(bitmap,(i%4)*240f+10,(i/4)*200f+10,null)
            native.drawText(id,(i%4)*240f+10,(i/4)*200f+182,paint)
            bitmap.recycle()
        }
        assertEquals(ids.size,samples.size)
        val output=java.io.File("build/reports/material-cubes-preview.png");output.parentFile.mkdirs()
        output.outputStream().use{gallery.compress(Bitmap.CompressFormat.PNG,100,it)}
        gallery.recycle()
    }
}