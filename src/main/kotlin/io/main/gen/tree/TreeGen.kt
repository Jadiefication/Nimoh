package io.main.gen.tree

import org.bukkit.Material
import org.bukkit.block.data.type.PointedDripstone
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.joml.Vector3L
import java.util.Random

class TreeGen: BlockPopulator() {

    private val maxDepth = 25

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
                if (random.nextInt(5) == 1) {
                    val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                    generateFractalTree(Vector3L(worldX, worldY, worldZ), Vector3L(0, 5, 0), 3, limitedRegion, random, 0)
                }
            }
        }
    }

    private fun handleGettingFreeBlock(worldInfo: WorldInfo, x: Int, z: Int, limitedRegion: LimitedRegion): Int {
        var freeBlock = worldInfo.maxHeight
        for (y in 0..freeBlock) {
            if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                        limitedRegion.getBlockData(x, y - 1, z).material == Material.GRASS_BLOCK || limitedRegion.getBlockData(x, y - 1, z).material == Material.DIRT)) {
                freeBlock = y
            } else {
                continue
            }
        }
        return freeBlock
    }

    private fun generateFractalTree(basePos: Vector3L, direction: Vector3L, thickness: Int, limitedRegion: LimitedRegion, random: Random, iterationDepth: Int) {
        if (direction.length() < 1 || iterationDepth >= maxDepth) {

            return
        }
    }
}