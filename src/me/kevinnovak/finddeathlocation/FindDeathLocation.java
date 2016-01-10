package me.kevinnovak.finddeathlocation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FindDeathLocation extends JavaPlugin implements Listener{
    public void onEnable() {
        Bukkit.getServer().getLogger().info("FindDeathLocation Enabled!");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }
  
    public void onDisable() {
        Bukkit.getServer().getLogger().info("FindDeathLocation Disabled!");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (e.getEntityType() == EntityType.PLAYER) {
            Player player = e.getEntity();
            player.sendMessage(ChatColor.DARK_BLUE + "You Died");
            getConfig().set(player.getName() + ".X", player.getLocation().getBlockX());
            getConfig().set(player.getName() + ".Y", player.getLocation().getBlockY());
            getConfig().set(player.getName() + ".Z", player.getLocation().getBlockZ());
            getConfig().set(player.getName() + ".World", player.getLocation().getWorld().getName());
            saveConfig();
        }
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Console cannot run finddeath!");
            return true;
        }

        Player player = (Player) sender;
        if(cmd.getName().equalsIgnoreCase("finddeath")) {
            player.sendMessage(ChatColor.GOLD + "Finding death location!");
            String playername = player.getName();
            player.sendMessage("Location: " + getConfig().getString(playername + ".World"));
        }
        return true;
    }
}
