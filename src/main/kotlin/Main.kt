package de.c4vxl

import de.c4vxl.bot.feature.C4Cloud
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

fun main() {
    // create jda
    val jda: JDA = JDABuilder.createDefault("<your_api_key>")
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .setActivity(Activity.customStatus("https://cloud.c4vxl.de/"))
        .build().awaitReady()

    jda.updateCommands().complete() // update all commands

    C4Cloud(jda) // enable c4Cloud Module

    // handle program quit
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Shutting down...")
            jda.shutdown()
        }
    })
}
