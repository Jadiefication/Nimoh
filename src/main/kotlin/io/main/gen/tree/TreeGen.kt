package io.main.gen.tree

import io.main.gen.WorldGen
import io.main.gen.math.handleNaN
import io.main.gen.math.handleSphereChecking
import io.main.gen.math.rotateVectorDebug
import io.main.nimoh.Nimoh.Companion.localRandom
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
import kotlin.math.*

private const val maxDepth = 5
private const val scale = 0.8562
private const val epsilon = 1e-6
private const val leafRadius = 8

class TreeGen(
    val worldGen: WorldGen
) : BlockPopulator() {

    private val sLeaves = Material.SPRUCE_LEAVES.createBlockData()
    private val oLeaves = Material.OAK_LEAVES.createBlockData()
    private val aLeaves = Material.AZALEA_LEAVES.createBlockData()
    private val dLeaves = Material.DARK_OAK_LEAVES.createBlockData()
    private val oak = (Material.OAK_LOG.createBlockData() as Orientable).apply { axis = Axis.Y }

    private val angles = (0 until 360 step 15).map { Math.toRadians(it.toDouble()) }
    private val cosCache = angles.map { cos(it) }
    private val sinCache = angles.map { sin(it) }
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
                decideTree(x, z, random, worldInfo, limitedRegion)
            }
        }
    }

    private fun decideTree(
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
                        6.0,                               // Main Y direction
                        1 + (random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                    ), limitedRegion = limitedRegion
                )
            }
        }
    }

    private fun rotateAroundY(
        vec: Vector,
        angle: Double
    ): Vector {
        val x = vec.x * cos(angle) - vec.z * sin(angle)
        val z = vec.x * sin(angle) + vec.z * cos(angle)
        return Vector(x, vec.y, z)
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

    private fun generateRecursive(
        basePos: Vector,
        direction: Vector,
        iterationDepth: Int = 0,
        thickness: Double = 5.0,
        limitedRegion: LimitedRegion
    ) {
        val unitDirection = direction.clone().normalize()
        if (handleNaN(direction) || unitDirection.lengthSquared() < epsilon * epsilon) return

        if (direction.length() < 1 || iterationDepth >= maxDepth) {
            for (i in direction.length().toInt() - 3 until direction.length().toInt()) {
                val pos = basePos.clone().add(unitDirection.clone().multiply(i))
                handleSphereChecking(leafRadius, pos) { x, y, z ->
                    val leaf = when (localRandom.nextInt(4)) {
                        0 -> sLeaves
                        1 -> oLeaves
                        2 -> aLeaves
                        else -> dLeaves
                    }

                    attemptPlacement(limitedRegion, pos, leaf)
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
                    attemptPlacement(limitedRegion, point, oak)
                }
                r += radialStep
            }
        }

        val newLength = direction.length() * scale
        val referenceVector =
            if (abs(unitDirection.dot(Vector(0, 1, 0))) > 1.0 - epsilon) Vector(1, 0, 0) else Vector(0, 1, 0)

        val azimuthA = localRandom.nextDouble() * 2 * PI
        val azimuthB = azimuthA + PI + (localRandom.nextDouble() - 0.5) * PI / 2
        val rotatedDirectionA = rotateAroundY(unitDirection.clone(), azimuthA)
        val rotatedDirectionB = rotateAroundY(unitDirection.clone(), azimuthB)

        var axis1 = rotatedDirectionA.clone().crossProduct(referenceVector).normalize()
        if (axis1.lengthSquared() < epsilon * epsilon) {
            axis1 = unitDirection.clone().crossProduct(Vector(0, 0, 1)).normalize()
        }
        val axis2 = rotatedDirectionB.clone().crossProduct(axis1).normalize()
        val branchAngleA = Math.toRadians(30 + localRandom.nextDouble() * 40)
        val branchAngleB = Math.toRadians(20 + localRandom.nextDouble() * 60)

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
            .multiply(newLength * (0.8 + localRandom.nextDouble() * 0.2))
            .add(Vector(0.0, 1.0, 0.0).multiply(0.4 + localRandom.nextDouble() * 0.2))

        val newThickness = thickness * (1 - iterationDepth / maxDepth.toDouble()).pow(1.5)

        generateRecursive(
            basePos.clone().add(direction),
            newDirectionA,
            iterationDepth + 1,
            newThickness,
            limitedRegion
        )
        generateRecursive(
            basePos.clone().add(direction),
            newDirectionB,
            iterationDepth + 1,
            newThickness,
            limitedRegion
        )
        generateRecursive(
            basePos.clone().add(direction),
            upwardDirection,
            iterationDepth + 1,
            newThickness,
            limitedRegion
        )
    }
}