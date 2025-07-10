package io.main.gen.path

import io.main.gen.WorldGen
import org.bukkit.Material
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import org.spongepowered.noise.module.source.Simplex
import java.util.*
import kotlin.math.floor
import kotlin.math.sin

class PathGen(
    val worldGen: WorldGen
) : BlockPopulator() {

    private val simplex = Simplex()
    private val pScale = 0.5
    private var declared = false
    private val pathBlock = Material.DIRT_PATH.createBlockData()
    private val coarseDirt = Material.COARSE_DIRT.createBlockData()
    private val brownSand = Material.BROWN_CONCRETE_POWDER.createBlockData()
    private val mossBlock = Material.MOSS_BLOCK.createBlockData()
    private val sTrapdoor = Material.SPRUCE_TRAPDOOR.createBlockData()
    private val sPlanks = Material.SPRUCE_PLANKS.createBlockData()

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        if (!declared) {
            simplex.setSeed(UUID.randomUUID().mostSignificantBits.toInt())
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
            val neighbours = checkNeighbours(worldX, worldZ, worldInfo, limitedRegion)
            if (neighbours.first) {
                neighbours.second.forEach {
                    val value = random.nextInt(0, 6)
                    val block = when (value) {
                        0 -> pathBlock
                        1 -> coarseDirt
                        2 -> brownSand
                        3 -> mossBlock
                        4 -> sTrapdoor
                        5 -> sPlanks
                        else -> null
                    }
                    limitedRegion.setBlockData(it, block!!)
                }
            }
        } else {
            return
        }
    }

    private fun checkNeighbours(
        x: Int,
        z: Int,
        worldInfo: WorldInfo,
        limitedRegion: LimitedRegion
    ): Pair<Boolean, List<Vector>> {
        val offsets = listOf(
            -1 to 0, // West
            0 to -1, // North
            -1 to -1, // Northwest
            1 to 0, // East
            0 to 1, // South
            1 to 1, // Southeast
            1 to -1, // Northeast
            -1 to 1,  // Southwest
            0 to 0 // Center
        )

        val ys = offsets.map { (dx, dz) ->
            Vector(x + dx, handleGettingFreeBlock(worldInfo, x + dx, z + dz, limitedRegion), z + dz)
        }

        return offsets.mapIndexed { index, (dx, dz) ->
            simplex.get((x + dx) * pScale, ys[index].y * pScale, ((z + dz) + sin(x * 0.01) * 10) * pScale)
        }.all { it <= 0.2 } to ys
    }

    private fun handleGettingFreeBlock(
        worldInfo: WorldInfo,
        x: Int,
        z: Int,
        limitedRegion: LimitedRegion
    ): Int {
        val freeBlock = worldInfo.maxHeight - 1
        for (y in freeBlock downTo 2) {
            val currentBlock = limitedRegion.getBlockData(x, y, z).material

            if (currentBlock == Material.GRASS_BLOCK || currentBlock == Material.DIRT) {
                return y
            }

        }
        return -0x8
    }
}