package io.main.gen.tree.precompute

import io.main.performance.PrecomputedPOI
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector

data class TreeBlock(val x: Int, val y: Int, val z: Int, val data: BlockData)

data class PrecomputedTree(
    override val origin: Vector,
    override val blocksPerChunk: Map<Pair<Int, Int>, List<TreeBlock>>,
    override val affectedChunks: Set<Pair<Int, Int>>,
    val type: TreeType = TreeType.OAK
): PrecomputedPOI()