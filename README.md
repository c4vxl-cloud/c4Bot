# c4vxl Cloud Discord Bot
This Discord bot allows users to interact with the c4vxl cloud platform using Discord commands. Users can connect their c4vxl cloud account with the bot using their API key, enabling various functionalities such as managing files and folders, creating backups, and more.

### Features
- **Account Overview**: Get an overview of your c4vxl cloud account data.
- **File Management**: Create, delete, move, rename, and edit files and folders.
- **Backup Management**: Create, delete, load, and list backups.
- **Command Help**: Access detailed information about bot commands.
- **Customizable Commands**: You can add support for other endpoints with ease, by modifying the `commandMappings.json` file

### Installation
1. Clone this repository to your local machine.
2. Configure the commandMappings.json class as needed
3. Add your Bot token in the `Main.kt` File
4. Build and Run the Bot

### Usage
To use the bot, invite it to your Discord server and prefix all commands with /c4vxl-cloud.
1. Connect your Account to the Bot: **/c4vxl-cloud connect <api_key>**
2. Use **/c4vxl-cloud help** to get an overview of all important commands

### Adding own Sub-Commands
To add your own endpoints to the commandMappings.json file, follow these steps:

1. Open the commandMappings.json file in your preferred text editor.
2. Add a new JSON object for your custom endpoint following the structure of existing entries.
3. Modify the values of the "name", "description", "request", "response", and "args" fields according to your endpoint's specifications.
4. Save the changes to the commandMappings.json file.

Here's an example of how you can add a custom endpoint:
```json
{
  "name": "custom_command",
  "description": "Description of your custom command",
  "request": "your_custom_request",
  "response": "your_custom_response",
  "args": [
    {
      "name": "param1",
      "description": "Description of parameter 1"
    },
    {
      "name": "param2",
      "description": "Description of parameter 2"
    }
  ]
}
```

After adding your custom endpoint to the `commandMappings.json` file, users can interact with it through the Discord bot by using the specified command name (/c4vxl-cloud custom_command) and providing the required parameters.

