package io.main.gen.tree.palm

import io.main.gen.WorldGen
import io.main.gen.tree.air
import io.main.gen.tree.handleBlockSphere
import io.main.gen.tree.precompute.PrecomputedTree
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.sin

class PalmGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val jLeaves = Material.JUNGLE_LEAVES.createBlockData()
    private val jungle = Material.JUNGLE_WOOD.createBlockData() as Orientable

    init {
        jungle.axis = Axis.Y
    }

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        for (x in 0..16) {
            for (z in 0..16) {
                handleChance(x, z, random, limitedRegion, worldInfo, chunkX, chunkZ)
            }
        }
    }

    private fun handleChance(
        x: Int,
        z: Int,
        random: Random,
        limitedRegion: LimitedRegion,
        worldInfo: WorldInfo,
        chunkX: Int,
        chunkZ: Int
    ) {
        val worldX = chunkX * 16 + x
        val worldZ = chunkZ * 16 + z

        val cellX = floor((worldX.toDouble() / worldGen.cellSize)).toLong()
        val cellZ = floor((worldZ.toDouble() / worldGen.cellSize)).toLong()

        if (worldGen.islandInCell.contains(cellX to cellZ)) {
            if (random.nextInt(100) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateFractalTree(Vector(worldX, worldY, worldZ), Vector(
                    3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                    7.0,                               // Main Y direction
                    1 +(random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                ), limitedRegion)
            }
        } else {
            return
        }
    }

    private fun handleGettingFreeBlock(
        worldInfo: WorldInfo,
        x: Int,
        z: Int,
        limitedRegion: LimitedRegion
    ): Int {
        val freeBlock = worldInfo.maxHeight - 1
        for (y in freeBlock downTo 2) {
            if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                        limitedRegion.getBlockData(x, y - 1, z).material == Material.SAND)) {
                return y
            } else {
                continue
            }
        }
        return -0x8
    }

    private fun handleMath(
        basePos: Vector,
        direction: Vector,
        limitedRegion: LimitedRegion
    ) {
        val top = basePos.clone().add(direction)
        val twoPi = (2 * Math.PI).toLong()
        val stepSize = Math.PI / 8

        var i = 0.0
        while (i <= twoPi) {
            val leafLength = 5
            val upwardBend = -i * 0.2  // Gives slight upward curve to leaf
            val pos = top.clone().add(Vector(cos(i), upwardBend, sin(i)).multiply(leafLength))
            val unitPos = pos.clone().normalize()

            for (j in 0..pos.length().toInt()) {
                val step = top.clone().add(unitPos.clone().multiply(j))

                val newX = step.x.toInt()
                val newY = step.y.toInt()
                val newZ = step.z.toInt()

                if (limitedRegion.getBlockData(newX, newY, newZ) == air) {
                    limitedRegion.setBlockData(newX, newY, newZ, jLeaves)
                }
            }

            i += stepSize
        }



    }

    private fun generateFractalTree(
        basePos: Vector,
        direction: Vector,
        limitedRegion: LimitedRegion
    ) {
        val unitDirection = direction.clone().normalize()

        for (i in 0..direction.length().toInt()) {
            val step = basePos.clone().add(unitDirection.clone().multiply(i))

            val offset = Vector(
                sin(i * 0.2) * 0.5,
                0.0,
                sin(i * 0.15) * 0.5
            )
            val pos = step.clone().add(offset)


            val y = pos.y.toInt()
            val x = pos.x.toInt()
            val z = pos.z.toInt()

            when (i) {
                0 -> handleBlockSphere(1, basePos, limitedRegion, jungle)
                else -> {
                    if (limitedRegion.getBlockData(x, y, z) == air) {
                        limitedRegion.setBlockData(x, y, z, jungle)
                    }
                }
            }
        }

        handleMath(basePos, direction, limitedRegion)
    }
}