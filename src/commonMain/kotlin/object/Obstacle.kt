package `object`

import com.soywiz.korim.color.Colors

class Obstacle(
    radius: Double,
    val blocksProjectiles: Boolean = true,
) : GameObject(
    radius = radius,
    color = Colors.BLACK.withA(100),
)