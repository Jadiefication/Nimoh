package io.main.nimoh

import io.main.command.CellCommand
import io.main.gen.WorldGen
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import kotlin.random.Random

class Nimoh : JavaPlugin() {

    companion object {
        val scope = CoroutineScope(Dispatchers.Default)
        lateinit var plugin: Nimoh
            private set
        val localRandom = Random(UUID.randomUUID().mostSignificantBits)
    }

    private val values = listOf(
        "baseSea", "terrainAmplitude",
        "sScale", "pScale", "landRadius", "falloffRadius", "cellSize"
    )
    private val worldGen = WorldGen()

    override fun onEnable() {
        plugin = this
        saveResource("config.yml", false)
        saveDefaultConfig()
        values.forEachIndexed { index, value ->
            when (index) {
                0 -> worldGen.baseSea = config.getInt(value, worldGen.baseSea)
                1 -> worldGen.terrainAmplitude = config.getInt(value, worldGen.terrainAmplitude)
                2 -> worldGen.sScale = config.getDouble(value, worldGen.sScale)
                3 -> worldGen.pScale = config.getDouble(value, worldGen.pScale)
                4 -> worldGen.landRadius = config.getInt(value, worldGen.landRadius)
                5 -> worldGen.falloffRadius = config.getInt(value, worldGen.falloffRadius)
                6 -> worldGen.cellSize = config.getInt(value, worldGen.cellSize)
            }
        }

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, {
            it.registrar().register(CellCommand(worldGen).command().build())
        })
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun getDefaultWorldGenerator(
        worldName: String,
        id: String?
    ): ChunkGenerator? {
        return worldGen
    }
}
