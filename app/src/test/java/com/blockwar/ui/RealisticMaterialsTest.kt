package com.blockwar.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.blockwar.economy.RealisticCatalog
import com.blockwar.model.Cell
import com.blockwar.model.PieceType
import com.blockwar.ui.components.PixelBlockDrawer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealisticMaterialsTest {
    @Test fun allTenSurfacesAndGarbageRenderWithoutChangingTheirCellBounds() {
        val bitmap=Bitmap.createBitmap(1200,310,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(12,17,26))
        val items=RealisticCatalog.items.filter{it.itemCategory=="blocos"}
        com.blockwar.ui.components.RealisticAssets.load(androidx.test.core.app.ApplicationProvider.getApplicationContext(),items.first().id)
        assertEquals(10,items.size)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap.asImageBitmap()),Size(1200f,310f)) {
            items.forEachIndexed {i,item->
                PixelBlockDrawer.drawPixelBlock(this,Offset(i*120f+10,15f),100f,Cell.fromPiece(PieceType.LINE_3),skinId=item.id)
                PixelBlockDrawer.drawPixelBlock(this,Offset(i*120f+10,145f),100f,Cell.GARBAGE,skinId=item.id)
            }
        }
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=12f}
        val canvas=android.graphics.Canvas(bitmap)
        items.forEachIndexed {i,item->canvas.drawText(item.name.take(17),i*120f+5,280f,text)}
        val output=java.io.File("build/reports/materials-preview.png");output.parentFile?.mkdirs()
        output.outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        assertTrue(output.length()>1000)
        val arena=Bitmap.createBitmap(960,640,Bitmap.Config.ARGB_8888)
        val arenaCanvas=Canvas(arena.asImageBitmap())
        for ((index,id) in listOf("real_fundicao","real_catedral","real_caverna").withIndex()) {
            com.blockwar.ui.components.RealisticAssets.load(androidx.test.core.app.ApplicationProvider.getApplicationContext(),id)
            arenaCanvas.save();arenaCanvas.translate(index*320f,0f)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,arenaCanvas,Size(320f,640f)) {
                with(com.blockwar.ui.components.CosmeticArt){scene(id)}
                drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha=.3f))
                inset(70f,100f,70f,180f) {
                    with(com.blockwar.ui.components.CosmeticArt){scene(id,board=true)}
                    for (x in 0..6) PixelBlockDrawer.drawPixelBlock(this,Offset(x*18f,342f),18f,Cell.fromPiece(PieceType.LINE_3),skinId="neon")
                }
            }
            arenaCanvas.restore()
        }
        java.io.File("build/reports/arena-board-preview.png").outputStream().use{arena.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
