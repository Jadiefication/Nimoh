package io.main.gen.tree.palm

import io.main.gen.math.handleSphereChecking
import io.main.gen.tree.GlobalPOIMap
import io.main.gen.tree.precompute.PrecomputedTree
import io.main.gen.tree.precompute.TreeBlock
import io.main.nimoh.Nimoh.Companion.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Orientable
import org.bukkit.util.Vector
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

private val jLeaves = Material.JUNGLE_LEAVES.createBlockData()
private val jungle = (Material.JUNGLE_WOOD.createBlockData() as Orientable).apply { axis = Axis.Y }

fun precomputePalm(
    origin: Vector,
    direction: Vector
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

        fun handleMath(
            basePos: Vector,
            direction: Vector
        ) {
            val top = basePos.clone().add(direction)
            val twoPi = (2 * Math.PI).toLong()
            val stepSize = Math.PI / 8

            var i = 0.0
            while (i <= twoPi) {
                val leafLength = 5
                val upwardBend = -i * 0.2  // Gives slight upward curve to leaf
                val pos = top.clone().add(Vector(cos(i), upwardBend, sin(i)).multiply(leafLength))
                val unitPos = pos.clone().normalize()

                for (j in 0..pos.length().toInt()) {
                    val step = top.clone().add(unitPos.clone().multiply(j))

                    val newX = step.x.toInt()
                    val newY = step.y.toInt()
                    val newZ = step.z.toInt()
                    addBlock(newX, newY, newZ, jLeaves)
                }

                i += stepSize
            }



        }

        fun generateFractalTree(
            basePos: Vector,
            direction: Vector
        ) {
            val unitDirection = direction.clone().normalize()

            for (i in 0..direction.length().toInt()) {
                val step = basePos.clone().add(unitDirection.clone().multiply(i))

                val offset = Vector(
                    sin(i * 0.2) * 0.5,
                    0.0,
                    sin(i * 0.15) * 0.5
                )
                val pos = step.clone().add(offset)


                val y = pos.y.toInt()
                val x = pos.x.toInt()
                val z = pos.z.toInt()

                when (i) {
                    0 -> handleSphereChecking(1, basePos) { x, y, z ->
                        addBlock(x, y, z, jungle)
                    }
                    else -> {
                        addBlock(x, y, z, jungle)
                    }
                }
            }

            handleMath(basePos, direction)
        }

        generateFractalTree(origin, direction)

        val precomputed = PrecomputedTree(
            origin = origin,
            blocksPerChunk = blocksPerChunk,
            affectedChunks = blocksPerChunk.keys.toSet()
        )

        withContext(Dispatchers.Main) {
            GlobalPOIMap.register(precomputed)
        }
    }
}