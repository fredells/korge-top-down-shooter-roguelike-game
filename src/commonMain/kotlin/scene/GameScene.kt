package scene

import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.view.*
import com.soywiz.korge.view.camera.CameraContainer
import com.soywiz.korge.view.camera.cameraContainer
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import data.LevelIterator
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import `object`.Enemy
import `object`.GameObject
import `object`.Obstacle
import `object`.Player

class GameScene(
    val player: Player,
    private val levelIterator: LevelIterator,
) : Scene() {

    lateinit var camera: CameraContainer
    lateinit var map: View

    lateinit var gameLoop: Cancellable

    val enemyMutex = Mutex()
    val enemies = mutableListOf<Enemy>()
    val projectiles = mutableListOf<GameObject>()
    val obstacles = mutableListOf<Obstacle>()

    override suspend fun Container.sceneInit() {
        stage?.gameWindow?.debug = true

        val level = levelIterator.next()

        camera = cameraContainer(640.0, 480.0) {
            map = solidRect(level.width, level.height, Colors.DARKGREEN)

            // player
            addChild(
                player
                    .apply { xy(level.playerSpawn) }
            )

            // enemies
            level.waves.first().forEach { (enemy, pos) ->
                addChild(
                    enemy.apply { xy(pos) }
                        .also { enemies.add(it) }
                )
            }

            // obstacles
            level.obstacles.forEach { (obstacle, pos) ->
                addChild(
                    obstacle.apply { xy(pos) }
                        .also { obstacles.add(it) }
                )
            }
        }

        camera.follow(player, setImmediately = true)

        gameLoop = addFixedUpdater(30.timesPerSecond) {
            if (enemies.isEmpty()) {
                launchImmediately {
                    stageCleared()
                }
            }

            player.update(this@GameScene)
            projectiles.onEach { it.update(this@GameScene) }
            enemies.onEach { it.update(this@GameScene) }
        }

        // explainer text for controls
        text(
            """
                move: arrow keys
                dash: space (or) d
                melee attack: a
                shoot gun: s
            """.trimIndent()
        )
    }

    suspend fun gameOver(isWin: Boolean = false) {
        // stop the fixed updater
        gameLoop.cancel()

        if (!isWin) {
            sceneView
                .uiButton("Game over")
                .apply { enabled = false }
                .centerOnStage()
        } else {
            sceneView.uiButton("You win")
                .apply { enabled = false }
                .centerOnStage()
        }

        sceneView.uiButton("Try again")
            .centerOnStage()
            .apply {
                y += 50

                onClick {
                    sceneView.removeChildren()
                    sceneContainer.changeTo<GameScene>(
                        Player(),
                        LevelIterator(),
                    )
                }
            }
    }

    private suspend fun stageCleared() {
        if (!levelIterator.hasNext()) {
            gameOver(isWin = true)
            return
        }

        gameLoop.cancel()

        sceneView.uiButton("Stage cleared")
            .apply { enabled = false }
            .centerOnStage()

        delay(2000L)

        sceneView.removeChildren()
        sceneContainer.changeTo<GameScene>(
            player,
            levelIterator,
        )
    }
}
