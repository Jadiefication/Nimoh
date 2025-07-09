package io.main.gen.tree

import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeType
import io.main.performance.PrecomputedPOI
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import java.util.*

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
        val trees = GlobalPOIMap.getForChunk(chunk)

        trees.filter { it is PrecomputedTree && it.type == TreeType.OAK }.forEach { tree ->
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

object GlobalPOIMap {
    private val treesPerChunk = mutableMapOf<Pair<Int, Int>, MutableList<PrecomputedPOI>>()

    fun register(poi: PrecomputedPOI) {
        for (chunk in poi.affectedChunks) {
            treesPerChunk.computeIfAbsent(chunk) { mutableListOf() }.add(poi)
        }
    }

    fun getForChunk(chunk: Pair<Int, Int>): List<PrecomputedPOI> {
        return treesPerChunk[chunk] ?: emptyList()
    }

    fun remove(poi: PrecomputedPOI) {
        poi.affectedChunks.forEach { chunk ->
            treesPerChunk[chunk]?.remove(poi)
        }
    }
}