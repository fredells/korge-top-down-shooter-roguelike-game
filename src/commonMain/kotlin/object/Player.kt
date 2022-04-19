package `object`

import com.soywiz.korev.Key
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import scene.GameScene
import kotlin.math.pow
import kotlin.math.sqrt

class Player(
    startingHeading: Heading = Heading.SouthEast,
) : GameObject(startingHeading.angle, color = Colors.CADETBLUE) {

    var maxHp = 50
    var currentHp = maxHp

    override var moveSpeed: Double = 5.0 // should this be a modifier instead?
    var dashRange = 50.0
    var attackSpeed = 1.0 // animation speed but also time in between attacks
    var hitRecoverySpeed = 1.0 // time to recover from staggering attacks

    var frameCounter = 0 // used to stall actions
    var basicAttackFrameCounter = 0 // 1 second to chain attacks
    var basicAttackStage = 0
    var projectileFrameCounter = 0 // used to stall projectile actions

    private val hpIndicator = text("$currentHp")

    override fun update(gameScene: GameScene) {
        hpIndicator.text = "$currentHp"

        if (basicAttackFrameCounter > 0) {
            basicAttackFrameCounter--
        } else {
            basicAttackStage = 0
        }

        if (projectileFrameCounter > 0) {
            projectileFrameCounter--
        }

        when {
            frameCounter >= 0 -> {
                frameCounter--
            }
            // dash
            stage?.input?.keys?.justPressed(Key.SPACE) ?: false ||
                    stage?.input?.keys?.justPressed(Key.D) ?: false -> {
                dash(gameScene)
            }

            // attack
            stage?.input?.keys?.justPressed(Key.A) ?: false -> {
                threeSequenceMeleeAttack(gameScene)
            }
            // special
            stage?.input?.keys?.justPressed(Key.S) ?: false &&
                    projectileFrameCounter == 0 -> {
                singleRangedAttack(gameScene)
            }
            Key.RIGHT.isPressed() && Key.UP.isPressed() -> {
                heading = Heading.NorthEast.angle
                move(gameScene)
            }
            Key.RIGHT.isPressed() && Key.DOWN.isPressed() -> {
                heading = Heading.SouthEast.angle
                move(gameScene)
            }
            Key.LEFT.isPressed() && Key.UP.isPressed() -> {
                heading = Heading.NorthWest.angle
                move(gameScene)
            }
            Key.LEFT.isPressed() && Key.DOWN.isPressed() -> {
                heading = Heading.SouthWest.angle
                move(gameScene)
            }
            Key.UP.isPressed() -> {
                heading = Heading.North.angle
                move(gameScene)
            }
            Key.RIGHT.isPressed() -> {
                heading = Heading.East.angle
                move(gameScene)
            }
            Key.DOWN.isPressed() -> {
                heading = Heading.South.angle
                move(gameScene)
            }
            Key.LEFT.isPressed() -> {
                heading = Heading.West.angle
                move(gameScene)
            }
            else -> {
                // idle
            }
        }

        updateHeadingIndicator()
    }

    private fun move(gameScene: GameScene) {
        var tryRange = moveSpeed

        val newX = moveSpeed * cos(heading)
        val newY = moveSpeed * sin(heading)

        var boundedX = x + newX
        boundedX = minOf(boundedX, gameScene.map.x + gameScene.map.width - radius)
        boundedX = maxOf(boundedX, gameScene.map.x + radius)

        var boundedY = y + newY
        boundedY = minOf(boundedY, gameScene.map.y + gameScene.map.width - radius)
        boundedY = maxOf(boundedY, gameScene.map.y + radius)


        while (tryRange > 0) {
            var collides = false

            for (it in (gameScene.enemies + gameScene.obstacles)) {
                val distanceToObstacle =
                    sqrt((it.x - boundedX).pow(2) + (it.y - boundedY).pow(2)) - radius - it.radius
                if (distanceToObstacle <= 0) {
                    collides = true
                }
            }
            if (!collides) {
                x = boundedX
                y = boundedY
                break
            } else {
                tryRange--
            }
        }
    }

    fun hit(gameScene: GameScene, damage: Int, stagger: Boolean = false) {
        currentHp -= damage

        if (currentHp <= 0) {
            die(gameScene)
            return
        }

        bounds.fill = Colors.PURPLE
        CoroutineScope(Dispatchers.Default).launch {
            delay(67L)
            bounds.fill = Colors.CADETBLUE
        }
    }

    private fun die(gameScene: GameScene) {
        hpIndicator.text = "$currentHp"

        CoroutineScope(Dispatchers.Default).launch {
            gameScene.gameOver()
        }
    }

    private fun dash(gameScene: GameScene) {
        val range = 50
        var tryRange = range

        while (tryRange > 0) {
            val newX = tryRange * cos(heading)
            val newY = tryRange * sin(heading)

            var boundedX = x + newX
            boundedX = minOf(boundedX, gameScene.map.x + gameScene.map.width - radius)
            boundedX = maxOf(boundedX, gameScene.map.x + radius)

            var boundedY = y + newY
            boundedY = minOf(boundedY, gameScene.map.y + gameScene.map.width - radius)
            boundedY = maxOf(boundedY, gameScene.map.y + radius)

            var collides = false

            for (it in (gameScene.enemies + gameScene.obstacles)) {
                val distanceToObstacle =
                    sqrt((it.x - boundedX).pow(2) + (it.y - boundedY).pow(2)) - radius - it.radius
                if (distanceToObstacle <= 0) {
                    collides = true
                }
            }

            if (collides) {
                tryRange -= 1
            } else {
                x = boundedX
                y = boundedY
                break
            }
        }
    }

    private fun threeSequenceMeleeAttack(gameScene: GameScene) {
        when (basicAttackStage) {
            0, 1 -> {
                gameScene.camera.content
                    .circle(10.0, Colors.BLUE)
                    .apply {
                        val pos = alongHeading(this@Player.radius + 20).apply {
                            x -= radius
                            y -= radius
                        }
                        x = this@Player.x + pos.x
                        y = this@Player.y + pos.y

                        CoroutineScope(Dispatchers.Default).launch {
                            delay(25L) // check collision at keyframe
                            val relativeX = x + radius
                            val relativeY = y + radius
                            gameScene.enemyMutex.withLock {
                                gameScene.enemies.onEach {
                                    val distanceToObstacle =
                                        sqrt((it.x - relativeX).pow(2) + (it.y - relativeY).pow(2)) - radius - it.radius

                                    if (distanceToObstacle <= 0) {
                                        it.hit(gameScene, damage = 20)
                                    }
                                }
                            }
                            delay(25L) // remove after animation
                            this@apply.removeFromParent()
                        }
                    }
                frameCounter = 2
                basicAttackFrameCounter = 30
                basicAttackStage++
            }
            2 -> {
                gameScene.camera.content
                    .circle(50.0, Colors.BLUE)
                    .apply {
                        val pos = alongHeading(this@Player.radius + 20).apply {
                            x -= radius
                            y -= radius
                        }
                        x = this@Player.x + pos.x
                        y = this@Player.y + pos.y

                        // todo replace coroutines with a stack of callbacks to be performed
                        // synchronously at the appropriate frame
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(25L) // check collision at keyframe
                            val relativeX = x + radius
                            val relativeY = y + radius

                            gameScene.enemyMutex.withLock {
                                gameScene.enemies.onEach {
                                    val distanceToObstacle =
                                        sqrt((it.x - relativeX).pow(2) + (it.y - relativeY).pow(2)) - radius - it.radius

                                    if (distanceToObstacle <= 0) {
                                        it.hit(gameScene, damage = 20)
                                    }
                                }
                            }
                            delay(25L) // remove after animation
                            this@apply.removeFromParent()
                        }
                    }
                frameCounter = 15
                basicAttackFrameCounter = 0
                basicAttackStage = 0
            }
        }
    }

    private fun singleRangedAttack(gameScene: GameScene) {
        frameCounter = 2
        projectileFrameCounter = 30

        val radius = 5.0
        val heading = this@Player.heading
        val moveSpeed = 8.0

        // todo scan for enemies in cone and auto aim
        // otherwise fire along heading
        val pos = alongHeading(this@Player.radius + 20).apply {
            x += this@Player.x - radius
            y += this@Player.y - radius
        }

        gameScene.camera.content
            .addChild(
                object : GameObject(
                    radius = radius,
                    heading = this@Player.heading,
                    color = Colors.BLUE
                ) {
                    var collided = false

                    init {
                        xy(pos)
                    }

                    override fun update(gameScene: GameScene) {
                        x += moveSpeed * cos(heading)
                        y += moveSpeed * sin(heading)

                        if (collided) {
                            removeAllComponents()
                            removeFromParent()
                            return
                        }

                        for (it in (gameScene.enemies + gameScene.obstacles.filter { it.blocksProjectiles })) {
                            if (!collided) {
                                val distanceToObstacle =
                                    sqrt((it.x - x).pow(2) + (it.y - y).pow(2)) - radius - it.radius

                                // check if it hit an enemy or obstacle
                                if (distanceToObstacle <= 0) {
                                    if (it is Enemy) {
                                        it.hit(gameScene, damage = 10)
                                    }
                                    collided = true
                                }
                            }
                        }

                        val inBounds = x + radius < gameScene.map.x + gameScene.map.width &&
                                x - radius > gameScene.map.x &&
                                y + radius < gameScene.map.y + gameScene.map.height &&
                                y - radius > gameScene.map.y

                        if (!inBounds) {
                            collided = true
                        }
                    }
                }
                    .also { gameScene.projectiles.add(it) }
            )
    }

    private fun Key.isPressed(): Boolean = stage?.input?.keys?.get(this) ?: false
}