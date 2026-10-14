package com.blockwar.ui
import androidx.compose.ui.graphics.Color
import com.blockwar.ui.components.*
import org.junit.Assert.*
import org.junit.Test
class ParticleTimingTest {
 private fun controller()=ParticleController().apply {
  particles.add(PixelParticle(0f,0f,10f,0f,Color.White,4f,lifeTimeMs=1000))
 }
 @Test fun horizontalTravelDependsOnElapsedTimeInsteadOfFrameCount() {
  val fast=controller(); val slow=controller()
  repeat(8){fast.update(10)};repeat(2){slow.update(40)}
  assertEquals(fast.particles.single().x,slow.particles.single().x,.001f)
  assertEquals(fast.particles.single().alpha,slow.particles.single().alpha,.001f)
 }
 @Test fun expiredParticlesAreRemovedAndZeroTimeDoesNotMoveThem() {
  val c=controller();c.update(0);assertEquals(0f,c.particles.single().x,0f)
  c.update(1000);assertTrue(c.particles.isEmpty())
 }
}
