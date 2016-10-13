package me.kevinnovak.deathfeathers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

// suppress the item id warnings
@SuppressWarnings("deprecation")

public class DeathFeathers extends JavaPlugin implements Listener{
    
    // create deaths.yml file
    public File deathsFile = new File(getDataFolder()+"/deaths.yml");
    public FileConfiguration deathData = YamlConfiguration.loadConfiguration(deathsFile);
    
    // load the item to listen for
    ItemStack deathItem = new ItemStack(getConfig().getInt("item"));
    
    // cooldown hashmaps
    private HashMap<String, Integer> cooldownTime;
    private HashMap<String, BukkitRunnable> cooldownTask;
    
    // ======================
    // Enable
    // ======================
    public void onEnable() {  
        // save default config file if it doesnt exist
        saveDefaultConfig();
        
        // name the death item if it is enabled in the config
        if (getConfig().getBoolean("itemNameEnabled") || getConfig().getBoolean("itemNameRequired")) {
            ItemMeta deathItemMeta = deathItem.getItemMeta();
            deathItemMeta.setDisplayName(convertedLang("itemName"));
            deathItem.setItemMeta(deathItemMeta);
        }
        
        // add lore to the death item if it is enabled in the config
        if (getConfig().getBoolean("itemLoreEnabled") || getConfig().getBoolean("itemLoreRequired")) {
            ItemMeta deathItemMeta = deathItem.getItemMeta();
            
            List<String> list = getConfig().getStringList("itemLore");
            List<String> convertedList = convertedLang(list);
            deathItemMeta.setLore(convertedList);

            deathItem.setItemMeta(deathItemMeta);
        }
        
        // register the listeners
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        // prepare the cooldown hasmaps
        cooldownTime = new HashMap<String, Integer>();
        cooldownTask = new HashMap<String, BukkitRunnable>();
        
        // start metrics
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                info("[DeathFeathers] Metrics Enabled!");
            } catch (IOException e) {
                info("[DeathFeathers] Failed to Start Metrics.");
            }
        } else {
            info("[DeathFeathers] Metrics Disabled.");
        }
        
        // plugin is enabled
        info("[DeathFeathers] Plugin Enabled!");
    }
  
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        // plugin is disabled
        info("[DeathFeathers] Plugin Disabled!");
    }
    
    // ======================
    // Saving Death Locations
    // ======================
    @EventHandler
    // when a player dies...
    public void onDeath(PlayerDeathEvent e) {
        if (e.getEntityType() == EntityType.PLAYER) {
            // saving the players death location
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
            
            // creating the string to log
            String location = player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ();
            String deathLog = convertedLang("deathLog").replace("{PLAYER}", player.getName()).replace("{LOCATION}", location).replace("{WORLD}", player.getLocation().getWorld().getName());
            
            // sending log to console
            if (getConfig().getBoolean("consoleLog")) {
                info("[DeathFeathers] " + player.getName() + " has died at " + location + " in " + player.getLocation().getWorld().getName() + ".");
            }
            
            // sending log to players with permissions
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("deathfeathers.log")) {
                    onlinePlayer.sendMessage(deathLog);
                }
            }
        }
    }
    
    // ======================
    // Events to Set Compass
    // ======================
    @EventHandler
    // when a player respawns...
    public void onRespawn(PlayerRespawnEvent e) {
        // give player the respawn item
        if (getConfig().getBoolean("itemOnRespawn")) {
            e.getPlayer().getInventory().addItem(deathItem);
        }
        
        // set the players compass
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    
    @EventHandler
    // when a player joins...
    public void onPlayerJoin(PlayerJoinEvent e) {
        
        // set the players compass
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    
    @EventHandler
    // when a player changes worlds...
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        
        // set the players compass
        if (getConfig().getBoolean("compassDirection")) {
            setCompass(e.getPlayer());
        }
    }
    
    // ======================
    // Setting Compass
    // ======================
    // sets the players compass to point to their death location
    void setCompass(Player player) {
        // delay setting the compass until the player has had time to properly respawn
        getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
            public void run() {
                String playername = player.getName();
                
                // if player has no death data, dont set the compass
                if (deathData.getString(playername) == null) {
                    return;
                }
                
                // otherwise grab the players death location
                World world = getServer().getWorld(deathData.getString(playername + ".World"));
                int xPos = getCoodinate(playername, 'X');
                int yPos = getCoodinate(playername, 'Y') + getConfig().getInt("numBlocksAbove");
                int zPos = getCoodinate(playername, 'Z');
                Location targetLocation = new Location(world, xPos, yPos, zPos);
                
                // set the players compass to death location
                player.setCompassTarget(targetLocation);
            }
        }, 1 * 20L); // delayed to allow player time to respawn
    }

    // ======================
    // Clicking with Item
    // ======================
    @EventHandler
    // when a player clicks
    public void interact(PlayerInteractEvent event) {
        // get the players name and type of click
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        // if player is left clicking with the death item
        if (getConfig().getBoolean("leftClick") || getConfig().getBoolean("leftClickParticles")) {
            if (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
                if (player.getItemInHand().getType() == deathItem.getType()) {
                    if(getConfig().getBoolean("leftClick")) {
                        // if item name is required
                        if(getConfig().getBoolean("itemNameRequired")) {  
                            
                            // if item does not have a display name
                            if(player.getItemInHand().getItemMeta().getDisplayName() == null) {
                                return;
                            }
                            
                            // if items display name is not the required one
                            if(!(player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(convertedLang("itemName")))) {
                                return;
                            }   
                        }
                        
                        // check for permission
                        if (!player.hasPermission("deathfeathers.item")) {
                            player.sendMessage(convertedLang("notPermitted"));
                            return;
                        }
                    
                        // send the distance to the player
                        sendDistance(player);
                    }
                    if(getConfig().getBoolean("leftClickParticles")) {
                        // if item name is required
                        if(getConfig().getBoolean("itemNameRequired")) {  
                            
                            // if item does not have a display name
                            if(player.getItemInHand().getItemMeta().getDisplayName() == null) {
                                return;
                            }
                            
                            // if items display name is not the required one
                            if(!(player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(convertedLang("itemName")))) {
                                return;
                            }   
                        }
                        
                        // check for permission
                        if (!player.hasPermission("deathfeathers.item")) {
                            player.sendMessage(convertedLang("notPermitted"));
                            return;
                        }
                        
                        sendParticles(player);
                    }
                }
            }
        }
        
     // if player is right clicking with the death item
        if (getConfig().getBoolean("rightClick") || getConfig().getBoolean("rightClickParticles")) {
            if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
                if (player.getItemInHand().getType() == deathItem.getType()) {
                    if(getConfig().getBoolean("rightClick")) {
                        // if item name is required
                        if(getConfig().getBoolean("itemNameRequired")) {  
                            
                            // if item does not have a display name
                            if(player.getItemInHand().getItemMeta().getDisplayName() == null) {
                                return;
                            }
                            
                            // if items display name is not the required one
                            if(!(player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(convertedLang("itemName")))) {
                                return;
                            }   
                        }
                        
                        // check for permission
                        if (!player.hasPermission("deathfeathers.item")) {
                            player.sendMessage(convertedLang("notPermitted"));
                            return;
                        }
                    
                        // send the distance to the player
                        sendDistance(player);
                    }
                    if(getConfig().getBoolean("rightClickParticles")) {
                        // if item name is required
                        if(getConfig().getBoolean("itemNameRequired")) {  
                            
                            // if item does not have a display name
                            if(player.getItemInHand().getItemMeta().getDisplayName() == null) {
                                return;
                            }
                            
                            // if items display name is not the required one
                            if(!(player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(convertedLang("itemName")))) {
                                return;
                            }   
                        }
                        
                        // check for permission
                        if (!player.hasPermission("deathfeathers.item")) {
                            player.sendMessage(convertedLang("notPermitted"));
                            return;
                        }
                        
                        sendParticles(player);
                    }
                }
            }
        }
    }
    
    // ===========================
    // Sending Distance to Player
    // ===========================
    void sendDistance(Player player) {
        String playername = player.getName();
        
        // if the player has not died, let them know
        if (deathData.getString(playername) == null) {
            player.sendMessage(convertedLang("notDied"));
            return;
        }
        
        // otherwise grab the world the player is in
        World world = getServer().getWorld(deathData.getString(playername + ".World"));
        
        // if their death world is the same world they are in
        if (world == player.getWorld()) {
            
            // grab the players death coodinates
            int xPos = getCoodinate(playername, 'X');
            int yPos = getCoodinate(playername, 'Y');
            int zPos = getCoodinate(playername, 'Z');
            
            // grab the players current coodinates
            double pxPos = player.getLocation().getX();
            double pyPos = player.getLocation().getY();
            double pzPos = player.getLocation().getZ();
            
            // calculate the distance
            int distanceToDeath = 0;
            if (getConfig().getBoolean("distance3D")) {
                distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)) + ((yPos - pyPos)*(yPos - pyPos)));
            } else {
                distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
            }
            
            // send that distance to the player
            player.sendMessage(convertedLang("sendDistance").replace("{DISTANCE}", Integer.toString(distanceToDeath)));
            
        // otherwise tell the player their death is in another world
        } else {
            player.sendMessage(convertedLang("anotherWorld"));
        }
    }
    
    // ======================
    // Send Particles
    // ======================
    void sendParticles(Player player) {
        String playername = player.getName();
        
        // if the player has not died, let them know
        if (deathData.getString(playername) == null) {
            player.sendMessage(convertedLang("notDied"));
            return;
        }
        
        // otherwise grab the world the player is in
        World world = getServer().getWorld(deathData.getString(playername + ".World"));
        
        // if their death world is the same world they are in
        if (world == player.getWorld()) {
            int xPos = getCoodinate(playername, 'X');
            int zPos = getCoodinate(playername, 'Z');
            double pxPos = player.getLocation().getX();
            double pzPos = player.getLocation().getZ();
            
            int distanceToDeath = (int) Math.sqrt(((xPos - pxPos)*(xPos - pxPos)) + ((zPos - pzPos)*(zPos - pzPos)));
            
            if (distanceToDeath != 0) {
                double pathLength = getConfig().getInt("particleStopDistance");
                if (distanceToDeath < pathLength) {
                    pathLength = distanceToDeath;
                }
                double m = (zPos - pzPos)/(xPos - pxPos);
                for (double i = getConfig().getInt("particleStartDistance"); i<pathLength; i = i + 0.25) {
                    double d = i;
                    double x = 0;
                    if (pxPos < xPos) {
                        x = (double) (pxPos + (d/(Math.sqrt(1 + (m * m)))));
                    } else if (pxPos > xPos) {
                        x = (double) (pxPos - (d/(Math.sqrt(1 + (m * m)))));
                    }
                    double z = (m*(x - pxPos)) + pzPos;
                    Location test = new Location(world, x,player.getLocation().getY() + 1,z);
                    world.playEffect(test, Effect.COLOURED_DUST, 0);
                }
            }
        } else {
            player.sendMessage(convertedLang("anotherWorld"));
        }
    }

    // ======================
    // Commands
    // ======================
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // ======================
        // Console
        // ======================
        // if command sender is the console, let them know, cancel command
        if (!(sender instanceof Player)) {
            sender.sendMessage(convertedLang("noConsole"));
            return true;
        }
        
        // otherwise the command sender is a player
        final Player player = (Player) sender;
        final String playername = player.getName();
        
        // ======================
        // /finddeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("finddeath")) {
            
            // check for permission
            if (!player.hasPermission("deathfeathers.command")) {
                player.sendMessage(convertedLang("notPermitted"));
                return true;
            
            // send the distance to the player
            } else {
                sendDistance(player);
            }
            
            return true;
        }
        
        // ======================
        // /tpdeath
        // ======================
        if(cmd.getName().equalsIgnoreCase("tpdeath")) {
            
            // if the player is still in cooldown
            if (cooldownTime.containsKey(player.getName())) {
                
                // if the player is still in waiting to teleport
                if(getConfig().getInt("cooldownSeconds") - cooldownTime.get(playername) < 0) {
                    
                    // send a message telling them theyre already teleporting
                    player.sendMessage(convertedLang("tpWait"));
               
                // the player is not waiting to teleport
                } else {
                    
                    // send a message telling them the time to wait
                    player.sendMessage(convertedLang("cooldown").replace("{COOLDOWN}", convertTime(cooldownTime.get(playername))));
                }
                return true;
            }

            // the player is no longer in cooldown
            // if the command sender has not specified a player to tp to
            if(args.length == 0) {
                
                // check for permission
                if (!player.hasPermission("deathfeathers.tp")) {
                    player.sendMessage(convertedLang("notPermitted"));
                    return true;
                
                } else {
                    
                    // if the command sender does not have death data
                    if (deathData.getString(player.getName()) == null) {
                        player.sendMessage(convertedLang("notDied"));
                        return true;
                    
                    // command sender has death data
                    } else {
                        
                        // teleport the player to their own death location
                        teleportPlayer(player, player.getName());
                        
                        if (!player.hasPermission("deathfeathers.tp.bypass")) {
                            // if cooldown in config, set the players cooldown
                            if (getConfig().getInt("cooldownSeconds") > 0) {
                                cooldown(player); 
                            }
                        }
                        return true;
                    }
                }
            }
            
            // command sender has specified a player to tp to
            // set the player as the target
            String target = args[0];
            
            // check for permission to tp to others
            if (!player.hasPermission("deathfeathers.tp.others")) {
                player.sendMessage(convertedLang("notPermitted"));
                return true;
            } else {
                
                // if the target does not have death data
                if (deathData.getString(target) == null) {
                    player.sendMessage(convertedLang("noLocation"));
                    return true;
                
                // target has death data
                } else {
                    
                    // teleport player to targets death location
                    teleportPlayer(player, target);
                    
                    if (!player.hasPermission("deathfeathers.tp.bypass")) {
                        // if cooldown in config, set the players cooldown
                        if (getConfig().getInt("cooldownSeconds") > 0) {
                            cooldown(player); 
                        }
                    }
                    return true;
                }
            }
        }  
        return true;
    }
    
    // ======================
    // Teleport Player
    // ======================
    // teleports player to their death location, or another players death location
    void teleportPlayer(Player player, String target) {
        
        // grabs the provide targets death location
        World world = getServer().getWorld(deathData.getString(target + ".World"));
        int xPos = getCoodinate(target, 'X');
        int yPos = getCoodinate(target, 'Y') + getConfig().getInt("numBlocksAbove");
        int zPos = getCoodinate(target, 'Z');
        Location targetLocation = new Location(world, xPos, yPos, zPos);
        
        // delay teleportation if configured so
        if (getConfig().getInt("delaySeconds") > 0) {
            if (!player.hasPermission("deathfeathers.tp.bypass")) {
                int delaySeconds = getConfig().getInt("delaySeconds");
                player.sendMessage(convertedLang("teleporting").replace("{DELAY}", Integer.toString(delaySeconds)));
                getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
                    public void run() {
                        player.teleport(targetLocation);
                    }
                }, delaySeconds * 20L);
            } else {
                // then teleport player
                player.teleport(targetLocation);
            }
        
        // otherwise just teleport the player
        } else {
            player.teleport(targetLocation);
        }
    }
    
    // =========================
    // Cooldown
    // =========================
    void cooldown(Player player) {
        // get player name
        String playername = player.getName();
        
        // put the player in the hash table with delay time
        cooldownTime.put(playername, getConfig().getInt("cooldownSeconds") + getConfig().getInt("delaySeconds"));
        
        cooldownTask.put(playername, new BukkitRunnable() {
                public void run() {
                    
                        // subtract 1 from cooldown time every second
                        cooldownTime.put(playername, cooldownTime.get(playername) - 1);
                        
                        // if cooldown time reaches 0, then remove the player from the hash table
                        if (cooldownTime.get(playername) == 0) {
                                cooldownTime.remove(playername);
                                cooldownTask.remove(playername);
                                cancel();
                        }
                }
        });
       
        // run this every 20 ticks, or 1 second
        cooldownTask.get(playername).runTaskTimer(this, 20, 20);
    }
    
    // ================================
    // Convert Cooldown Time to String
    // ================================
    String convertTime(int initSeconds) {
        String init = "";
        
        // days
        if ((initSeconds/86400) >= 1) {
            int days = initSeconds/86400;
            initSeconds = initSeconds%86400;
            if (days > 1) {
                init = init + " " + days + " " + getConfig().getString("days");
            } else {
                init = init + " " + days + " " + getConfig().getString("day");
            }
        }
        
        // hours
        if ((initSeconds/3600) >= 1) {
            int hours = initSeconds/3600;
            initSeconds = initSeconds%3600;
            if (hours > 1) {
                init = init + " " + hours + " " + getConfig().getString("hours");
            } else {
                init = init + " " + hours + " " + getConfig().getString("hour");
            }
        }
        
        // minutes
        if ((initSeconds/60) >= 1) {
            int minutes = initSeconds/60;
            initSeconds = initSeconds%60;
            if (minutes > 1) {
                init = init + " " + minutes + " " + getConfig().getString("minutes");
            } else {
                init = init + " " + minutes + " " + getConfig().getString("minute");
            }
        }
        
        // seconds
        if (initSeconds >= 1) {
            if (initSeconds > 1) {
                init = init + " " + initSeconds + " " + getConfig().getString("seconds");
            } else {
                init = init + " " + initSeconds + " " + getConfig().getString("second");
            }
        }
        // remove the initial space
        init = init.substring(1, init.length());
        return init;
    }
    
    // =========================
    // Convert String in Config
    // =========================
    // converts string in config, to a string with colors
    String convertedLang(String toConvert) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(toConvert));
    }
    List<String> convertedLang(List<String> toConvert) {
        List<String> translatedColors = new ArrayList<String>();
        for (String stringToTranslate: toConvert){
            translatedColors.add(ChatColor.translateAlternateColorCodes('&',stringToTranslate));
             
        }
        return translatedColors;
    }
    
    // =========================
    // Info
    // =========================
    // sends an info string to the console
    void info(String toConsole) {
        Bukkit.getServer().getLogger().info(toConsole);
    }
    
    // =========================
    // Get Coodinate
    // =========================
    int getCoodinate(String player, char coodinate) {
        return Integer.parseInt(deathData.getString(player + "." + coodinate));
    }
}
