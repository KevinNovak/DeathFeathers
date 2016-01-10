package me.kevinnovak.finddeathlocation;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FindDeathLocation extends JavaPlugin implements Listener{
    public File deathsFile = new File(getDataFolder()+"/deaths.yml");
    public FileConfiguration deathData = YamlConfiguration.loadConfiguration(deathsFile);
    
    
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
            deathData.set(player.getName() + ".World", player.getLocation().getWorld().getName());
            deathData.set(player.getName() + ".X", player.getLocation().getBlockX());
            deathData.set(player.getName() + ".Z", player.getLocation().getBlockZ());
            try {
                deathData.save(deathsFile);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
    
    @EventHandler
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        if (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
            if (player.getItemInHand().getType() == Material.FEATHER) {
                String playername = player.getName();
                World world = getServer().getWorld(deathData.getString(playername + ".World"));
                if (world == player.getWorld()) {
                    int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                    int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                    int pxPos = player.getLocation().getBlockX();
                    int pzPos = player.getLocation().getBlockZ();
                    int distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
                    player.sendMessage(ChatColor.GOLD + "Your death location is "+ distanceToDeath + " blocks away!");
                } else {
                    player.sendMessage(ChatColor.RED + "Your death location is in another world!");
                }   
            }
        }
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Console cannot run finddeath!");
            return true;
        }

        Player player = (Player) sender;
        if(cmd.getName().equalsIgnoreCase("finddeath")) {
            String playername = player.getName();
            World world = getServer().getWorld(deathData.getString(playername + ".World"));
            if (world == player.getWorld()) {
                int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                int pxPos = player.getLocation().getBlockX();
                int pzPos = player.getLocation().getBlockZ();
                int distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
                player.sendMessage(ChatColor.GOLD + "Your death location is "+ distanceToDeath + " blocks away!");
            } else {
                player.sendMessage(ChatColor.RED + "Your death location is in another world!");
            }
        }
        return true;
    }
}
