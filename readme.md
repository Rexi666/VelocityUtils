# VelocityUtils by Rexi666

**VelocityUtils** is a plugin designed for **Velocity proxy servers** that adds essential utilities for both players and server administrators. It aims to simplify network management with useful, configurable, and easy-to-use features.

---

### ✨ Features
- 📣 **Global Alerts** - send broadcast messages across the entire network with /alert.
- 🔧 **Maintenance mode** - restrict server access when needed.
- 📋 **Player Reporting System** - players can report others, and staff receive notifications, including via Discord webhook.
- 🧭 **Player Location Commands** - find which server a player is on with /find and teleport to them with /goto.
- 👥 **StaffList** - view online staff and their connection times with /stafflist and /stafftime.
- 🔐 **StaffChat** and **AdminChat** - private channels for staff communication. (Needs the plugin link installed)
- 📜 **Dynamic MOTD** - customize your network’s MOTD through the config.
- 🧑‍🤝‍🧑 **vList** - shows all connected players, filterable by rank if LuckPerms is installed.
- 🚪 **MoveCommands** - configure shortcuts like /lobby or /survival to move players between servers.
- 🧮 **PlaceholderAPI Support** - includes placeholders for player counters both globally and per server to use in chat, scoreboards, plugins, and other compatible places. (Needs the plugin link installed)

### ⚙️ Dependencies
- ☕ Java JDK 17
- 📚 [Luckperms Velocity](https://luckperms.net/download)
- 🔁 [VelocityUtilsLink (backend plugin)](https://github.com/Rexi666/VelocityUtilsLink/releases/latest) only needed if you want to use StaffChat and AdminChat features or you want to use PlaceholderAPI placeholders.

### 📸 Screenshots
|                                                                                                                  |                         |
|:----------------------------------------------------------------------------------------------------------------:| :--------------------------------------: |
|       ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/report.png)        | ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/report_discord.png) | 
|                                                 *Report Message*                                                 | *Discord Report alert* | 
|      ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/stafflist.png)      | ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/maintenance.png) | 
|                                               *Stafflist Message*                                                | *Maintenance mode MOTD* |
| ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/staffchat_adminchat.png) | ![](https://raw.githubusercontent.com/Rexi666/VelocityUtils/refs/heads/main/Wiki/Images/alert.png) |
|                                        *StaffChat and AdminChat Messages*                                        | *Alert message* |

### 🧪 Commands:
#### Admin commands
- `/velocityutils reload` | `/vu reload` - Reload the config
- `/stafftime <jugador> [day|week|month]` - See how long a staff member has been online on the network.
#### Staff commands
- `/alert <message>` - Send a broadcast message to all the server
- `/maintenance`
  - `<on/off>` - Activate/Deactivate the maintenance mode
  - `<add/remove> <nick>` - Add/Remove someone of the maintenance list exception
- `/find <player>` - Tells you where a player is
- `/goto <player>` - Sends you to the server where the player is
- `/stafflist` - Shows the staff list of your network
- `/staffchat` | `/sc` - Enables/Disables the staff chat
- `/adminchat` | `/ac` - Enables/Disables the admin chat
- `/vlist [server|rank]` - Shows the list of players in your network.
#### User commands
- `/report <user> <reason>` - Report a user to the staff team
- You can configure a MoveCommand to move players to another server, for example /lobby or /survival

### 🔐 Permissions:
#### Admin permissions
- `velocityutils.admin` - access to /velocityutils reload and /vu reload
- `velocityutils.stafftime.exclude` - permission to be excluded from the staff time
- `velocityutils.stafftime.use` - access to /stafftime
- `velocityutils.staffjoin.notify` - permission to be notified when a staff joins/leaves/changes server
#### Staff permissions
- `velocityutils.alert` - access to /alert
- `velocityutils.maintenance` - access to /maintenance
- `velocityutils.find` - access to /find
- `velocityutils.goto` - access to /goto
- `velocityutils.report.see` - access to see reports
- `velocityutils.stafflist.use` - access to /stafflist
- `velocityutils.stafflist.staff` - permission to be listed in the staff list
- `velocityutils.staffchat` - access to /staffchat
- `velocityutils.adminchat` - access to /adminchat
- `velocityutils.stafftime.staff` - permission to be listed in the staff time
- `velocityutils.vlist` - access to /vlist
- `velocityutils.staffjoin.staff` - permission to notify when join/leave/change server to those with the permission notify
#### User permissions
- `velocityutils.report.use` - access to /report
- `velocityutils.movecommand.<command>` - permission to use a MoveCommand, e.g. `velocityutils.movecommand.lobby` to use the command /lobby. Use `velocityutils.movecommand.*` to grant all move commands.

### ✏️ Placeholders:
[VelocityUtilsLink (backend plugin)](https://github.com/Rexi666/VelocityUtilsLink/releases/latest) + [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) needed to be installed on the backends to use these placeholders.
- `%velocityutils_globalplayers%` - Total number of players across the network.
- `%velocityutils_players_<server>%` - Number of players on a specific server.

### 📦 Why Choose VelocityUtils?
- ✅ Lightweight and optimized.
- ⚙️ Highly configurable.
- 🧠 Suitable for small to large Velocity networks.
- 🔗 Discord integration via webhook.

### 💬 Need Help or Support?
📖 Wiki: https://rexi666-plugins.gitbook.io/rexi666/velocityutils

Join my Discord server (Spanish/English):
<p align="center">
  <a href="https://discord.com/invite/a3zkKtrjTr">
    <img src="https://discordapp.com/api/guilds/1025688556779360266/widget.png?style=banner3" alt="Discord Invite"/>
  </a>
</p>