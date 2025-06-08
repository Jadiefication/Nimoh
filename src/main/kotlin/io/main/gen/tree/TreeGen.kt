package io.main.gen.tree

import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs

class TreeGen: BlockPopulator() {

    private val maxDepth = 25
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
                val worldX = chunkX * 16 + x
                val worldZ = chunkZ * 16 + z
                if (random.nextInt(100) == 1) {
                    val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                    generateFractalTree(Vector(worldX, worldY, worldZ), Vector(0, 5, 0), limitedRegion, random, 0)
                }
            }
        }
    }

    private fun handleGettingFreeBlock(worldInfo: WorldInfo, x: Int, z: Int, limitedRegion: LimitedRegion): Int {
        var freeBlock = worldInfo.maxHeight
        for (y in freeBlock downTo 0) {
            if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                        limitedRegion.getBlockData(x, y - 1, z).material == Material.GRASS_BLOCK || limitedRegion.getBlockData(x, y - 1, z).material == Material.DIRT)) {
                freeBlock = y
            } else {
                continue
            }
        }
        return freeBlock
    }

    private fun generateFractalTree(basePos: Vector, direction: Vector, limitedRegion: LimitedRegion, random: Random, iterationDepth: Int) {
        val oak = Material.OAK_LOG.createBlockData() as Directional
        oak.facing = BlockFace.UP
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

            val axis1 = (if (abs(direction.y) <= 1) direction.crossProduct(Vector(1, 0, 0)) else direction.crossProduct(Vector(0, 1, 0))).normalize()

            val axis2 = direction.crossProduct(axis1).normalize()
            val branchAngleA = Math.toRadians(30.0) + (random.nextDouble() - 0.1)
            val branchAngleB = Math.toRadians(30.0) + (random.nextDouble() - 0.1)

            val newDirectionA = unitDirection.clone()
                .rotateAroundAxis(axis1, branchAngleA)
                .rotateAroundAxis(axis2, branchAngleB)
                .multiply(newLength)

            val newDirectionB = unitDirection.clone()
                .rotateAroundAxis(axis1, -branchAngleA)
                .rotateAroundAxis(axis2, branchAngleB)
                .normalize()
                .multiply(newLength)

            generateFractalTree(newPos, newDirectionA, limitedRegion, random, iterationDepth + 1)
            generateFractalTree(newPos, newDirectionB, limitedRegion, random, iterationDepth + 1)
        }
    }
}