package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.utils.C4CloudAPI
import de.c4vxl.bot.utils.EmbedColor
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

class C4Cloud(bot: Bot) {
    val guild: Guild = bot.guild
    val jda: JDA = bot.jda

    init {
        bot.registerSlashCommand(
            Commands.slash("c4vxl-cloud", "Interact with the c4vxl cloud")
                .addSubcommands(
                    SubcommandData("connect", "Connect to your c4vxl cloud Account")
                        .addOption(OptionType.STRING, "api_key", "Pass your API Key", true),

                    SubcommandData("account", "Get an overview of your Account Data"),

                    SubcommandData("logout", "Log out of your account"),

                    SubcommandData("help", "Get an overview of how to use this command")
                        .addOption(OptionType.STRING, "command", "Get detailed information of one command"),

                    SubcommandData("cd", "Change the directory you are currently in")
                        .addOption(OptionType.STRING, "path", "Pass the new directory", true),

                    SubcommandData("cdir", "Get the directory you are currently in"),

                    SubcommandData("editor", "Edit a file")
                        .addOption(OptionType.STRING, "path", "Pass the Path to your file", true)
                ).apply {
                    commandMappings.forEach { mapping ->
                        val scd = SubcommandData(mapping["name"].toString(), mapping["description"].toString())

                        (mapping["args"] as? List<Map<String, Any>>)?.forEach {
                            scd.addOption(OptionType.STRING, it["name"].toString().lowercase(Locale.getDefault()), it["description"].toString(), true)
                        }

                        this.addSubcommands(scd)
                    }
                }
        ) { event: SlashCommandInteractionEvent ->
            when(event.subcommandName?.lowercase(Locale.getDefault())) {
                "connect" -> {
                    val apiKey: String = event.getOption("api_key", OptionMapping::getAsString) ?: return@registerSlashCommand
                    val api: C4CloudAPI = C4CloudAPI(apiKey)

                    if (!api.isValidKey) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_INVALID_API_KEY).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    event.replyEmbeds(Embeds.EMBED_LOGIN_SUCCESS).setEphemeral(true).queue()

                    connectedUsers[event.user] = CloudConnectedUser(event.user, api)
                }

                "account" -> {
                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    event.replyEmbeds(Embeds.EMBED_ACCOUNT_OVERVIEW(user)).setEphemeral(true).queue()
                }

                "logout" -> {
                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    connectedUsers.remove(event.user)

                    event.replyEmbeds(Embeds.EMBED_LOGOUT_SUCCESS).setEphemeral(true).queue()
                }

                "help" -> {
                    val cmd = event.getOption("command", OptionMapping::getAsString)
                    val mapping = commandMappings.find { it["name"]?.toString()?.contentEquals(cmd) == true }

                    if (mapping == null && cmd != null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_CMD_UNKNOWN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    if (cmd == null || mapping == null) event.replyEmbeds(Embeds.EMBED_HELP).setEphemeral(true).queue()
                    else event.replyEmbeds(Embeds.EMBED_HELP_CMD(mapping)).setEphemeral(true).queue()
                }

                "editor" -> {
                    val path: String = event.getOption("path", OptionMapping::getAsString) ?: return@registerSlashCommand
                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }


                    val response = user.api.sendRequest("filesystem_get_file_content", mutableMapOf("path" to path))

                    if (response["error"] == 5) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_PATH_DOES_NOT_EXIST).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    val fileContent: String = response["content"] as? String ?: ""

                    user.currentFile = path

                    val input = TextInput.create("text_input", "Your File:", TextInputStyle.PARAGRAPH)
                        .setMaxLength(4000)

                    if (fileContent != "") input.setValue(if (fileContent.length > 4000) fileContent.substring(0, 4000) else fileContent)

                    event.replyModal(
                        Modal.create("c4vxlcloud_bot_editor_interface", "Text Editor: $path")
                            .addActionRow(
                                input.build()
                            )
                            .build()
                    ).queue()
                }

                "cd" -> {
                    val path: String = event.getOption("path", OptionMapping::getAsString) ?: return@registerSlashCommand

                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    user.currentDirectory = path
                    event.replyEmbeds(Embeds.EMBED_CD_SUCCESS).setEphemeral(true).queue()
                }

                "cdir" -> {
                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    event.replyEmbeds(Embeds.EMBED_CURRENT_DIR(user.currentDirectory)).setEphemeral(true).queue()
                }

                else -> {
                    val user = event.user.asCloudConnectedUser

                    if (user == null) {
                        event.replyEmbeds(Embeds.EMBED_ERROR_NOT_LOGGED_IN).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    val commandName = event.subcommandName
                    val commandArgs = event.getOptionsByType(OptionType.STRING).map { it.asString }.toMutableList()

                    commandMappings.find { it["name"]?.toString()?.contentEquals(commandName) == true }?.let { mappings ->
                        val vars = mutableMapOf<Any, Any>().apply {
                            (mappings["args"] as? List<Map<String, Any>>)?.forEachIndexed { index, argMapping ->
                                if (commandArgs.size <= index) {
                                    event.replyEmbeds(Embeds.EMBED_ERROR_ARGUMENT_MISSING(mutableListOf(
                                        mutableListOf(argMapping["name"].toString(), argMapping["description"].toString())
                                    ))).setEphemeral(true).queue()
                                    return@registerSlashCommand
                                }

                                this[argMapping["name"].toString()] = commandArgs[index]
                            }
                        }

                        if (vars["path"] == "~") vars["path"] = ""
                        if (vars.containsKey("path") && vars["path"] != "/") vars["path"] = user.currentDirectory + "/" + vars["path"]

                        val responseScope = mappings["response"].toString()
                        val response = user.api.sendRequest(mappings["request"].toString(), vars)

                        event.replyEmbeds(
                            Embeds.EMBED_SUCCESS_RUN_RESPONSE(responseScope, response[responseScope].toString(), response["error_message"]?.toString())
                        ).setEphemeral(true).queue()
                    } ?: event.replyEmbeds(Embeds.EMBED_ERROR_RUN_INVALID_COMMAND).setEphemeral(true).queue()
                }
            }
        }

        jda.addEventListener(object : ListenerAdapter() {
            override fun onModalInteraction(event: ModalInteractionEvent) {
                if (!event.modalId.startsWith("c4vxlcloud_bot")) return

                when(event.modalId) {
                    "c4vxlcloud_bot_editor_interface" -> {
                        val fileContent: String = event.getValue("text_input")?.asString ?: return
                        event.user.asCloudConnectedUser?.api?.sendRequest("filesystem_set_file_content", mutableMapOf("path" to (event.user.asCloudConnectedUser?.currentFile ?: return), "newContent" to fileContent)) ?: return
                        event.replyEmbeds(Embeds.EMBED_EDIT_SUCCESS).setEphemeral(true).queue()
                    }
                }
            }
        })
    }

    private object Embeds {
        val EMBED_ERROR_INVALID_API_KEY = EmbedBuilder()
            .setTitle("Error")
            .setDescription("It seems like your API Key is invalid! _Please go to the **[c4vxl cloud Web Interface](https://cloud.c4vxl.de/cloud/)**, navigate to your Account settings and press \"Create new API Key\"_")
            .addField("", "**Account Settings > Create new API Key**", false)
            .setFooter("error_invalid_api_key")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.DANGER.asInt)
            .build()

        val EMBED_ERROR_NOT_LOGGED_IN = EmbedBuilder()
            .setTitle("Error")
            .setDescription("It seems like you are currently not logged into your Account. Please run **/c4vxl-cloud connect <api_key>**, to log into your Account!")
            .setFooter("error_not_logged_in")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.DANGER.asInt)
            .build()

        val EMBED_ERROR_CMD_UNKNOWN = EmbedBuilder()
            .setTitle("Error")
            .setDescription("Unknown command!")
            .setFooter("error_command_unknown")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.DANGER.asInt)
            .build()

        val EMBED_HELP = EmbedBuilder()
            .setTitle("Commands")
            .setDescription("Here's a list of all commands and how to use them. _Tip: Type /c4vxl-cloud help <command> for detailed information of one command_:")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.LIGHT_BLUE.asInt)
            .apply {
                this.addField("", "Bot Configuration:", false)
                this.addField("/c4vxl-cloud connect <api_key>", "Connect to your c4vxl cloud Account", false)
                this.addField("/c4vxl-cloud account", "Get an overview of your Account Data", false)
                this.addField("/c4vxl-cloud logout", "Log out of your account", false)
                this.addField("/c4vxl-cloud help [command]", "Open this Overview", false)
                this.addField("/c4vxl-cloud cd <path>", "Change the directory you are currently in", false)
                this.addField("/c4vxl-cloud cdir", "Get the directory you are currently in", false)
                this.addField("/c4vxl-cloud editor <path>", "Edit a file", false)

                this.addField("", "Cloud Interaction:", false)
                commandMappings.forEach { mapping ->
                    this.addField(("/c4vxl-cloud " + mapping["name"].toString() + " " + (mapping["args"] as? List<Map<String, Any>>)?.map { "<${it["name"]}>" }
                        ?.joinToString(" ")), mapping["description"].toString(), false)
                }
            }
            .build()

        fun EMBED_HELP_CMD(mapping: Map<String, Any>) = EmbedBuilder()
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.LIGHT_BLUE.asInt)
            .addField("Description: ", mapping["description"]?.toString() ?: "`None`", false)
            .apply {
                this.setTitle("/c4vxl-cloud ${mapping["name"]} ${(mapping["args"] as? List<Map<String, Any>>)?.map { "<${it["name"]}>" }?.joinToString(" ") ?: ""}")

                (mapping["args"] as? List<Map<String, Any>>)?.let {
                    this.addField("", "Arguments:", false)

                    it.forEach { arg ->
                        this.addField(arg["name"].toString(), arg["description"].toString(), false)
                    }
                }
            }
            .build()

        fun EMBED_ERROR_ARGUMENT_MISSING(args: MutableList<MutableList<String>>): MessageEmbed {
            val builder = EmbedBuilder()
                .setTitle("Error")
                .setDescription("Argument Missing:")
                .setFooter("error_arg_missing")
                .setTimestamp(Date().toInstant())
                .setColor(EmbedColor.DANGER.asInt)

            args.forEach { builder.addField(it[0], it[1], false) }

            return builder.build()
        }

        val EMBED_ERROR_PATH_DOES_NOT_EXIST = EmbedBuilder()
            .setTitle("Error")
            .setDescription("It seems like the Path you are trying to access does not exist!")
            .setFooter("error_no_path")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.DANGER.asInt)
            .build()

        val EMBED_LOGIN_SUCCESS = EmbedBuilder()
            .setTitle("Success")
            .setDescription("You have been signed into your Account successfully! _Type **/c4vxl-cloud account** to get information about your Account!_")
            .setFooter("success_login")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.SUCCESS.asInt)
            .build()

        val EMBED_LOGOUT_SUCCESS = EmbedBuilder()
            .setTitle("Success")
            .setDescription("Successfully logged out of your Account!")
            .setFooter("success_logout")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.SUCCESS.asInt)
            .build()

        val EMBED_CD_SUCCESS = EmbedBuilder()
            .setTitle("Success")
            .setDescription("Successfully changed your current Directory!")
            .setFooter("success_cd")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.SUCCESS.asInt)
            .build()

        fun EMBED_CURRENT_DIR(dir: String) = EmbedBuilder()
            .setTitle("Current Directory")
            .setDescription("Your current Directory is:")
            .addField(dir, "", false)
            .setFooter("success_cdir")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.LIGHT_BLUE.asInt)
            .build()

        val EMBED_EDIT_SUCCESS = EmbedBuilder()
            .setTitle("Success")
            .setDescription("Successfully edited your File")
            .setFooter("success_editor")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.SUCCESS.asInt)
            .build()

        fun EMBED_ACCOUNT_OVERVIEW(user: CloudConnectedUser): MessageEmbed = EmbedBuilder()
            .setTitle("Account Overview")
            .addField("Username", user.api.username ?: "unknown", true)
            .addField("UUID", user.api.uuid, true)
            .addField("Status", user.api.status ?: "`<Not Set>`", false)
            .addField("Profile Picture", user.api.profilePicURL, false)
            .addField("API Key", "||${user.api.apiKey}||", false)
            .setFooter("success_account_overview")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.LIGHT_BLUE.asInt)
            .build()

        val EMBED_ERROR_RUN_INVALID_COMMAND = EmbedBuilder()
            .setTitle("Error")
            .setDescription("This command does not exist!")
            .setFooter("error_invalid_command")
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.DANGER.asInt)
            .build()

        fun EMBED_SUCCESS_RUN_RESPONSE(responseScope: String, response: String, errorMessage: String?) = EmbedBuilder()
            .setTitle("Success")
            .setDescription("The Server gave the following response:")
            .setFooter("success_run_command")
            .addField(responseScope, response, false)
            .setTimestamp(Date().toInstant())
            .setColor(EmbedColor.SUCCESS.asInt)
            .apply {
                if (errorMessage != null) this.addField("Error", errorMessage, false)
            }
            .build()
    }


    val connectedUsers: MutableMap<User, CloudConnectedUser> = mutableMapOf()

    data class CloudConnectedUser(val user: User, val api: C4CloudAPI, var currentDirectory: String = "/", var currentFile: String? = null)

    private val User.asCloudConnectedUser: CloudConnectedUser? get() {
        return connectedUsers[this]
    }

    companion object {
        var commandMappings: MutableList<Map<String, Any>> = JSONArray(File("commandMappings.json").inputStream().bufferedReader().use { it.readText() }).map { json ->
            if (json is JSONObject) return@map json.toMap()
            else return@map json
        }.toMutableList() as? MutableList<Map<String, Any>> ?: mutableListOf()
    }
}
