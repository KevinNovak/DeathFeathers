package me.kevinnovak.finddeathlocation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

// suppress the item id warnings
@SuppressWarnings("deprecation")

public class FindDeathLocation extends JavaPlugin implements Listener{
    
    // create deaths.yml file
    public File deathsFile = new File(getDataFolder()+"/deaths.yml");
    public FileConfiguration deathData = YamlConfiguration.loadConfiguration(deathsFile);
    // load the item to listen for
    ItemStack item = new ItemStack(getConfig().getInt("item")); 
    // cooldown hashmaps
    private HashMap<Player, Integer> cooldownTime;
    private HashMap<Player, BukkitRunnable> cooldownTask;
    
    // ======================
    // Enable
    // ======================
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        
        
        cooldownTime = new HashMap<Player, Integer>();
        cooldownTask = new HashMap<Player, BukkitRunnable>();
        
        
        
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                Bukkit.getServer().getLogger().info("[FindDeathLocation] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[FindDeathLocation] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[FindDeathLocation] Metrics Disabled.");
        }
        Bukkit.getServer().getLogger().info("[FindDeathLocation] Plugin Enabled!");
    }
  
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("[FindDeathLocation] Plugin Disabled!");
    }
    
    // =========================
    // Convert String in Config
    // =========================
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
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
            // ======================
            // Death Logging
            // ======================
            String location = player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ();
            String deathlog = convertedLang("deathlog").replace("{PLAYER}", player.getName()).replace("{LOCATION}", location).replace("{WORLD}", player.getLocation().getWorld().getName());
            if (getConfig().getBoolean("consoleLog")) {
                Bukkit.getServer().getLogger().info("[FindDeathLocation] " + player.getName() + " has died at " + location + " in " + player.getLocation().getWorld().getName() + ".");
            }
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("finddeathlocation.log")) {
                    onlinePlayer.sendMessage(deathlog);
                }
            }
        }
    }
    
    // ======================
    // Events to Set Compass
    // ======================
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (getConfig().getBoolean("itemOnRespawn")) {
            e.getPlayer().getInventory().addItem(item);
        }
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    
    // ======================
    // Setting Compass
    // ======================
    void setCompass(Player player) {
        getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
            public void run() {
                String playername = player.getName();
                if (deathData.getString(playername) == null) {
                    return;
                }
                World world = getServer().getWorld(deathData.getString(playername + ".World"));
                int xPos = Integer.parseInt(deathData.getString(playername + ".X"));
                int yPos = Integer.parseInt(deathData.getString(playername + ".Y")) + getConfig().getInt("numBlocksAbove");
                int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
                Location targetLocation = new Location(world, xPos, yPos, zPos);
                player.setCompassTarget(targetLocation);
            }
        }, 1 * 20L);
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
            int yPos = Integer.parseInt(deathData.getString(playername + ".Y"));
            int zPos = Integer.parseInt(deathData.getString(playername + ".Z"));
            int pxPos = player.getLocation().getBlockX();
            int pyPos = player.getLocation().getBlockY();
            int pzPos = player.getLocation().getBlockZ();
            int distanceToDeath = 0;
            if (getConfig().getBoolean("distance3D")) {
                distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)) + ((yPos - pyPos)*(yPos - pyPos)));
            } else {
                distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
            }
            player.sendMessage(convertedLang("senddistance").replace("{DISTANCE}", Integer.toString(distanceToDeath)));
        } else {
            player.sendMessage(convertedLang("anotherworld"));
        }
    }
    
    // ======================
    // Teleport Player
    // ======================
    void teleportPlayer(Player player, String target) {
        World world = getServer().getWorld(deathData.getString(target + ".World"));
        int xPos = Integer.parseInt(deathData.getString(target + ".X"));
        int yPos = Integer.parseInt(deathData.getString(target + ".Y")) + getConfig().getInt("numBlocksAbove");
        int zPos = Integer.parseInt(deathData.getString(target + ".Z"));
        Location targetLocation = new Location(world, xPos, yPos, zPos);
        if (getConfig().getInt("delaySeconds") > 0) {
            if (!player.hasPermission("finddeathlocation.tp.bypass")) {
                int delaySeconds = getConfig().getInt("delaySeconds");
                player.sendMessage(convertedLang("teleporting").replace("{DELAY}", Integer.toString(delaySeconds)));
                getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
                    public void run() {
                        player.teleport(targetLocation);
                    }
                }, delaySeconds * 20L);
            } else {
                player.teleport(targetLocation);
            }
        } else {
            player.teleport(targetLocation);
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
        if (getConfig().getBoolean("leftClick")) {
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
        if (getConfig().getBoolean("rightClick")) {
            if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
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
        
        final Player player = (Player) sender;
        // ======================
        // /finddeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("finddeath")) {
            // no permission
            if (!player.hasPermission("finddeathlocation.command")) {
                player.sendMessage(convertedLang("notpermitted"));
                return true;
            } else {
                // run command
                sendDistance(player);
            }
            return true;
        }
        
        
        // ======================
        // /tpdeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("tpdeath")) {
            
            
            
            if (cooldownTime.containsKey(player)) {
                if(getConfig().getInt("cooldownSeconds") - cooldownTime.get(player) < 0) {
                    player.sendMessage(convertedLang("tpwait"));
                } else {
                    player.sendMessage(convertedLang("cooldown").replace("{COOLDOWN}", convertTime(cooldownTime.get(player))));
                }
                return true;
            }

            
            
            // no arguments
            if(args.length == 0) {
                if (!player.hasPermission("finddeathlocation.tp")) {
                    player.sendMessage(convertedLang("notpermitted"));
                    return true;
                } else {
                    if (deathData.getString(player.getName()) == null) {
                        player.sendMessage(convertedLang("notdied"));
                        return true;
                    } else {
                        teleportPlayer(player, player.getName());
                        if (getConfig().getInt("cooldownSeconds") > 0) {
                            cooldown(player); 
                        }
                        return true;
                    }
                }
            }
            // no storage
            String target = args[0];
            if (!player.hasPermission("finddeathlocation.tp.others")) {
                player.sendMessage(convertedLang("notpermitted"));
                return true;
            } else {
                if (deathData.getString(target) == null) {
                    player.sendMessage(convertedLang("nolocation"));
                    return true;
                // run command
                } else {
                    teleportPlayer(player, target);
                    cooldown(player);
                }
            }    
            return true;
        }  
        return true;
    }
    
    void cooldown(Player player) {
        cooldownTime.put(player, getConfig().getInt("cooldownSeconds") + getConfig().getInt("delaySeconds"));
        cooldownTask.put(player, new BukkitRunnable() {
                public void run() {
                        cooldownTime.put(player, cooldownTime.get(player) - 1);
                        if (cooldownTime.get(player) == 0) {
                                cooldownTime.remove(player);
                                cooldownTask.remove(player);
                                cancel();
                        }
                }
        });
       
        cooldownTask.get(player).runTaskTimer(this, 20, 20);
    }
    
    String convertTime(int initSeconds) {
        String init = "";
        if ((initSeconds/86400) >= 1) {
            int days = initSeconds/86400;
            initSeconds = initSeconds%86400;
            if (days > 1) {
                init = init + " " + days + " Days";
            } else {
                init = init + " " + days + " Day";
            }
        }
        if ((initSeconds/3600) >= 1) {
            int hours = initSeconds/3600;
            initSeconds = initSeconds%3600;
            if (hours > 1) {
                init = init + " " + hours + " Hours";
            } else {
                init = init + " " + hours + " Hour";
            }
        }
        if ((initSeconds/60) >= 1) {
            int minutes = initSeconds/60;
            initSeconds = initSeconds%60;
            if (minutes > 1) {
                init = init + " " + minutes + " Minutes";
            } else {
                init = init + " " + minutes + " Minute";
            }
        }
        if (initSeconds >= 1) {
            if (initSeconds > 1) {
                init = init + " " + initSeconds + " Seconds";
            } else {
                init = init + " " + initSeconds + " Second";
            }
        }
        init = init.substring(1, init.length());
        return init;
    }
}
