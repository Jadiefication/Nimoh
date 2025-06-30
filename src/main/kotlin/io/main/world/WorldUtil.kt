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
        if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                    limitedRegion.getBlockData(x, y - 1, z).material == Material.GRASS_BLOCK || limitedRegion.getBlockData(x, y - 1, z).material == Material.DIRT)) {
            return y
        } else {
            continue
        }
    }
    return -0x8
}