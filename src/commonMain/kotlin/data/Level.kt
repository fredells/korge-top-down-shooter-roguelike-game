package data

import com.soywiz.korma.geom.Point
import `object`.Enemy
import `object`.Obstacle

data class Level(
    val width: Int = 600,
    val height: Int = 600,
    val playerSpawn: Point = Point(100, 100),
    val waves: List<LevelEnemies>,
    val obstacles: List<LevelObstacles> = listOf(),
) {
    companion object {
        fun singleWave(
            width: Int = 600,
            height: Int = 600,
            playerSpawn: Point = Point(100, 100),
            wave: LevelEnemies,
            obstacles: List<LevelObstacles> = listOf()
        ) = Level(
            width = width,
            height = height,
            playerSpawn = playerSpawn,
            waves = listOf(wave),
            obstacles = obstacles
        )
    }
}

typealias LevelEnemies = List<Pair<Enemy, Point>>

typealias LevelObstacles = Pair<Obstacle, Point>

class LevelIterator {
    private val iterator = levels.iterator()
    fun next() = iterator.next()
    fun hasNext() = iterator.hasNext()
}

val levels
    get() = listOf(
        Level.singleWave(
            wave = listOf(
                smallEnemy() to Point(400, 400)
            ),
        ),
        Level.singleWave(
            wave = listOf(
                smallEnemy() to Point(400, 200),
                mediumEnemy() to Point(400, 400),
            ),
            obstacles = listOf(
                Obstacle(
                    radius = 40.0,
                ) to Point(150, 150),
                Obstacle(
                    radius = 20.0,
                ) to Point(300, 300)
            )
        ),
    )

fun smallEnemy() = Enemy(radius = 15.0)

fun mediumEnemy() = Enemy(radius = 25.0)
