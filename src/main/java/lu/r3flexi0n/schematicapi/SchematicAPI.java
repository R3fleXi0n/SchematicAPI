package lu.r3flexi0n.schematicapi;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SchematicAPI extends JavaPlugin implements Listener {

    public static SchematicAPI instance;

    public static String PREFIX;

    public static int BLOCKS_PER_TICK;

    @Override
    public void onEnable() {
        instance = this;

        getConfig().addDefault("Settings.Prefix", "&8[&6SchematicAPI&8] &7");
        getConfig().addDefault("Settings.BlocksPerTick", 500);
        getConfig().options().copyDefaults(true);
        saveConfig();

        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.Prefix"));
        BLOCKS_PER_TICK = getConfig().getInt("Settings.BlocksPerTick");

        getCommand("schematic").setExecutor(new SchematicCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        SchematicCommand.POSITIONS.remove(e.getPlayer());
    }
}
