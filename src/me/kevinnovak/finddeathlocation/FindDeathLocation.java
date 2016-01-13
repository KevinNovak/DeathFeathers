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

// suppress the item id warnings
@SuppressWarnings("deprecation")

public class FindDeathLocation extends JavaPlugin implements Listener{
    // create deaths.yml file
    public File deathsFile = new File(getDataFolder()+"/deaths.yml");
    public FileConfiguration deathData = YamlConfiguration.loadConfiguration(deathsFile);
    // load the item to listen for
    ItemStack item = new ItemStack(getConfig().getInt("item")); 
    
    // ======================
    // Enable
    // ======================
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getLogger().info("FindDeathLocation Enabled!");
    }
  
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("FindDeathLocation Disabled!");
    }
    
    // =========================
    // Convert String in Config
    // =========================
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
    }
    
    // ===========================
    // Sending Distance to Player
    // ===========================
    void sendDistance(Player player) {
        String playername = player.getName();
        if (deathData.getString(playername) == null) {
            player.sendMessage(convertedLang("notdied"));
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
            player.sendMessage(convertedLang("anotherworld"));
        }   
    }

    // ======================
    // Saving Death Locations
    // ======================
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
    
    // ======================
    // Clicking with Item
    // ======================
    @EventHandler
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        // if player is left clicking with item
        if (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
            if (player.getItemInHand().getType() == item.getType()) {
                // no permission
                if (!player.hasPermission("finddeathlocation.item")) {
                    player.sendMessage(convertedLang("notpermitted"));
                    return;
                }    
                // run command
                sendDistance(player);
            }
        }
    }

    // ======================
    // Commands
    // ======================
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // ======================
        // Console
        // ======================
        if (!(sender instanceof Player)) {
            sender.sendMessage(convertedLang("noconsole"));
            return true;
        }
        
        Player player = (Player) sender;
        // ======================
        // /finddeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("finddeath")) {
            // no permission
            if (!player.hasPermission("finddeathlocation.command")) {
                player.sendMessage(convertedLang("notpermitted"));
                return true;
            }
            // run command
            sendDistance(player);
        }
        
        
        // ======================
        // /tpdeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("tpdeath")) {
            // no permission
            if (!player.hasPermission("finddeathlocation.tp")) {
                player.sendMessage(convertedLang("notpermitted"));
                return true;
            }
            // no arguments
            if(args.length == 0) {
                player.sendMessage(convertedLang("noplayer"));
                return true;
            }
            // no storage
            String playername = args[0];
            if (deathData.getString(playername) == null) {
                player.sendMessage(convertedLang("nolocation"));
                return true;
            // run command
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
