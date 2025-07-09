package io.main.gen.tree

import io.main.gen.math.handleNaN
import io.main.gen.math.handleSphereChecking
import io.main.gen.math.rotateVectorDebug
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeBlock
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

private const val maxDepth = 5
private const val scale = 0.8562
private const val epsilon = 1e-6
private const val leafRadius = 8

private val sLeaves = Material.SPRUCE_LEAVES.createBlockData()
private val oLeaves = Material.OAK_LEAVES.createBlockData()
private val aLeaves = Material.AZALEA_LEAVES.createBlockData()
private val dLeaves = Material.DARK_OAK_LEAVES.createBlockData()
private val oak = (Material.OAK_LOG.createBlockData() as Orientable).apply { axis = org.bukkit.Axis.Y }

private val angles = (0 until 360 step 15).map { Math.toRadians(it.toDouble()) }
private val cosCache = angles.map { cos(it) }
private val sinCache = angles.map { sin(it) }

fun generateFractalTreePrecomputed(
    basePos: Vector,
    direction: Vector,
    random: Random
) {
    scope.launch {
        val blocksPerChunk = mutableMapOf<Pair<Int, Int>, MutableList<TreeBlock>>()

        fun addBlock(
            x: Int,
            y: Int,
            z: Int,
            data: BlockData
        ) {
            val chunk = (x shr 4) to (z shr 4)
            blocksPerChunk.computeIfAbsent(chunk) { mutableListOf() }
                .add(TreeBlock(x, y, z, data))
        }

        fun rotateAroundY(
            vec: Vector,
            angle: Double
        ): Vector {
            val x = vec.x * cos(angle) - vec.z * sin(angle)
            val z = vec.x * sin(angle) + vec.z * cos(angle)
            return Vector(x, vec.y, z)
        }

        fun generateRecursive(
            basePos: Vector,
            direction: Vector,
            iterationDepth: Int,
            thickness: Double
        ) {
            val unitDirection = direction.clone().normalize()
            if (handleNaN(direction) || unitDirection.lengthSquared() < epsilon * epsilon) return

            if (direction.length() < 1 || iterationDepth >= maxDepth) {
                for (i in direction.length().toInt() - 3 until direction.length().toInt()) {
                    val pos = basePos.clone().add(unitDirection.clone().multiply(i))
                    handleSphereChecking(leafRadius, pos) { x, y, z ->
                        val leaf = when (random.nextInt(4)) {
                            0 -> sLeaves
                            1 -> oLeaves
                            2 -> aLeaves
                            else -> dLeaves
                        }
                        addBlock(x, y, z, leaf)
                    }
                }
                return
            }

            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))
                val reference = if (abs(unitDirection.dot(Vector(0, 1, 0))) > 0.9) Vector(1, 0, 0) else Vector(0, 1, 0)
                val axis1 = unitDirection.clone().crossProduct(reference).normalize()
                val axis2 = unitDirection.clone().crossProduct(axis1).normalize()

                val radialStep = 0.5

                var r = 0.0
                while (r <= thickness) {
                    for (i in angles.indices) {
                        val offset = axis1.clone().multiply(cosCache[i] * r)
                            .add(axis2.clone().multiply(sinCache[i] * r))
                        val point = step.clone().add(offset)
                        addBlock(point.blockX, point.blockY, point.blockZ, oak)
                    }
                    r += radialStep
                }
            }

            val newLength = direction.length() * scale
            val referenceVector =
                if (abs(unitDirection.dot(Vector(0, 1, 0))) > 1.0 - epsilon) Vector(1, 0, 0) else Vector(0, 1, 0)

            val azimuthA = random.nextDouble() * 2 * PI
            val azimuthB = azimuthA + PI + (random.nextDouble() - 0.5) * PI / 2
            val rotatedDirectionA = rotateAroundY(unitDirection.clone(), azimuthA)
            val rotatedDirectionB = rotateAroundY(unitDirection.clone(), azimuthB)

            var axis1 = rotatedDirectionA.clone().crossProduct(referenceVector).normalize()
            if (axis1.lengthSquared() < epsilon * epsilon) {
                axis1 = unitDirection.clone().crossProduct(Vector(0, 0, 1)).normalize()
            }
            val axis2 = rotatedDirectionB.clone().crossProduct(axis1).normalize()
            val branchAngleA = Math.toRadians(30 + random.nextDouble() * 40)
            val branchAngleB = Math.toRadians(20 + random.nextDouble() * 60)

            val newDirectionA = rotateVectorDebug(
                rotateVectorDebug(unitDirection.clone(), axis1, branchAngleA),
                axis2,
                branchAngleB
            ).multiply(newLength)
            val newDirectionB = rotateVectorDebug(
                rotateVectorDebug(unitDirection.clone(), axis1, -branchAngleA),
                axis2,
                -branchAngleB
            ).multiply(newLength)

            val upwardDirection = unitDirection.clone()
                .multiply(newLength * (0.8 + random.nextDouble() * 0.2))
                .add(Vector(0.0, 1.0, 0.0).multiply(0.4 + random.nextDouble() * 0.2))

            val newThickness = thickness * (1 - iterationDepth / maxDepth.toDouble()).pow(1.5)

            generateRecursive(basePos.clone().add(direction), newDirectionA, iterationDepth + 1, newThickness)
            generateRecursive(basePos.clone().add(direction), newDirectionB, iterationDepth + 1, newThickness)
            generateRecursive(basePos.clone().add(direction), upwardDirection, iterationDepth + 1, newThickness)
        }

        generateRecursive(basePos, direction, 0, 5.0)

        val precomputed = PrecomputedTree(
            origin = basePos,
            blocksPerChunk = blocksPerChunk,
            affectedChunks = blocksPerChunk.keys.toSet()
        )

        Bukkit.getScheduler().runTask(plugin) { task ->
            GlobalPOIMap.register(precomputed)
        }
    }

}

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