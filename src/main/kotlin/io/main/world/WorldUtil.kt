package io.main.world

import org.bukkit.Material
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo

fun handleGettingFreeBlock(
    worldInfo: WorldInfo,
    x: Int,
    z: Int,
    limitedRegion: LimitedRegion
): Int {
    val freeBlock = worldInfo.maxHeight - 1
    for (y in freeBlock downTo 2) {
        val currentBlock = limitedRegion.getBlockData(x, y, z).material
        val belowBlock = limitedRegion.getBlockData(x, y - 1, z).material

        if (currentBlock == Material.AIR &&
            (belowBlock == Material.GRASS_BLOCK || belowBlock == Material.DIRT)
        ) {
            return y
        }

    }
    return -0x8
}