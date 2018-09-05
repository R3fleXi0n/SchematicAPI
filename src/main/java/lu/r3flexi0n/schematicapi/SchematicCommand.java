package lu.r3flexi0n.schematicapi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lu.r3flexi0n.schematicapi.objects.Schematic;
import lu.r3flexi0n.schematicapi.objects.SchematicLocation;
import lu.r3flexi0n.schematicapi.utils.Positions;
import lu.r3flexi0n.schematicapi.utils.Region;
import lu.r3flexi0n.schematicapi.utils.Vector;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SchematicCommand implements CommandExecutor {

    public static final Map<Player, Positions> POSITIONS = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(SchematicAPI.PREFIX + "This command is for players only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 2 && args[0].equalsIgnoreCase("paste")) {

            if (!player.hasPermission("schematicapi.paste")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            File file = new File(SchematicAPI.instance.getDataFolder(), args[1].replace(".schematic", "") + ".schematic");
            if (!file.exists()) {
                player.sendMessage(SchematicAPI.PREFIX + "Schematic not found");
                return true;
            }

            player.sendMessage(SchematicAPI.PREFIX + "Starting to paste schematic...");
            try {
                Schematic schematic = new Schematic(file);
                schematic.paste(player.getLocation(), SchematicAPI.BLOCKS_PER_TICK, (Long time) -> {
                    player.sendMessage(SchematicAPI.PREFIX + "Schematic was pasted in " + (time / 1000F) + " seconds");
                });
            } catch (IOException ex) {
                player.sendMessage(SchematicAPI.PREFIX + "An error occured while pasting");
                ex.printStackTrace();
            }

        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {

            if (!player.hasPermission("schematicapi.list")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            StringBuilder builder = new StringBuilder();
            for (File files : SchematicAPI.instance.getDataFolder().listFiles()) {
                if (files.getName().endsWith(".schematic")) {
                    builder.append(", ").append(files.getName());
                }
            }
            player.sendMessage(SchematicAPI.PREFIX + "Schematics: " + builder.toString().replaceFirst(", ", ""));

        } else if (args.length == 2 && args[0].equalsIgnoreCase("addlocation")) {

            if (!player.hasPermission("schematicapi.addlocation")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            SchematicLocation location = new SchematicLocation(args[1], new Vector(player.getLocation()));

            List<String> locations = SchematicAPI.instance.getConfig().getStringList("Locations");
            locations.add(location.toString());
            SchematicAPI.instance.getConfig().set("Locations", locations);
            SchematicAPI.instance.saveConfig();

            Vector vector = location.getLocation();
            spawnArmorStand(player, player.getWorld(), vector.getX(), vector.getY(), vector.getZ(), vector.getYaw(), vector.getPitch(), location.getKey());

            player.sendMessage(SchematicAPI.PREFIX + "Location '" + args[1] + "' was set");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("removelocation")) {

            if (!player.hasPermission("schematicapi.removelocation")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            int i = 0;

            List<String> locations = SchematicAPI.instance.getConfig().getStringList("Locations");
            Iterator<String> iterator = locations.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().startsWith(args[1])) {
                    iterator.remove();
                    i++;
                }
            }
            SchematicAPI.instance.getConfig().set("Locations", locations);
            SchematicAPI.instance.saveConfig();

            player.sendMessage(SchematicAPI.PREFIX + i + " locations have been removed");

        } else if (args.length == 1 && args[0].equalsIgnoreCase("showlocations")) {

            if (!player.hasPermission("schematicapi.showlocations")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            for (String data : SchematicAPI.instance.getConfig().getStringList("Locations")) {
                SchematicLocation location = new SchematicLocation(data);
                Vector vector = location.getLocation();
                spawnArmorStand(player, player.getWorld(), vector.getX(), vector.getY(), vector.getZ(), vector.getYaw(), vector.getPitch(), location.getKey());
            }

            player.sendMessage(SchematicAPI.PREFIX + "All nearby locations are now shown (babyzombies)");

        } else if (args.length == 1 && (args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2"))) {

            if (!player.hasPermission("schematicapi.pos")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            Location location = player.getLocation();

            Positions positions = POSITIONS.get(player);
            if (positions == null) {
                positions = new Positions();
                POSITIONS.put(player, positions);
            }

            Vector position = new Vector(location.getBlockX(), location.getBlockY(), location.getBlockZ());

            int index = Integer.parseInt(args[0].replace("pos", ""));
            if (index == 2) {
                positions.setPosition2(position);
            } else {
                positions.setPosition1(position);
            }

            player.sendMessage(SchematicAPI.PREFIX + "Position " + index + " was set");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("save")) {

            if (!player.hasPermission("schematicapi.save")) {
                player.sendMessage(SchematicAPI.PREFIX + "No permission");
                return true;
            }

            Positions positions = POSITIONS.get(player);
            if (positions == null || positions.isIncomplete()) {
                player.sendMessage(SchematicAPI.PREFIX + "You have to set 2 positions");
                return true;
            }

            File file = new File(SchematicAPI.instance.getDataFolder(), args[1] + ".schematic");
            if (file.exists()) {
                player.sendMessage(SchematicAPI.PREFIX + "Schematic already exists");
                return true;
            }

            Vector origin = new Vector(player.getLocation());
            Region region = new Region(player.getWorld(), origin, positions.getPosition1(), positions.getPosition2());
            Schematic schematic = new Schematic(region.getWithoutAir());

            for (String data : SchematicAPI.instance.getConfig().getStringList("Locations")) {
                SchematicLocation location = new SchematicLocation(data);
                if (region.isInside(location.getLocation())) {
                    schematic.addLocation(location);
                }
            }

            try {
                file.createNewFile();
                schematic.save(file);
                player.sendMessage(SchematicAPI.PREFIX + "Schematic " + args[1] + " has been saved (" + schematic.getRegion().getSize() + " blocks)");
            } catch (IOException ex) {
                player.sendMessage(SchematicAPI.PREFIX + "Schematic could not be saved");
                ex.printStackTrace();
            }

        } else {
            player.sendMessage(SchematicAPI.PREFIX + "Usage:");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " pos1 | pos2");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " save <name>");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " paste <schematic>");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " list");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " addlocation <key>");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " removelocation <key>");
            player.sendMessage(SchematicAPI.PREFIX + "/" + label + " showlocations");
        }
        return true;
    }

    private void spawnArmorStand(Player player, World world, double x, double y, double z, float yaw, float pitch, String name) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        EntityArmorStand armorStand = new EntityArmorStand(worldServer, x, y, z);
        armorStand.setSmall(true);
        armorStand.setCustomName(name);
        armorStand.setCustomNameVisible(true);

        connection.sendPacket(new PacketPlayOutSpawnEntityLiving(armorStand));
        connection.sendPacket(new PacketPlayOutEntityTeleport(armorStand.getId(), convertDouble(x), convertDouble(y), convertDouble(z), convertFloat(yaw), convertFloat(pitch), false));
        connection.sendPacket(new PacketPlayOutEntityEquipment(armorStand.getId(), 4, CraftItemStack.asNMSCopy(new ItemStack(Material.SKULL_ITEM, 1, (byte) 3))));
    }

    private int convertDouble(double d) {
        return (int) (d * 32.0D);
    }

    private byte convertFloat(float f) {
        return (byte) ((int) (f * 256.0F / 360.0F));
    }
}
