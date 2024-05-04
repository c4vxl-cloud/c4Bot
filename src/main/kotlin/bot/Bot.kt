package de.c4vxl.bot

import de.c4vxl.bot.feature.C4Cloud
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData

class Bot(val jda: JDA, val guild: Guild) {
    private val commands: MutableMap<CommandData, (SlashCommandInteractionEvent) -> Unit> = mutableMapOf()

    init {
        println("Initializing Bot for guild ${guild.id}") // log

        C4Cloud(this) // enable c4vxl cloud

        pushCommands() // push commands to guild
        initCommandHandler() // initialize command handling
    }

    fun registerSlashCommand(command: CommandData, handler: (SlashCommandInteractionEvent) -> Unit) {
        commands[command] = handler
    }

    fun pushCommands() {
        guild.updateCommands().addCommands(commands.keys).queue()
    }

    private fun initCommandHandler() {
        jda.addEventListener(object : ListenerAdapter() {
            override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
                if (event.guild != guild) return

                commands[commands.keys.find { it.name == event.name }]?.invoke(event)
            }
        })
    }
}