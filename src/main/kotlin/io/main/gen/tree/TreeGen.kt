package io.main.gen.tree

import io.main.gen.WorldGen
import io.main.gen.math.handleNaN
import io.main.gen.math.handleSphereChecking
import io.main.gen.math.rotateVectorDebug
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.world.handleGettingFreeBlock
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

class TreeGen(): BlockPopulator() {

    private val oak = Material.OAK_LOG.createBlockData() as Orientable

    init {
        oak.axis = Axis.Y
    }

    override fun populate(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        limitedRegion: LimitedRegion
    ) {
        val chunk = chunkX to chunkZ
        val trees = GlobalTreeMap.getTreesForChunk(chunk)

        trees.forEach { tree ->
            tree.blocksPerChunk[chunk]?.forEach { block ->
                if (limitedRegion.isInRegion(block.x, block.y, block.z)) {
                    limitedRegion.setBlockData(block.x, block.y, block.z, block.data)
                }
            }
            tree.completedChunks += chunk

            if (tree.completedChunks.containsAll(tree.affectedChunks)) {
                GlobalTreeMap.removeTree(tree)
            }
        }
    }


}

object GlobalTreeMap {
    private val treesPerChunk = mutableMapOf<Pair<Int, Int>, MutableList<PrecomputedTree>>()

    fun registerTree(tree: PrecomputedTree) {
        for (chunk in tree.affectedChunks) {
            treesPerChunk.computeIfAbsent(chunk) { mutableListOf() }.add(tree)
        }
    }

    fun getTreesForChunk(chunk: Pair<Int, Int>): List<PrecomputedTree> {
        return treesPerChunk[chunk] ?: emptyList()
    }

    fun removeTree(tree: PrecomputedTree) {
        tree.affectedChunks.forEach { chunk ->
            treesPerChunk[chunk]?.remove(tree)
        }
    }
}