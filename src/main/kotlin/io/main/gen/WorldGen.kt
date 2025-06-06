package io.main.gen

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.joml.Vector2L
import org.joml.Vector3L
import org.spongepowered.noise.module.source.Perlin
import org.spongepowered.noise.module.source.Simplex
import java.util.*
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt


class WorldGen: ChunkGenerator() {

    private val baseSea = 62
    private val terrainAmplitude = 40
    private val sScale = 0.005
    private val pScale = 0.0001
    private val iScale = 0.001
    private val sNoise = Simplex()
    private val pNoise = Perlin()
    private val iNoise = Perlin()
    private val landRadius = 300
    private val falloffRadius = 400
    private var distance: Double = 0.0
    private val cellSize = 1024
    private val islandInCell = mutableMapOf(Vector2L(0, 0) to Vector2L(0, 0))

    override fun generateNoise(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunk: ChunkData) {
        random.setSeed(worldInfo.seed)
        val seed = random.nextInt()
        sNoise.setSeed(seed)
        pNoise.setSeed(seed)
        iNoise.setSeed(seed)
        for (x in 0..16) {
            for (z in 0..16) {
                val worldX = chunkX * 16 + x
                val worldZ = chunkZ * 16 + z

                val cellX = floor((worldX / cellSize).toDouble()).toLong()
                val cellZ = floor((worldZ / cellSize).toDouble()).toLong()

                if (!islandInCell.contains(Vector2L(cellX, cellZ))) {
                    val islandNoise = iNoise.get(worldX * iScale, 0.0, worldZ * iScale)
                    if (islandNoise <= 0.7) {
                        islandInCell.put(Vector2L(cellX, cellZ), Vector2L(worldX.toLong(), worldZ.toLong()))
                    }
                }

                val center = islandInCell[Vector2L(cellX, cellZ)]

                if (center != null) {
                    val simplexNoise = sNoise.get(worldX * sScale, 0.0, worldZ * sScale)
                    val perlinNoise = pNoise.get(worldX * pScale, 0.0, worldZ * pScale)
                    val sHeight = (baseSea + ((simplexNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val pHeight = (baseSea + ((perlinNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val height = sHeight * 0.7 + pHeight * 0.3
                    val finalHeight = baseSea + ((height - baseSea) * handleFalloff(worldX, worldZ, center)).toInt()
                    setBlocks(finalHeight, chunk, x, z)
                } else {
                    setBlocks(baseSea, chunk, x, z)
                }
            }
        }
    }

    private fun handleFalloff(x: Int, z: Int, center: Vector2L): Double {
        val dx = x - center.x
        val dz = z - center.y
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

    private fun setBlocks(height: Int, chunk: ChunkData, x: Int, z: Int) {
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
                handleShore(y, chunk, x, z)
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

    private fun handleShore(y: Int, chunk: ChunkData, x: Int, z: Int) {
        if (63 <= y && y <= 65) {
            chance(chunk, x, y, z)

        } else {
            when (y) {
                66 -> {
                    chance(chunk, x, y, z, 3)
                }
                67 -> {
                    chance(chunk, x, y, z, 4)
                }
                68 -> {
                    chance(chunk, x, y, z, 5)
                }
                else -> {
                    chunk.setBlock(x, y, z, Material.GRASS_BLOCK)
                }
            }
        }
    }

    private fun chance(chunk: ChunkData, x: Int, y: Int, z: Int) {
        if (Random().nextInt() % 2 == 0) {
            chunk.setBlock(x, y, z, Material.SAND)
        } else {
            chunk.setBlock(x, y, z, Material.GRAVEL)
        }
    }
    private fun chance(chunk: ChunkData, x: Int, y: Int, z: Int, probability: Int) {
        if (Random().nextInt() % probability == 0) {
            chance(chunk, x, y, z)
        } else {
            chunk.setBlock(x, y, z, Material.GRASS_BLOCK)
        }
    }
}