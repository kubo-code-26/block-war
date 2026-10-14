package com.blockwar.social
import com.blockwar.model.FeaturedBadge
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResultCardTest {
    @Test fun rendersPortraitCardWithPublicResultsAndBadge() {
        val bitmap=ResultCard.render(ResultCardData("JOGADOR",true,148,20,64,4,123,"BATALHA ONLINE",FeaturedBadge("wins",2)))
        assertEquals(900,bitmap.width);assertEquals(1600,bitmap.height)
        val output=java.io.File("build/reports/result-card-preview.png");output.parentFile?.mkdirs()
        output.outputStream().use{assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}
        assertTrue(output.length()>1000)
    }
}
