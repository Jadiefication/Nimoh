package io.main.gen.tree

import io.main.gen.WorldGen
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
import kotlin.math.sin

class TreeGen(
    val worldGen: WorldGen
): BlockPopulator() {

    private val maxDepth = 6
    private val scale = 0.8562
    private val epsilon = 1e-6

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
            if (random.nextInt(500) == 1) {
                val worldY = handleGettingFreeBlock(worldInfo, worldX, worldZ, limitedRegion)
                if (worldY == -0x8) return
                generateFractalTree(Vector(worldX, worldY, worldZ), Vector(
                    (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                    6.0,                               // Main Y direction
                    (random.nextDouble() - 0.5) * 0.1   // Small random Z offset
                ).normalize().multiply(6.0), limitedRegion, random, 0)
            }
        } else {
            return
        }
    }

    private fun handleGettingFreeBlock(worldInfo: WorldInfo, x: Int, z: Int, limitedRegion: LimitedRegion): Int {
        val freeBlock = worldInfo.maxHeight - 1
        for (y in freeBlock downTo 2) {
            if (limitedRegion.getBlockData(x, y, z).material == Material.AIR && (
                        limitedRegion.getBlockData(x, y - 1, z).material == Material.GRASS_BLOCK || limitedRegion.getBlockData(x, y - 1, z).material == Material.DIRT)) {
                return y
            } else {
                continue
            }
        }
        return -0x8
    }

    private fun generateFractalTree(basePos: Vector, direction: Vector, limitedRegion: LimitedRegion, random: Random, iterationDepth: Int) {
        val oak = Material.OAK_LOG.createBlockData() as Orientable
        oak.axis = Axis.Y

        if (handleNaN(direction, iterationDepth)) {
            return
        }

        val unitDirection = direction.clone().normalize()
        if (unitDirection.lengthSquared() < epsilon * epsilon) {
            return
        }

        if (direction.length() < 1 || iterationDepth >= maxDepth) {

            return
        } else {
            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))
                val x = step.x.toInt()
                val y = step.y.toInt()
                val z = step.z.toInt()

                limitedRegion.setBlockData(x, y, z, oak)
            }

            handleMath(basePos, direction, random, unitDirection, limitedRegion, iterationDepth)
        }
    }

    private fun handleNaN(direction: Vector, iterationDepth: Int): Boolean {
        return direction.x.isNaN() || direction.y.isNaN() || direction.z.isNaN() || direction.lengthSquared() < epsilon * epsilon || iterationDepth >= maxDepth
    }

    private fun handleMath(basePos: Vector, direction: Vector, random: Random, unitDirection: Vector, limitedRegion: LimitedRegion, iterationDepth: Int) {
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
        val branchAngleA = Math.toRadians(30.0) + (random.nextDouble() - 0.1)
        val branchAngleB = Math.toRadians(30.0) + (random.nextDouble() - 0.1)

        val rotatedAStep1 = rotateVectorDebug(unitDirection.clone(), axis1, branchAngleA)
        val newDirectionAUnscaled = rotateVectorDebug(rotatedAStep1, axis2, branchAngleB)
            .multiply(newLength)

        val rotatedBStep1 = rotateVectorDebug(unitDirection.clone(), axis1, -branchAngleA)
        val newDirectionBUnscaled = rotateVectorDebug(rotatedBStep1, axis2, branchAngleB)
            .multiply(newLength)

        generateFractalTree(newPos, newDirectionAUnscaled, limitedRegion, random, iterationDepth + 1)
        generateFractalTree(newPos, newDirectionBUnscaled, limitedRegion, random, iterationDepth + 1)
    }

    private fun rotateVectorDebug(vec: Vector, axis: Vector, angle: Double): Vector {
        val x = vec.getX()
        val y = vec.getY()
        val z = vec.getZ()
        val x2 = axis.getX()
        val y2 = axis.getY()
        val z2 = axis.getZ()

        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        val dotProduct = vec.dot(axis)

        val xPrime = x2 * dotProduct * (1.0 - cosTheta) + x * cosTheta + (-z2 * y + y2 * z) * sinTheta
        val yPrime = y2 * dotProduct * (1.0 - cosTheta) + y * cosTheta + (z2 * x - x2 * z) * sinTheta
        val zPrime = z2 * dotProduct * (1.0 - cosTheta) + z * cosTheta + (-y2 * x + x2 * y) * sinTheta

        val result = Vector(xPrime, yPrime, zPrime)
        return result
    }
}