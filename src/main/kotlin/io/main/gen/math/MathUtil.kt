package io.main.gen.math

import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

const val epsilon = 1e-6
fun handleSphereChecking(radius: Int, basePos: Vector, function: (Int, Int, Int) -> Unit) {
    val xCenter = basePos.x
    val yCenter = basePos.y
    val zCenter = basePos.z

    for (x in (xCenter - radius).toInt()..(xCenter + radius).toInt()) {
        for (y in (yCenter - radius).toInt()..(yCenter + radius).toInt()) {
            for (z in (zCenter - radius).toInt()..(zCenter + radius).toInt()) {
                val distanceSquared = (x - xCenter).pow(2) + (y - yCenter).pow(2) + (z - zCenter).pow(2)
                if (distanceSquared <= radius * radius) {
                    function(x, y, z)
                }
            }
        }
    }
}

fun handleNaN(direction: Vector): Boolean {
    return direction.x.isNaN() || direction.y.isNaN() || direction.z.isNaN() || direction.lengthSquared() < epsilon * epsilon
}

fun rotateVectorDebug(vec: Vector, axis: Vector, angle: Double): Vector {
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