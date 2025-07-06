# VelocityUtils by Rexi666

**VelocityUtils** is a plugin for Velocity proxy servers that adds essential utilities for both players and server administrators.  
Its main features include: global alerts, maintenance mode, player reporting system, server location and teleportation commands, and much more.  
Lightweight, configurable, and easy to use — it's perfect for any network looking to improve server management effortlessly.

---

### Dependencies
- Java JDK 17

### Features
- **Broadcast alerts** - Use the command /alert to send a message to all the network. You can change the prefix of the message on the config.
- **MOTD** - You can change the MOTD of your network in the config. If you make a change with your server online, you can use the command `/vu reload` to reload it.
- **Maintenance** - You can set your server on maintenance mode, so only the ones who are on the config can access the server.
- **Report** - User can report players for any reason. Staff can see the reports and tp to the server where the reported is (staff need goto permission for this). Also, you can setup an alert on discord.
- **Find** - You can see in which server a player is.
- **GoTo** - You can teleport to the server where a player is.

### Commands:
#### Staff commands
- `/velocityutils reload` | `/vu reload` - Reload the config
- `/alert <message>` - Send a broadcast message to all the server
- `/maintenance`
  - `<on/off>` - Activate/Deactivate the maintenance mode
  - `<add/remove> <nick>` - Add/Remove someone of the maintenance list exception
- `/find <player>` - Tells you where a player is
- `/goto <player>` - Sends you to the server where the player is
#### User commands
- `/report <user> <reason>` - Report a user to the staff team

### Permissions:
#### Staff permissions
- `velocityutils.admin` - access to /velocityutils reload and /vu reload
- `velocityutils.alert` - access to /alert
- `velocityutils.maintenance` - access to /maintenance
- `velocityutils.find` - access to /find
- `velocityutils.goto` - access to /goto
- `velocityutils.report.see` - access to see reports
#### User permissions
- `velocityutils.report.use` - access to /report

### TO DO:
- Staff Chat & /stafflist
- Lobby command
