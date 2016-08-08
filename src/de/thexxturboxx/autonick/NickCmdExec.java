package de.thexxturboxx.autonick;

import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCmdExec implements CommandExecutor {
	
	AutoNick plugin;
	Statement s;
	
	public NickCmdExec(AutoNick plugin, Statement s) {
		this.plugin = plugin;
		this.s = s;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(p.hasPermission("nick.cmd.nick")) {
				if(args.length == 1) {
					try {
						s.executeUpdate("UPDATE " + AutoNick.TABLE + " SET nicked = '2' WHERE UUID = '" + p.getName() + "'");
						s.executeUpdate("UPDATE " + AutoNick.TABLE + " SET name = '" + args[0] + "' WHERE UUID = '" + p.getName() + "'");
						p.sendMessage(AutoNick.getPrefix() + ChatColor.DARK_RED + "Du wirst als " + ChatColor.GOLD + args[0] + ChatColor.DARK_RED + " spielen!");
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else {
					p.sendMessage(AutoNick.getPrefix() + ChatColor.DARK_RED + "Nutze /xnick <Name>");
				}
			} else {
				p.sendMessage(AutoNick.getPrefix() + ChatColor.DARK_RED + "Dazu hast du keine Erlaubnis!");
			}
		} else {
			plugin.getServer().getLogger().info("Das kann nur ein Spieler machen, du Schlingel ;)");
		}
		return true;
	}
	
}