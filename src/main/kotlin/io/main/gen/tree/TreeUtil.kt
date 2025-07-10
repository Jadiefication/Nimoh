package io.main.gen.tree

import io.main.gen.math.handleNaN
import io.main.gen.math.handleSphereChecking
import io.main.gen.math.rotateVectorDebug
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeBlock
import io.main.nimoh.Nimoh.Companion.localRandom
import io.main.nimoh.Nimoh.Companion.plugin
import io.main.nimoh.Nimoh.Companion.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Orientable
import org.bukkit.generator.LimitedRegion
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.*

val air = Material.AIR.createBlockData()

fun handleBlockSphere(
    radius: Int,
    pos: Vector,
    limitedRegion: LimitedRegion,
    blockData: BlockData
) {
    handleSphereChecking(radius, pos) { x, y, z ->
        if (limitedRegion.getBlockData(x, y, z) == air) {
            limitedRegion.setBlockData(x, y, z, blockData)
        }
    }
}