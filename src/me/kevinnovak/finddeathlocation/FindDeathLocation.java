package me.kevinnovak.finddeathlocation;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")

public class FindDeathLocation extends JavaPlugin implements Listener{
    public File deathsFile = new File(getDataFolder()+"/deaths.yml");
    public FileConfiguration deathData = YamlConfiguration.loadConfiguration(deathsFile);
    ItemStack item = new ItemStack(getConfig().getInt("item")); 
    
    public void onEnable() {
        // only if there isnt a configuration file
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getLogger().info("FindDeathLocation Enabled!");
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
            deathData.set(player.getName() + ".Y", player.getLocation().getBlockY());
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
            if (player.getItemInHand().getType() == item.getType()) {
                
                if (!player.hasPermission("finddeathlocation.item")) {
                    player.sendMessage(ChatColor.RED + "You are not permitted to do this!");
                    return;
                }
                
                String playername = player.getName();
                if (deathData.getString(playername) == null) {
                    player.sendMessage(ChatColor.RED + "You have not yet died!");
                    return;
                }
                World world = getServer().getWorld(deathData.getString(playername + ".World"));
                if (world == player.getWorld()) {
                    int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                    int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                    int pxPos = player.getLocation().getBlockX();
                    int pzPos = player.getLocation().getBlockZ();
                    int distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
                    player.sendMessage(ChatColor.GRAY + "Your last death location is currently " + distanceToDeath + " blocks away.");
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
            if (!player.hasPermission("finddeathlocation.command")) {
                player.sendMessage(ChatColor.RED + "You are not permitted to do this!");
                return true;
            }
            String playername = player.getName();
            if (deathData.getString(playername) == null) {
                player.sendMessage(ChatColor.RED + "You have not yet died!");
                return true;
            }
            World world = getServer().getWorld(deathData.getString(playername + ".World"));
            if (world == player.getWorld()) {
                int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                int pxPos = player.getLocation().getBlockX();
                int pzPos = player.getLocation().getBlockZ();
                int distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
                player.sendMessage(ChatColor.GRAY + "Your last death location is currently " + distanceToDeath + " blocks away.");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Your death location is in another world!");
                return true;
            }  
        }
        
        
        if(cmd.getName().equalsIgnoreCase("tpdeath")) {
            if (!player.hasPermission("finddeathlocation.tp")) {
                player.sendMessage(ChatColor.RED + "You are not permitted to do this!");
                return true;
            }
            if(args.length == 0) {
                player.sendMessage(ChatColor.RED + "Please specify a player!");
                return true;
            }
            String playername = args[0];
            if (deathData.getString(playername) == null) {
                player.sendMessage(ChatColor.RED + "Player death location not found!");
                return true;
            } else {
                World world = getServer().getWorld(deathData.getString(playername + ".World"));
                int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                int yPos = Integer.parseInt(deathData.getString(playername + ".Y")) + getConfig().getInt("numBlocksAbove");
                int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                Location targetLocation = new Location(world, xPos, yPos, zPos);
                player.teleport(targetLocation);
            }
            return true;
        }
        
        return true;
    }
}
