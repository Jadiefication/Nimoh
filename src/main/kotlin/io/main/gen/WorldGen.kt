package io.main.gen

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.spongepowered.noise.module.source.Perlin
import java.util.*


class WorldGen: ChunkGenerator() {

    private val baseSea = 62
    private val terrainAmplitude = 120
    private val scale = 0.005
    private val noise = Perlin().apply {
        setSeed(500)
    }

    override fun generateNoise(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunk: ChunkData) {
        for (x in 0..16) {
            for (z in 0..16) {
                val worldX = chunkX * 16 + x
                val worldZ = chunkZ * 16 + z

                val noise = noise.get(worldX.toDouble() * scale, 0.0, worldZ.toDouble() * scale)
                val height = (baseSea + ((noise + 1.0) / 2) * terrainAmplitude).toInt()
                setBlocks(height, chunk, x, z)
            }
        }
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
                chunk.setBlock(x, y, z, Material.GRASS_BLOCK) // Topmost layer grass
            }
        }

        // Place water if below sea level
        for (y in (height + 1)..baseSea) {
            chunk.setBlock(x, y, z, Material.WATER)
        }
    }
}