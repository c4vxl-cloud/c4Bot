package de.c4vxl

import de.c4vxl.bot.Bot
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

fun main() {
    // create jda
    val jda: JDA = JDABuilder.createDefault("<your_bot_token>")
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .setActivity(Activity.customStatus("https://cloud.c4vxl.de/"))
        .build().awaitReady()

    jda.guilds.forEach { Bot(jda, it) } // init bot for all guilds
    jda.addEventListener(object : ListenerAdapter() { // register join listener
        override fun onGuildJoin(event: GuildJoinEvent) {
            println("Joined new Guild: ${event.guild.id}") // log
            Bot(event.jda, event.guild) // enable bot for guild
        }
    })

    // handle program quit
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Shutting down...")
            jda.shutdown()
        }
    })
}