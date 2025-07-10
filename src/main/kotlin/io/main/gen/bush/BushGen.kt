package io.main.gen.bush

import io.main.gen.WorldGen
import io.main.gen.tree.air
import io.main.gen.tree.handleBlockSphere
import io.main.world.handleGettingFreeBlock
import org.bukkit.Material
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.floor

class BushGen(
    val worldGen: WorldGen
) : BlockPopulator() {

    private val fence = Material.SPRUCE_FENCE.createBlockData()
    private val sLeaves = Material.SPRUCE_LEAVES.createBlockData()
    private val oLeaves = Material.OAK_LEAVES.createBlockData()
    private val aLeaves = Material.AZALEA_LEAVES.createBlockData()
    private val dLeaves = Material.DARK_OAK_LEAVES.createBlockData()
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
            newRandom = Random(random.nextLong())
            declared = true
        }
        for (x in 0..16) {
            for (z in 0..16) {
                handleChance(x, z, newRandom, limitedRegion, worldInfo, chunkX, chunkZ)
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
            if (random.nextInt(1000) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateBush(
                    Vector(worldX, worldY, worldZ), Vector(
                        3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                        4.0,                               // Main Y direction
                        1 + (random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                    ), limitedRegion, random
                )
            }
        } else {
            return
        }
    }

    private fun generateBush(
        basePos: Vector,
        direction: Vector,
        limitedRegion: LimitedRegion,
        random: Random
    ) {
        val unitDirection = direction.normalize()

        for (i in 0..direction.length().toInt()) {
            val step = basePos.clone().add(unitDirection.clone().multiply(i))

            val x = step.x.toInt()
            val y = step.y.toInt()
            val z = step.z.toInt()

            if (limitedRegion.getBlockData(x, y, z) == air) {
                limitedRegion.setBlockData(x, y, z, fence)
            }

            if (i == direction.length().toInt()) {
                val leaf = when (random.nextInt(0, 2)) {
                    0 -> sLeaves
                    1 -> oLeaves
                    2 -> aLeaves
                    else -> dLeaves
                }
                handleBlockSphere(3, Vector(x, y, z), limitedRegion, leaf)
            }
        }
    }
}