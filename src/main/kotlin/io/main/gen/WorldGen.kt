package io.main.gen

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.joml.Vector3L
import org.spongepowered.noise.module.source.Perlin
import org.spongepowered.noise.module.source.Simplex
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt


class WorldGen: ChunkGenerator() {

    private val baseSea = 62
    private val terrainAmplitude = 40
    private val sScale = 0.005
    private val pScale = 0.0001
    private val iScale = 0.000001
    private val sNoise = Simplex()
    private val pNoise = Perlin()
    private val iNoise = Perlin()
    private val landRadius = 400
    private val falloffRadius = 600
    private var distance: Double = 0.0

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

                val islandNoise = iNoise.get(worldX * iScale, 0.0, worldZ * iScale)
                val iNoiseValue = (islandNoise + 1.0) / 2

                val finalHeight = if (iNoiseValue >= 0.6) {
                    val simplexNoise = sNoise.get(worldX * sScale, 0.0, worldZ * sScale)
                    val perlinNoise = pNoise.get(worldX * pScale, 0.0, worldZ * pScale)
                    val sHeight = (baseSea + ((simplexNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val pHeight = (baseSea + ((perlinNoise + 1.0) / 2) * terrainAmplitude).toInt()
                    val height = sHeight * 0.7 + pHeight * 0.3
                    baseSea + ((height - baseSea) * iNoiseValue).toInt()
                } else {
                    baseSea
                }

                /*val simplexNoise = sNoise.get(worldX * sScale, 0.0, worldZ * sScale)
                val perlinNoise = pNoise.get(worldX * pScale, 0.0, worldZ * pScale)
                val sHeight = (baseSea + ((simplexNoise + 1.0) / 2) * terrainAmplitude).toInt()
                val pHeight = (baseSea + ((perlinNoise + 1.0) / 2) * terrainAmplitude).toInt()
                val height = sHeight * 0.7 + pHeight * 0.3
                val finalHeight = baseSea + ((height - baseSea) * handleFalloff(worldX, worldZ)).toInt()*/

                setBlocks(finalHeight, chunk, x, z)
            }
        }
    }

    private fun handleFalloff(x: Int, z: Int): Double {
        distance = sqrt(x.toDouble().pow(2) + z.toDouble().pow(2))

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