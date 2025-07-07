package io.main.gen.tree.precompute

import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector

data class TreeBlock(val x: Int, val y: Int, val z: Int, val data: BlockData)

data class PrecomputedTree(
    val origin: Vector,
    val blocksPerChunk: Map<Pair<Int, Int>, List<TreeBlock>>,
    val affectedChunks: Set<Pair<Int, Int>>,
    val completedChunks: MutableSet<Pair<Int, Int>> = mutableSetOf()
)