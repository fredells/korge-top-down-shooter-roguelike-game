import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.ISizeInt
import data.LevelIterator
import `object`.Player
import scene.GameScene
import kotlin.reflect.KClass

suspend fun main() = Korge(
    Korge.Config(
        module = MainModule,
        debug = true,
        virtualSize = ISizeInt(640, 480),
        windowSize = ISizeInt(1280, 720),
    )
)

object MainModule : Module() {
    override val mainScene: KClass<out Scene>
        get() = GameScene::class

    override val bgcolor: RGBA
        get() = Colors["#2b2b2b"]

    override suspend fun AsyncInjector.configure() {
        mapInstance(Player())
        mapInstance(LevelIterator())
        mapPrototype { GameScene(get(), get()) }
    }
}