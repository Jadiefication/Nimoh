package io.main.gen.tree.palm

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

        if (currentBlock == Material.SAND || currentBlock == Material.GRAVEL) {
            return y
        }

    }
    return -0x8
}