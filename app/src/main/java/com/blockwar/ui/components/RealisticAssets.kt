package com.blockwar.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.*
import com.blockwar.R

object RealisticAssets {
    private val cache=object:LruCache<Int,ImageBitmap>(12*1024*1024) {
        override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*2
    }
    private val materialIds=listOf("real_aco","real_titanio","real_obsidiana","real_magma","real_vidro","real_gelo","real_cristal","real_cobre","real_marmore","real_madeira")
    fun resource(id:String):Int? = when {
        id=="hero_forja"->R.drawable.season_forge_hero
        id=="avatar_forja"->R.drawable.avatar_forge_sentinel
        id in materialIds->R.drawable.material_atlas
        id=="real_fundicao"||id.endsWith("_forja")->R.drawable.arena_forge
        id=="real_catedral"||id.endsWith("_glacial")->R.drawable.arena_ice
        id=="real_caverna"||id.endsWith("_cristal")->R.drawable.arena_crystal
        else->null
    }
    fun load(context:Context,id:String):ImageBitmap? {
        val resource=resource(id)?:return null
        return cache.get(resource)?:BitmapFactory.decodeResource(context.resources,resource,
            BitmapFactory.Options().apply{inSampleSize=2;inPreferredConfig=android.graphics.Bitmap.Config.RGB_565})
            .asImageBitmap().also{cache.put(resource,it)}
    }
    fun draw(scope:DrawScope,id:String):Boolean {
        val bitmap=resource(id)?.let(cache::get)?:return false
        with(scope) {
            val srcAspect=bitmap.width.toFloat()/bitmap.height;val dstAspect=size.width/size.height
            val srcWidth=if(dstAspect<srcAspect)(bitmap.height*dstAspect).toInt() else bitmap.width
            val srcHeight=if(dstAspect>srcAspect)(bitmap.width/dstAspect).toInt() else bitmap.height
            drawImage(bitmap,IntOffset((bitmap.width-srcWidth)/2,(bitmap.height-srcHeight)/2),IntSize(srcWidth,srcHeight),
                IntOffset.Zero,IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
        }
        return true
    }
    fun material(scope:DrawScope,id:String,origin:androidx.compose.ui.geometry.Offset,side:Float,alpha:Float) {
        val index=materialIds.indexOf(id);if(index<0)return
        val bitmap=cache.get(R.drawable.material_atlas)?:return
        val w=bitmap.width/4;val h=bitmap.height/3
        with(scope) {drawImage(bitmap,IntOffset(index%4*w+1,index/4*h+1),IntSize(w-2,h-2),
            IntOffset(origin.x.toInt(),origin.y.toInt()),IntSize(side.toInt().coerceAtLeast(1),side.toInt().coerceAtLeast(1)),alpha=alpha)}
    }
}
@Composable fun PrepareRealisticAsset(id:String) {
    val context=LocalContext.current.applicationContext
    remember(id,context){RealisticAssets.load(context,id)}
}
