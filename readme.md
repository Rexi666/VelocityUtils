# VelocityUtils by Rexi666
### Dependencies
- Java JDK 17

### Features
- **Broadcast alerts**. Use the command /alert to send a message to all the network. You can change the prefix of the message on the config
- **MOTD**. You can change the MOTD of your network in the config. If you make a change with your server online, you can use the command `/vu reload` to reload it
- **Maintenance** You can set your server on maintenance mode, so only the ones who are on the config can access the server

### Commands:
#### Staff commands
- `/velocityutils reload` | `/vu reload` - Reload the config
- `/alert <message>` - Send a broadcast message to all the server
- `/maintenance`
  - `<on/off>` - Activate/Deactivate the maintenance mode
  - `<add/remove> <nick>` - Add/Remove someone of the maintenance list exception
#### User commands
- `/report <user> <reason>` - Report a user to the staff team

### Permissions:
#### Staff permissions
- `velocityutils.admin` - access to /velocityutils reload and /vu reload
- `velocityutils.alert` - access to /alert
- `velocityutils.maintenance` - access to /maintenance
- `velocityutils.report.see` - access to see reports
#### User permissions
- `velocityutils.report.use` - access to /report

### TO DO:
- Staff Chat & /stafflist
- Command find and goto
- Report command
- Lobby command
