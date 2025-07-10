package io.main.gen.tree.palm

import io.main.gen.WorldGen
import io.main.gen.math.handleSphereChecking
import io.main.gen.tree.air
import io.main.gen.tree.handleBlockSphere
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeType
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.plusAssign
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class PalmGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val jLeaves = Material.JUNGLE_LEAVES.createBlockData()
    private val jungle = (Material.JUNGLE_WOOD.createBlockData() as Orientable).apply { axis = Axis.Y }
    private val notPlacedBlocks: MutableMap<Triple<Int, Int, Int>, BlockData> = ConcurrentHashMap()

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        if (notPlacedBlocks.isNotEmpty()) {
            notPlacedBlocks.forEach { (x, y, z), blockData ->
                attemptPlacement(limitedRegion, Vector(x, y, z), blockData)
            }
        }

        for (x in 0..16) {
            for (z in 0..16) {
                decidePalm(x, z, random, worldInfo, limitedRegion)
            }
        }
    }

    private fun decidePalm(
        x: Int,
        z: Int,
        random: Random,
        worldInfo: WorldInfo,
        limitedRegion: LimitedRegion
    ) {
        val worldX = x * 16 + x
        val worldZ = z * 16 + z

        val cellX = floor((worldX.toDouble() / worldGen.cellSize)).toLong()
        val cellZ = floor((worldZ.toDouble() / worldGen.cellSize)).toLong()

        if (worldGen.islandInCell.contains(cellX to cellZ)) {
            if (random.nextInt(5000) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateRecursive(
                    Vector(worldX, worldY, worldZ), Vector(
                        3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                        8.0,                               // Main Y direction
                        1 + (random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                    ), limitedRegion
                )
            }
        }
    }

    private fun attemptPlacement(
        limitedRegion: LimitedRegion,
        pos: Vector,
        blockData: BlockData
    ) {
        val x = pos.x.toInt()
        val y = pos.y.toInt()
        val z = pos.z.toInt()

        if (!limitedRegion.isInRegion(x, y, z)) {
            notPlacedBlocks.put(Triple(x, y, z), blockData)
        } else {
            if (limitedRegion.getBlockData(x, y, z) == air) {
                limitedRegion.setBlockData(x, y, z, blockData)
                notPlacedBlocks.remove(Triple(x, y, z), blockData)
            }
        }
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
                attemptPlacement(limitedRegion, step, jLeaves)
            }

            i += stepSize
        }

    }

    private fun generateRecursive(
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

            when (i) {
                0 -> handleSphereChecking(1, basePos) { x, y, z ->
                    attemptPlacement(limitedRegion, pos, jungle)
                }
                else -> {
                    attemptPlacement(limitedRegion, pos, jungle)
                }
            }
        }

        handleMath(basePos, direction, limitedRegion)
    }
}