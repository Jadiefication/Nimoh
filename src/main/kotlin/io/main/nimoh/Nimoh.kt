package io.main.nimoh

import io.main.gen.WorldGen
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin

class Nimoh : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        return WorldGen()
    }
}
