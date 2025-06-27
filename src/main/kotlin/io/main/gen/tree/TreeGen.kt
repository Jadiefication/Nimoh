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
import kotlin.math.floor

class TreeGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val maxDepth = 4
    private val scale = 0.8562
    private val epsilon = 1e-6
    private val leafRadius = 3
    private val leaves = Material.OAK_LEAVES.createBlockData()
    private lateinit var previousDirectionA: Vector
    private lateinit var previousDirectionB: Vector

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
            if (random.nextInt(1000) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateFractalTree(Vector(worldX, worldY, worldZ), Vector(
                    3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                    8.0,                               // Main Y direction
                    1 +(random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                ).normalize().multiply(6.0), limitedRegion, random, 0, 2.0)
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
        iterationDepth: Int
    ) {
        val newPos = basePos.clone().add(direction)
        val newLength = direction.length() * scale

        val referenceVector = if (abs(unitDirection.clone().dot(Vector(0, 1, 0))) > 1.0 - epsilon) {
            Vector(1, 0, 0)
        } else {
            Vector(0, 1, 0)
        }

        var axis1 = unitDirection.clone().crossProduct(referenceVector).normalize()

        if (axis1.lengthSquared() < epsilon * epsilon) {
            axis1 = unitDirection.clone().crossProduct(Vector(0, 0, 1)).normalize()

            if (axis1.lengthSquared() < epsilon * epsilon) {
                error("CRITICAL ERROR: Failed to generate a non-zero axis1 for direction $direction at depth $iterationDepth.")
            }
        }

        val axis2 = unitDirection.clone().crossProduct(axis1).normalize()
        val branchAngleA = Math.toRadians(60.0) + (random.nextDouble() - 0.1)
        val branchAngleB = Math.toRadians(60.0) + (random.nextDouble() - 0.1)

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

        generateFractalTree(newPos, newDirectionAUnscaled, limitedRegion, random, iterationDepth + 1)
        generateFractalTree(newPos, newDirectionBUnscaled, limitedRegion, random, iterationDepth + 1)
    }

    private fun generateFractalTree(
        basePos: Vector,
        direction: Vector,
        limitedRegion: LimitedRegion,
        random: Random,
        iterationDepth: Int,
        thickness: Double = 2.0
    ) {
        val oak = Material.OAK_LOG.createBlockData() as Orientable
        oak.axis = Axis.Y
        val unitDirection = direction.clone().normalize()

        if (handleNaN(direction) || unitDirection.lengthSquared() < epsilon * epsilon) {
            return
        }

        if (direction.length() < 1 || iterationDepth >= maxDepth) {
            handleLeaves(basePos, limitedRegion)
            handleLeaves(basePos, limitedRegion)

            return
        } else {
            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))

                val x = step.x.toInt()
                val y = step.y.toInt()
                val z = step.z.toInt()

                if (iterationDepth == 0) {
                    when (i) {
                        0 -> handleBlockSphere((thickness + random.nextInt(0, 1)).toInt(), basePos, limitedRegion, oak)
                        1 -> handleBlockSphere(thickness.toInt(), basePos, limitedRegion, oak)
                        2 -> handleBlockSphere(thickness.toInt() - 1, basePos, limitedRegion, oak)
                        else -> {
                            if (limitedRegion.getBlockData(x, y, z) == air) {
                                limitedRegion.setBlockData(x, y, z, oak)
                            }
                        }
                    }
                } else {
                    if (limitedRegion.getBlockData(x, y, z) == air) {
                        limitedRegion.setBlockData(x, y, z, oak)
                    }
                }
            }

            handleMath(basePos, direction, random, unitDirection, limitedRegion, iterationDepth)
        }
    }

    private fun handleLeaves(
        basePos: Vector,
        limitedRegion: LimitedRegion
    ) {
        placeLeaves(limitedRegion, basePos)
    }

    private fun placeLeaves(limitedRegion: LimitedRegion, basePos: Vector) {
        handleBlockSphere(leafRadius, basePos, limitedRegion, leaves)
    }
}