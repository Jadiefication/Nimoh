package io.main.command

import io.main.gen.WorldGen
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CellCommand(
    val worldGen: WorldGen
): Command("command") {

    override fun execute(
        sender: CommandSender,
        commandLabel: String,
        args: Array<out String>
    ): Boolean {
        if (sender.isOp) {
            try {
                when (args.first()) {
                    "getIslandCells" -> {
                        sender.sendMessage {
                            Component.text(worldGen.islandInCell.keys.toString())
                        }
                    }
                    "tp" -> {
                        val cellX = args[1].toLong()
                        val cellZ = args[2].toLong()
                        handleTpToCell(cellX, cellZ, sender)
                    }
                }
                return true
            } catch (e: Exception) {
                sender.sendMessage {
                    Component.text("No args supplied")
                }
                return false
            }
        } else {
            return false
        }
    }

    private fun handleTpToCell(cellX: Long, cellZ: Long, sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage {
                Component.text("Only players may use this command")
            }
            return
        }
        if (worldGen.islandInCell.contains((cellX to cellZ))) {
            val (centerX, centerZ) = worldGen.islandInCell[(cellX to cellZ)]!!
            sender.teleport(Location(sender.world, centerX.toDouble(), sender.y, centerZ.toDouble()))
        } else {
            val centerX = cellX * worldGen.cellSize + worldGen.cellSize / 2
            val centerZ = cellZ * worldGen.cellSize + worldGen.cellSize / 2
            sender.teleport(Location(sender.world, centerX.toDouble(), sender.y, centerZ.toDouble()))
        }
    }
}