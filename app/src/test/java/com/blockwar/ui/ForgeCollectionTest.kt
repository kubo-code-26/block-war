package com.blockwar.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import com.blockwar.model.*
import com.blockwar.ui.components.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ForgeCollectionTest {
    @Test fun animatedCollectionPreservesEmptyBoardCenterAndGrayAttackBlocks() {
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        for(id in listOf("real_fundicao","real_obsidiana"))RealisticAssets.load(context,id)
        val output=java.io.File("build/reports/forge-frames").apply{mkdirs()}
        var first:IntArray?=null
        for(frame in 0..11) {
            val p=frame/12f
            val bitmap=Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap.asImageBitmap())
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,canvas,Size(640f,640f)) {
                with(CosmeticArt){scene("real_fundicao",p)}
                drawRect(Color.Black.copy(alpha=.35f))
                inset(25f,55f,395f,145f) {
                    with(CosmeticArt){scene("real_fundicao",p,board=true)}
                    for(y in 17..19)for(x in 0..6)PixelBlockDrawer.drawPixelBlock(this,Offset(x*22f,y*22f),22f,Cell.fromPiece(PieceType.LINE_3),skinId="real_obsidiana",firePhase=p)
                    with(LineClearArt){render("season_anim_forja",16*22f,220f,22f,p)}
                }
                inset(315f,55f,185f,445f){with(CosmeticArt){avatar("avatar_forja",p);frame("season_frame_forja",p)}}
                inset(285f,270f,45f,300f){with(CosmeticArt){effect("real_fagulhas",p)}}
                inset(285f,390f,25f,120f){with(BannerArt){render("season_banner_forja",p)}}
                for(i in 0..3)PixelBlockDrawer.drawPixelBlock(this,Offset(285f+i*65,530f),55f,Cell.fromPiece(PieceType.LINE_3),skinId="real_magma",firePhase=p)
            }
            val pixels=IntArray(640*640);bitmap.getPixels(pixels,0,640,0,0,640,640)
            if(frame==0)first=pixels else if(frame==6)assertTrue("Flames must visibly move",first!!.indices.count{first!![it]!=pixels[it]}>1000)
            val center=bitmap.getPixel(25+101,55+111)
            assertTrue("Playing surface stays dark",android.graphics.Color.red(center)<40&&android.graphics.Color.green(center)<40&&android.graphics.Color.blue(center)<40)
            assertEquals("Flames stay away from the playing surface",first!![(55+111)*640+25+101],center)
            java.io.File(output,"frame-%02d.png".format(frame)).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
        fun garbage(phase:Float):IntArray {
            val image=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image.asImageBitmap()),Size(48f,48f)) {
                PixelBlockDrawer.drawPixelBlock(this,Offset.Zero,48f,Cell.GARBAGE,skinId="real_magma",firePhase=phase)
            }
            return IntArray(48*48).also{image.getPixels(it,0,48,0,0,48,48);image.recycle()}
        }
        assertArrayEquals("Attack blocks must stay visually distinct",garbage(0f),garbage(.5f))
    }
}
