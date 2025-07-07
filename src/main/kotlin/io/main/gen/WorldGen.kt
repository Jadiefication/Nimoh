package io.main.gen

import io.main.gen.bush.BushGen
import io.main.gen.tree.TreeGen
import io.main.gen.tree.generateFractalTreePrecomputed
import io.main.gen.tree.palm.PalmGen
import io.main.gen.tree.precompute.PrecomputedTree
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.util.Vector
import org.spongepowered.noise.module.source.Perlin
import org.spongepowered.noise.module.source.Simplex
import java.util.*
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt


class WorldGen: ChunkGenerator() {

    var baseSea = 62
    var terrainAmplitude = 40
    var sScale = 0.005
    var pScale = 0.0001
    private val sNoise = Simplex()
    private val pNoise = Perlin()
    var landRadius = 300
    var falloffRadius = 400
    private var distance: Double = 0.0
    var cellSize = 1000
    internal val islandInCell = mutableMapOf<Pair<Long, Long>, Pair<Long, Long>>()
    private var seedsSet = false

    override fun generateNoise(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        chunk: ChunkData
    ) {
        if (!seedsSet) {
            val seed = random.nextInt()
            sNoise.setSeed(seed)
            pNoise.setSeed(seed)
            seedsSet = true
        }
        for (x in 0..16) {
            for (z in 0..16) {
                val worldX = chunkX * 16 + x
                val worldZ = chunkZ * 16 + z

                val cellX = floor((worldX.toDouble() / cellSize)).toLong()
                val cellZ = floor((worldZ.toDouble() / cellSize)).toLong()

                handleIslandCenter(cellX, cellZ, worldInfo.seed)

                val center = islandInCell[(cellX to cellZ)]

                if (center != null) {
                    val simplexNoise = sNoise.get(worldX * sScale, 0.0, worldZ * sScale)
                    val perlinNoise = pNoise.get(worldX * pScale, 0.0, worldZ * pScale)
                    val sHeight = (baseSea + ((simplexNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val pHeight = (baseSea + ((perlinNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val height = sHeight * 0.7 + pHeight * 0.3
                    val finalHeight = baseSea + ((height - baseSea) * handleFalloff(worldX, worldZ, center)).toInt()
                    setBlocks(finalHeight, chunk, x, z, random)
                } else {
                    setBlocks(baseSea, chunk, x, z, random)
                }
            }
        }
    }

    private fun handleIslandCenter(
        cellX: Long,
        cellZ: Long,
        worldSeed: Long
    ) {
        if (!islandInCell.contains(cellX to cellZ)) {
            val centerX = cellX * cellSize + cellSize / 2
            val centerZ = cellZ * cellSize + cellSize / 2

            val cellSeed = worldSeed xor (cellX * 782987399) xor (cellZ * 978937987)
            val cellRandom = Random(cellSeed)

            if (cellRandom.nextInt(10) == 1) {
                islandInCell.put((cellX to cellZ), (centerX to centerZ))
            }
        }
    }

    private fun handleFalloff(
        x: Int,
        z: Int,
        center: Pair<Long, Long>
    ): Double {
        val dx = x - center.first
        val dz = z - center.second
        distance = sqrt(dx.toDouble().pow(2) + dz.toDouble().pow(2))

        var falloffValue: Double
        if (distance <= landRadius) {
            falloffValue = 1.0
        } else if (distance >= falloffRadius) {
            falloffValue = 0.0
        } else {
            val range = falloffRadius - landRadius
            var progress = (distance - landRadius) / range
            progress = progress * progress * (3 - 2 * progress)
            falloffValue = 1.0 - progress
        }
        return falloffValue
    }

    private fun setBlocks(
        height: Int,
        chunk: ChunkData,
        x: Int,
        z: Int,
        random: Random
    ) {
        for (y in 0..height) {
            if (y == 0) {
                chunk.setBlock(x, y, z, Material.BEDROCK)
            } else if (y < baseSea- 2 && y <= height - 5) { // Deeper stone for lower parts
                chunk.setBlock(x, y, z, Material.STONE)
            } else if (y < height - 2) {
                chunk.setBlock(x, y, z, Material.STONE) // Fill lower layers with stone
            } else if (y < height) {
                chunk.setBlock(x, y, z, Material.DIRT) // Top layers dirt
            } else { // y == height
                handleShore(y, chunk, x, z, random)
            }
        }

        if (height == baseSea) {
            for (y in 0..height) {
                if (y == 0) {
                    chunk.setBlock(x, y, z, Material.BEDROCK)
                }
                chunk.setBlock(x, y, z, Material.WATER)
            }
        }
    }

    private fun decideTree(
        x: Int,
        y: Int,
        z: Int,
        random: Random
    ) {
        if (random.nextInt(5000) == 1) {
            val direction = Vector(
                3 + (random.nextDouble() - 0.5) * 0.1, // Small random X offset
                20.0,                               // Main Y direction
                1 + (random.nextDouble() - 0.5) * 0.1   // Small random Z offset
            )
            generateFractalTreePrecomputed(
                Vector(x, y, z),
                direction,
                random
            )
        }
    }

    private fun handleShore(
        y: Int,
        chunk: ChunkData,
        x: Int,
        z: Int,
        random: Random
    ) {
        if (63 <= y && y <= 65) {
            chance(chunk, x, y, z)

        } else {
            when (y) {
                66 -> {
                    chance(chunk, x, y, z, 3, random)
                }
                67 -> {
                    chance(chunk, x, y, z, 4, random)
                }
                68 -> {
                    chance(chunk, x, y, z, 5, random)
                }
                else -> {
                    decideTree(x, y, z, random)
                    chunk.setBlock(x, y, z, Material.GRASS_BLOCK)
                }
            }
        }
    }

    private fun chance(
        chunk: ChunkData,
        x: Int,
        y: Int,
        z: Int
    ) {
        if (Random().nextInt() % 2 == 0) {
            chunk.setBlock(x, y, z, Material.SAND)
        } else {
            chunk.setBlock(x, y, z, Material.GRAVEL)
        }
    }
    private fun chance(
        chunk: ChunkData,
        x: Int,
        y: Int,
        z: Int,
        probability: Int,
        random: Random
    ) {
        if (random.nextInt() % probability == 0) {
            chance(chunk, x, y, z)
        } else {
            decideTree(x, y, z, random)
            chunk.setBlock(x, y, z, Material.GRASS_BLOCK)
        }
    }

    override fun getDefaultPopulators(
        world: World
    ): List<BlockPopulator?> {
        return listOf(TreeGen(), BushGen(this), GrassGen(this), PalmGen(this))
    }
}