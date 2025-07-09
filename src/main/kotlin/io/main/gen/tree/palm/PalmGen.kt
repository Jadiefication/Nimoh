package io.main.gen.tree.palm

import io.main.gen.WorldGen
import io.main.gen.tree.GlobalPOIMap
import io.main.gen.tree.air
import io.main.gen.tree.handleBlockSphere
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeType
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import kotlin.collections.plusAssign
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class PalmGen(): BlockPopulator() {

    private val jLeaves = Material.JUNGLE_LEAVES.createBlockData()
    private val jungle = Material.JUNGLE_WOOD.createBlockData() as Orientable

    init {
        jungle.axis = Axis.Y
    }

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        val chunk = chunkX to chunkZ
        val trees = GlobalPOIMap.getForChunk(chunk)

        trees.filter { it is PrecomputedTree && it.type == TreeType.PALM }.forEach { tree ->
            tree.blocksPerChunk[chunk]?.forEach { block ->
                if (limitedRegion.isInRegion(block.x, block.y, block.z)) {
                    limitedRegion.setBlockData(block.x, block.y, block.z, block.data)
                }
            }
            tree.completedChunks += chunk

            if (tree.completedChunks.containsAll(tree.affectedChunks)) {
                GlobalPOIMap.remove(tree)
            }
        }
    }
}