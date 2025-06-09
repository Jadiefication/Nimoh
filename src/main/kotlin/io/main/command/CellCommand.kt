package io.main.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.main.gen.WorldGen
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CellCommand(
    val worldGen: WorldGen
) {

    fun command(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("cell")
            .then(Commands.literal("getIslandCells").executes {
                val sender = it.source.executor

                if (sender?.isOp == true) {
                    sender.sendMessage {
                        Component.text(worldGen.islandInCell.keys.toString())
                    }
                }

                return@executes Command.SINGLE_SUCCESS
            }).then(Tp().command())
    }

    inner class Tp {

        fun command(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands.literal("tp")
                .then(Commands.argument("x", LongArgumentType.longArg())
                    .then(Commands.argument("z", LongArgumentType.longArg()).executes {
                        val x = LongArgumentType.getLong(it, "x")
                        val z = LongArgumentType.getLong(it, "z")
                        val sender = it.source.executor

                        if (sender?.isOp == true) {
                            handleTpToCell(x, z, sender)
                        }

                        return@executes Command.SINGLE_SUCCESS
                    })
                )
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
}