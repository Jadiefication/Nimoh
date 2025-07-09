package io.main.performance

import io.main.gen.tree.precompute.TreeBlock
import org.bukkit.util.Vector

abstract class PrecomputedPOI {

    abstract val origin: Vector
    abstract val blocksPerChunk: Map<Pair<Int, Int>, List<TreeBlock>>
    abstract val affectedChunks: Set<Pair<Int, Int>>
    val completedChunks: MutableSet<Pair<Int, Int>> = mutableSetOf()
}