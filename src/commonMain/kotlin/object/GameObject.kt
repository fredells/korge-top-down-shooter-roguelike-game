package `object`

import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import scene.GameScene

open class GameObject(
    var heading: Angle = Heading.North.angle, // degrees
    val radius: Double = 15.0,
    val color: RGBA = Colors.INDIANRED,
) : FixedSizeContainer(radius * 2, radius * 2) {

    val bounds: Circle = circle(radius = radius, fill = color)
        .xy(-radius, -radius)

    val center = circle(1.0, Colors.YELLOW)
        .centerOn(bounds)

    private val headingIndicator = line(
        Point(0, 0),
        Point(
            radius * cos(heading),
            radius * sin(heading)
        )
    )

    open var moveSpeed: Double = 5.0

    open fun update(gameScene: GameScene) {}

    fun updateHeadingIndicator() {
        headingIndicator.apply {
            x2 = radius * cos(heading)
            y2 = radius * sin(heading)
        }
    }

    fun alongHeading(range: Double): Point {
        return Point(
            range * cos(heading),
            range * sin(heading)
        )
    }
}