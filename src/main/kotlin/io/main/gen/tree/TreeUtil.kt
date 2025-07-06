package io.main.gen.tree

import io.main.gen.math.handleSphereChecking
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.generator.LimitedRegion
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

val air = Material.AIR.createBlockData()
fun handleBlockSphere(
    radius: Int,
    pos: Vector,
    limitedRegion: LimitedRegion,
    blockData: BlockData,
    notPlacedBlocks: MutableMap<Triple<Int, Int, Int>, BlockData>
) {
    handleSphereChecking(radius, pos) { x, y, z ->
        if (!limitedRegion.isInRegion(x, y, z)) {
            notPlacedBlocks.put(Triple(x, y, z), blockData)
        } else {
            if (limitedRegion.getBlockData(x, y, z) == air) {
                limitedRegion.setBlockData(x, y, z, blockData)
            }
        }
    }
}

fun rotateAroundY(
    vec: Vector,
    angle: Double
): Vector {
    val x = vec.x * cos(angle) - vec.z * sin(angle)
    val z = vec.x * sin(angle) + vec.z * cos(angle)
    return Vector(x, vec.y, z)
}
