# VelocityUtils by Rexi666

**VelocityUtils** is a plugin for Velocity proxy servers that adds essential utilities for both players and server administrators.  
Its main features include: global alerts, maintenance mode, player reporting system, server location and teleportation commands, and much more.  
Lightweight, configurable, and easy to use — it's perfect for any network looking to improve server management effortlessly.

---

### Dependencies
- Java JDK 17
- Luckperms (Optional for the stafflist)  
- [StaffChatLink-VelocityUtils](https://github.com/Rexi666/StaffChatLink-VelocityUtils) On the backends, needed if you want to use the StaffChat and AdminChat features.  
  This plugin is not required if you don't want to use the StaffChat and AdminChat features.

### Features
- **Broadcast alerts** - Use the command /alert to send a message to all the network. You can change the prefix of the message on the config.
- **MOTD** - You can change the MOTD of your network in the config. If you make a change with your server online, you can use the command `/vu reload` to reload it.
- **Maintenance** - You can set your server on maintenance mode, so only the ones who are on the config can access the server.
- **Report** - User can report players for any reason. Staff can see the reports and tp to the server where the reported is (staff need goto permission for this). Also, you can set up an alert on discord.
- **Find** - You can see in which server a player is.
- **GoTo** - You can teleport to the server where a player is.
- **StaffList** - You can see the staff list of your network.
- **StaffChat** and **AdminChat** - You can enable a staff chat to communicate with other staff members.

### Commands:
#### Staff commands
- `/velocityutils reload` | `/vu reload` - Reload the config
- `/alert <message>` - Send a broadcast message to all the server
- `/maintenance`
  - `<on/off>` - Activate/Deactivate the maintenance mode
  - `<add/remove> <nick>` - Add/Remove someone of the maintenance list exception
- `/find <player>` - Tells you where a player is
- `/goto <player>` - Sends you to the server where the player is
- `/stafflist` - Shows the staff list of your network
- `/staffchat` | `/sc` - Enables/Disables the staff chat
- `/adminchat` | `/ac` - Enables/Disables the admin chat
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
- `velocityutils.stafflist.use` - access to /stafflist
- `velocityutils.stafflist.staff` - permission to be listed in the staff list
- `velocityutils.staffchat` - access to /staffchat
- `velocityutils.adminchat` - access to /adminchat
#### User permissions
- `velocityutils.report.use` - access to /report

### TO DO:
- Lobby command
- Moderation Time