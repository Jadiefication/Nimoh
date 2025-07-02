package io.main.gen.tree

import io.main.gen.WorldGen
import io.main.gen.math.handleNaN
import io.main.gen.math.handleSphereChecking
import io.main.gen.math.rotateVectorDebug
import io.main.world.handleGettingFreeBlock
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.data.Orientable
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

class TreeGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val maxDepth = 4
    private val scale = 0.8562
    private val epsilon = 1e-6
    private val leafRadius = 2
    private lateinit var previousDirectionA: Vector
    private lateinit var previousDirectionB: Vector
    private val sLeaves = Material.SPRUCE_LEAVES.createBlockData()
    private val oLeaves = Material.OAK_LEAVES.createBlockData()
    private val aLeaves = Material.AZALEA_LEAVES.createBlockData()
    private val dLeaves = Material.DARK_OAK_LEAVES.createBlockData()
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
        for (x in 0..16) {
            for (z in 0..16) {
                handleChance(x, z, random, limitedRegion, worldInfo, chunkX, chunkZ)
            }
        }
    }

    private fun handleChance(
        x: Int,
        z: Int,
        random: Random,
        limitedRegion: LimitedRegion,
        worldInfo: WorldInfo,
        chunkX: Int,
        chunkZ: Int
    ) {
        val worldX = chunkX * 16 + x
        val worldZ = chunkZ * 16 + z

        val cellX = floor((worldX.toDouble() / worldGen.cellSize)).toLong()
        val cellZ = floor((worldZ.toDouble() / worldGen.cellSize)).toLong()

        if (worldGen.islandInCell.contains(cellX to cellZ)) {
            if (random.nextInt(5000) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateFractalTree(Vector(worldX, worldY, worldZ), Vector(
                    3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                    20.0,                               // Main Y direction
                    1 +(random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                ), limitedRegion, random, 0)
            }
        } else {
            return
        }
    }

    private fun handleMath(
        basePos: Vector,
        direction: Vector,
        random: Random,
        unitDirection: Vector,
        limitedRegion: LimitedRegion,
        iterationDepth: Int,
        thickness: Double
    ) {
        val newPos = basePos.clone().add(direction)
        val newLength = direction.length() * scale

        val referenceVector = if (abs(unitDirection.clone().dot(Vector(0, 1, 0))) > 1.0 - epsilon) {
            Vector(1, 0, 0)
        } else {
            Vector(0, 1, 0)
        }

        /*
        Don't understand at all anymore
        */
        val azimuthA = random.nextDouble() * 2 * Math.PI
        val azimuthB = azimuthA + Math.PI + (random.nextDouble() - 0.5) * Math.PI / 2

        val rotatedDirectionA = rotateAroundY(unitDirection.clone(), azimuthA)
        val rotatedDirectionB = rotateAroundY(unitDirection.clone(), azimuthB)

        var axis1 = rotatedDirectionA.clone().crossProduct(referenceVector).normalize()

        if (axis1.lengthSquared() < epsilon * epsilon) {
            axis1 = unitDirection.clone().crossProduct(Vector(0, 0, 1)).normalize()

            if (axis1.lengthSquared() < epsilon * epsilon) {
                error("CRITICAL ERROR: Failed to generate a non-zero axis1 for direction $direction at depth $iterationDepth.")
            }
        }

        val axis2 = rotatedDirectionB.clone().crossProduct(axis1).normalize()
        val branchAngleA = Math.toRadians(30 + random.nextDouble() * 40)  // 30–70 degrees
        val branchAngleB = Math.toRadians(20 + random.nextDouble() * 60)  // 20–80 degrees

        val rotatedAStep1 = rotateVectorDebug(unitDirection.clone(), axis1, branchAngleA)
        val newDirectionAUnscaled = rotateVectorDebug(rotatedAStep1, axis2, branchAngleB)
            .multiply(newLength)

        val rotatedBStep1 = rotateVectorDebug(unitDirection.clone(), axis1, -branchAngleA)
        val newDirectionBUnscaled = rotateVectorDebug(rotatedBStep1, axis2, -branchAngleB)
            .multiply(newLength)

        if (iterationDepth >= maxDepth - 1) {
            previousDirectionA = newDirectionAUnscaled
            previousDirectionB = newDirectionBUnscaled
        }

        val newThickness = thickness * (1 - iterationDepth / maxDepth).toDouble().pow(1.5)

        val upwardDirection = unitDirection.clone()
            .multiply(newLength * (0.8 + random.nextDouble() * 0.2))
            .add(Vector(0.0, 1.0, 0.0).multiply(0.4 + random.nextDouble() * 0.2)) // slight Y boost


        generateFractalTree(newPos, newDirectionAUnscaled, limitedRegion, random, iterationDepth + 1, newThickness)
        generateFractalTree(newPos, newDirectionBUnscaled, limitedRegion, random, iterationDepth + 1, newThickness)
        generateFractalTree(newPos, upwardDirection, limitedRegion, random, iterationDepth + 1, newThickness)
    }

    private fun generateFractalTree(
        basePos: Vector,
        direction: Vector,
        limitedRegion: LimitedRegion,
        random: Random,
        iterationDepth: Int,
        thickness: Double = 5.0
    ) {
        val unitDirection = direction.clone().normalize()

        if (handleNaN(direction) || unitDirection.lengthSquared() < epsilon * epsilon) {
            return
        }

        if (direction.length() < 1 || iterationDepth >= maxDepth) {
            decideLeaves(basePos, limitedRegion, random, direction, unitDirection)
            return
        } else {
            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))

                when (i) {
                    0 -> handleBlockSphere((thickness).toInt(), basePos, limitedRegion, oak)
                    else -> {
                        handleCylinder(limitedRegion, unitDirection, step, thickness)
                        /*if (limitedRegion.getBlockData(x, y, z) == air) {
                            limitedRegion.setBlockData(x, y, z, oak)
                        }*/
                    }
                }
            }

            handleMath(basePos, direction, random, unitDirection, limitedRegion, iterationDepth, thickness)
        }
    }

    private fun handleCylinder(
        limitedRegion: LimitedRegion,
        unitDirection: Vector,
        step: Vector,
        thickness: Double
    ) {
        val reference = if (abs(unitDirection.dot(Vector(0, 1, 0))) > 0.9) Vector(1, 0, 0) else Vector(0, 1, 0)
        val axis1 = unitDirection.clone().crossProduct(reference).normalize()
        val axis2 = unitDirection.clone().crossProduct(axis1).normalize()

        for (angle in 0 until 360 step 30) {
            val radians = Math.toRadians(angle.toDouble())
            val offset = axis1.clone().multiply(cos(radians) * thickness)
                .add(axis2.clone().multiply(sin(radians) * thickness))

            val point = step.clone().add(offset)
            val blockX = point.blockX
            val blockY = point.blockY
            val blockZ = point.blockZ

            if (limitedRegion.getBlockData(blockX, blockY, blockZ) == air) {
                limitedRegion.setBlockData(blockX, blockY, blockZ, oak)
            }
        }
    }

    private fun decideLeaves(
        basePos: Vector,
        limitedRegion: LimitedRegion,
        random: Random,
        direction: Vector,
        unitDirection: Vector
    ) {
        for (i in direction.length().toInt() - 3 until direction.length().toInt()) {
            val pos = basePos.clone().add(unitDirection.clone().multiply(i))

            handleSphereChecking(leafRadius, pos) { x, y, z ->
                val leaf = when (random.nextInt(0, 2)) {
                    0 -> sLeaves
                    1 -> oLeaves
                    2 -> aLeaves
                    else -> dLeaves
                }
                limitedRegion.setBlockData(x, y, z, leaf)
            }
        }
    }
}