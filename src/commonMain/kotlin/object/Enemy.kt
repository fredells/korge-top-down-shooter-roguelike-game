package `object`

import com.soywiz.korge.view.circle
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import scene.GameScene
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

open class Enemy(
    radius: Double = 15.0,
    startingHeading: Heading = Heading.North,
) : GameObject(startingHeading.angle, radius) {

    override var moveSpeed: Double = 3.0
    private val maxHp = 60
    var currentHp = maxHp

    var attackRange = radius + 25.0
    var frameCounter = 0 // used to stall actions

    val hpIndicator = text("$currentHp")

    override fun update(gameScene: GameScene) {
        hpIndicator.text = "$currentHp"

        if (frameCounter > 0) {
            frameCounter--
            return
        }

        val player = gameScene.player
        val distanceToPlayer = sqrt((player.x - x).pow(2) + (player.y - y).pow(2)) - player.radius

        if (distanceToPlayer < attackRange) {
            singleMeleeAttack(gameScene)
            return
        }

        move(gameScene)
        updateHeadingIndicator()
    }

    private fun move(gameScene: GameScene) {
        val player = gameScene.player
        val angleToPlayer = Angle.fromRadians(atan2(player.y - y, player.x - x))

        var tryAngle = angleToPlayer
        var angleCorrection = 0

        while (kotlin.math.abs(angleCorrection) < 140) {
            var collides = false

            val newX = moveSpeed * cos(tryAngle)
            val newY = moveSpeed * sin(tryAngle)

            var boundedX = x + newX
            boundedX = minOf(boundedX, gameScene.map.x + gameScene.map.width - radius)
            boundedX = maxOf(boundedX, gameScene.map.x + radius)

            var boundedY = y + newY
            boundedY = minOf(boundedY, gameScene.map.y + gameScene.map.width - radius)
            boundedY = maxOf(boundedY, gameScene.map.y + radius)

            for (it in (gameScene.enemies + gameScene.obstacles)) {
                if (it != this && !collides) {
                    val distanceToObstacle =
                        sqrt((it.x - boundedX).pow(2) + (it.y - boundedY).pow(2)) - radius - it.radius

                    if (distanceToObstacle <= 0) {
                        collides = true
                    }
                }
            }

            if (collides) {
                angleCorrection =
                    (kotlin.math.abs(angleCorrection) + 20) * (if (angleCorrection >= 0) 1 else -1) * -1
                tryAngle = Angle.fromDegrees(heading.degrees + angleCorrection)
            } else {
                this.heading = tryAngle
                x = boundedX
                y = boundedY
                break
            }
        }
    }

    private fun singleMeleeAttack(gameScene: GameScene) {
        val player = gameScene.player
        val angleToPlayer = Angle.fromRadians(atan2(player.y - y, player.x - x))
        heading = angleToPlayer

        gameScene.camera.content
            .circle(10.0, Colors.RED)
            .apply {
                val pos = alongHeading(this@Enemy.radius + 20).apply {
                    x -= radius
                    y -= radius
                }
                x = this@Enemy.x + pos.x
                y = this@Enemy.y + pos.y

                CoroutineScope(Dispatchers.Default).launch {
                    delay(25L) // check collision at keyframe
                    val relativeX = x + radius
                    val relativeY = y + radius

                    val distanceToPlayer =
                        sqrt((player.x - relativeX).pow(2) + (player.y - relativeY).pow(2)) - radius - player.radius

                    if (distanceToPlayer <= 0) {
                        player.hit(gameScene, damage = 5)
                    }

                    delay(25L) // remove after animation
                    this@apply.removeFromParent()
                }
            }
        frameCounter = 20
    }

    fun hit(gameScene: GameScene, damage: Int, stagger: Boolean = false) {
        currentHp -= damage
        if (currentHp <= 0) {
            die(gameScene)
            return
        }

        bounds.fill = Colors.ORANGERED
        CoroutineScope(Dispatchers.Default).launch {
            delay(67L)
            bounds.fill = Colors.INDIANRED
        }
    }

    fun die(gameScene: GameScene) {
        CoroutineScope(Dispatchers.Default).launch {
            gameScene.enemyMutex.withLock {
                gameScene.enemies.remove(this@Enemy)
            }
        }
        removeAllComponents()
        removeFromParent()
    }
}