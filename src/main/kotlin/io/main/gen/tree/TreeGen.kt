package io.main.gen.tree

import io.main.gen.WorldGen
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

class TreeGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val maxDepth = 6
    private val scale = 0.8562

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
                generateFractalTree(Vector(worldX, worldY, worldZ), Vector(0, 5, 0), limitedRegion, random, 0)
            }
        } else {
            return
        }
    }

    private fun handleGettingFreeBlock(worldInfo: WorldInfo, x: Int, z: Int, limitedRegion: LimitedRegion): Int {
        val freeBlock = worldInfo.maxHeight - 1
        for (y in freeBlock downTo 2) {
            if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                        limitedRegion.getBlockData(x, y - 1, z).material == Material.GRASS_BLOCK || limitedRegion.getBlockData(x, y - 1, z).material == Material.DIRT)) {
                return y
            } else {
                continue
            }
        }
        return -0x8
    }

    private fun generateFractalTree(basePos: Vector, direction: Vector, limitedRegion: LimitedRegion, random: Random, iterationDepth: Int) {
        val oak = Material.OAK_LOG.createBlockData() as Orientable
        oak.axis = Axis.Y
        if (direction.length() < 1 || iterationDepth >= maxDepth) {

            return
        } else {
            val unitDirection = direction.normalize()
            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))
                val x = step.x.toInt()
                val y = step.y.toInt()
                val z = step.z.toInt()

                limitedRegion.setBlockData(x, y, z, oak)
            }

            val newPos = basePos.clone().add(direction)
            val newLength = direction.length() * scale

            val axis1 = if (abs(direction.y) < 0.9) {
                direction.crossProduct(Vector(0, 1, 0)).normalize()
            } else {
                direction.crossProduct(Vector(1, 0, 0)).normalize()
            }

            val axis2 = direction.crossProduct(axis1).normalize()
            val branchAngleA = Math.toRadians(30.0) + (random.nextDouble() - 0.1)
            val branchAngleB = Math.toRadians(30.0) + (random.nextDouble() - 0.1)

            /*val newDirectionA = unitDirection.clone()
                .rotateAroundAxis(axis1, branchAngleA)
                .rotateAroundAxis(axis2, branchAngleB)
                .multiply(newLength)

            val newDirectionB = unitDirection.clone()
                .rotateAroundAxis(axis1, -branchAngleA)
                .rotateAroundAxis(axis2, branchAngleB)
                .normalize()
                .multiply(newLength)*/

            val newDirectionA = Vector(2, 3, 1).multiply(newLength)
            val newDirectionB = Vector(-2, 3, -1).multiply(newLength)

            generateFractalTree(newPos, newDirectionA, limitedRegion, random, iterationDepth + 1)
            generateFractalTree(newPos, newDirectionB, limitedRegion, random, iterationDepth + 1)
        }
    }
}