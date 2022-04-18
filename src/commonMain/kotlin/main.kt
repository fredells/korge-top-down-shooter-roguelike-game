import com.soywiz.korge.Korge
import com.soywiz.korge.input.gamepad
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.XY

suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b2b"]) {

    fixedSizeContainer(width, height) {
        solidRect(600, 400, Colors.DARKGREEN)
            .center()
    }

//    gamepad {
//        connected {
//            println("gamepad connection: $it")
//        }
//
//        stick { _, stick, x, y ->
//            println("stick $stick - xy $x:$y")
//        }
//        button { _, pressed, button, value ->
//            println("button $button - pressed $pressed - value $value")
//        }
//    }
}

interface Person {
    val xy: XY
    val heading: Double // degrees

}