package de.thexxturboxx.autonick;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.huskehhh.mysql.mysql.MySQL;

public class AutoNick extends JavaPlugin implements Listener {
	
	public static AutoNick instance;
	public static File path = new File("plugins/AutoNick"), dataPath;
    private static CommandMap cmap;
	MySQL MySQL = null;
    Connection c = null;
    public static final String TABLE = "AutoNick",
    		DATABASE = "AutoNick";
	
	public static AutoNick getInstance() {
		return instance;
	}
	
	@Override
	public void onEnable() {
		try {
			loadConfiguration();
			if(!getConfig().contains("MySQL.hostname") || getConfig().getString("MySQL.hostname").equals("null")) {
				set("MySQL.hostname", "null");
				set("MySQL.port", "null");
				set("MySQL.username", "null");
				set("MySQL.password", "null");
				getServer().getLogger().info("Bitte gib Deine MySQL-Daten in der Config ein!");
				getServer().shutdown();
			} else {
				MySQL = new MySQL(getConfig().getString("MySQL.hostname"),
								  getConfig().getString("MySQL.port"),
								  DATABASE,
								  getConfig().getString("MySQL.username"),
								  getConfig().getString("MySQL.password"));
				c = MySQL.openConnection();
				Statement s = c.createStatement();
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (UUID VARCHAR(40) PRIMARY KEY, nicked INT, name VARCHAR(40));");
			}
			try {
				if(Bukkit.getServer() instanceof CraftServer) {
					final Field f = CraftServer.class.getDeclaredField("commandMap");
					f.setAccessible(true);
					cmap = (CommandMap) f.get(Bukkit.getServer());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			NickCmd cmd_nick = new NickCmd(this);
			cmap.register("", cmd_nick);
			cmd_nick.setExecutor(new NickCmdExec(this, c.createStatement()));
			getServer().getPluginManager().registerEvents(this, this);
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					for(Player p : getServer().getOnlinePlayers()) {
						if(p.hasPermission("nick.cmd.nick")) {
							try {
								Statement s = c.createStatement();
								ResultSet res = s.executeQuery("SELECT * FROM " + TABLE + " WHERE UUID = '" + p.getUniqueId().toString() + "';");
								int nicked = 0;
								if(res.next())
									nicked = res.getInt("nicked");
								else
									s.executeUpdate("INSERT INTO " + TABLE + " (UUID, nicked, name) VALUES ('" + p.getUniqueId().toString() + "','0','');");
								if(nicked == 2) {
									String name = res.getString("name");
									p.getInventory().setItem(8, getAutoNickItem(name));
								} else
									p.getInventory().setItem(8, getAutoNickItem(nicked == 1));
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}, 0L, 1L);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public void set(String key, Object value) {
		getConfig().set(key, value);
		saveConfig();
	}
	
	public void loadConfiguration() {
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	public void simplePardon(String name) {
		getServer().getBanList(Type.NAME).pardon(name);
	}
	
	public void simpleBan(String name, String reason) {
		getServer().getBanList(Type.NAME).addBan(name, reason, null, null);
	}
	
	public static File getPluginPath() {
		return path;
	}
	
	public static File getDataPath() {
		return dataPath;
	}
	
	public static String getPrefix() {
		return ChatColor.GRAY + "[" + ChatColor.DARK_PURPLE + "NICK" + ChatColor.GRAY + "] ";
	}
	
	public static double round(double value, int decimal) {
	    return (double) Math.round(value * Math.pow(10d, decimal)) / Math.pow(10d, decimal);
	}
	
	@EventHandler
	public void nickSwitch(PlayerInteractEvent e) {
		if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			try {
				Statement s = c.createStatement();
				ResultSet res = s.executeQuery("SELECT * FROM " + TABLE + " WHERE UUID = '" + e.getPlayer().getUniqueId().toString() + "';");
				String name = null;
				if(res.next()) {
					name = res.getString("name");
				}
				if(e.getPlayer().getItemInHand().isSimilar(getAutoNickItem(true)) ||
						(name != null && e.getPlayer().getItemInHand().isSimilar(getAutoNickItem(name)))) {
					e.getPlayer().getInventory().setItem(8, getAutoNickItem(false));
					s.executeUpdate("UPDATE " + TABLE + " SET nicked = '0' WHERE UUID = '" + e.getPlayer().getUniqueId().toString() + "'");
					e.getPlayer().sendMessage(getPrefix() + "§4AutoNick wurde §cdeaktiviert");
				} else if(e.getPlayer().getItemInHand().isSimilar(getAutoNickItem(false))) {
					e.getPlayer().getInventory().setItem(8, getAutoNickItem(true));
					s.executeUpdate("UPDATE " + TABLE + " SET nicked = '1' WHERE UUID = '" + e.getPlayer().getUniqueId().toString() + "'");
					e.getPlayer().sendMessage(getPrefix() + "§4AutoNick wurde §aaktiviert");
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private ItemStack getAutoNickItem(boolean autoNick) {
		ItemStack stack = new ItemStack(Material.NAME_TAG);
		ItemMeta im = stack.getItemMeta();
		im.setDisplayName("§6AutoNick " + booleanToString(autoNick));
		stack.setItemMeta(im);
		return stack;
	}
	
	private ItemStack getAutoNickItem(String name) {
		ItemStack stack = new ItemStack(Material.NAME_TAG);
		ItemMeta im = stack.getItemMeta();
		im.setDisplayName("§6AutoNick: " + "§a" + name);
		stack.setItemMeta(im);
		return stack;
	}
	
	private String booleanToString(boolean b) {
		if(b) {
			return "§aeingeschaltet";
		} else {
			return "§4ausgeschaltet";
		}
	}
	
}