package io.main.gen

import io.main.world.handleGettingFreeBlock
import org.bukkit.Material
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import java.util.*
import kotlin.math.floor

class GrassGen(
    val worldGen: WorldGen
) : BlockPopulator() {

    private val longGrass = Material.TALL_GRASS.createBlockData()
    private val shortGrass = Material.SHORT_GRASS.createBlockData()
    private var declared = false
    private lateinit var newRandom: Random

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        if (!declared) {
            newRandom = Random(random.nextLong() * 2)
            declared = true
        }
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
            if (random.nextInt(3) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                selectGrass(worldX, worldY, worldZ, random, limitedRegion)
            }
        } else {
            return
        }
    }

    private fun selectGrass(
        x: Int,
        y: Int,
        z: Int,
        random: Random,
        limitedRegion: LimitedRegion
    ) {
        when (random.nextInt(0, 5)) {
            0 -> limitedRegion.setBlockData(x, y, z, longGrass)
            else -> limitedRegion.setBlockData(x, y, z, shortGrass)
        }
    }
}