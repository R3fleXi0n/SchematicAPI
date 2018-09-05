package lu.r3flexi0n.schematicapi.objects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import lu.r3flexi0n.schematicapi.SchematicAPI;
import lu.r3flexi0n.schematicapi.utils.Region;
import lu.r3flexi0n.schematicapi.utils.Vector;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class Schematic {

    private final List<SchematicBlock> blockList = new ArrayList<>();
    private final List<SchematicTile> tileList = new ArrayList<>();
    private final List<SchematicEntity> entityList = new ArrayList<>();
    private final List<SchematicLocation> locationList = new ArrayList<>();

    private final Region region;

    private BukkitTask task;

    public Schematic(Region region) {

        this.region = region;

        World world = region.getWorld();
        Vector min = region.getMinLocation();
        Vector max = region.getMaxLocation();

        //blocks =========================================================================================
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Vector location = new Vector(x, y, z).subtract(min);
                    SchematicBlock schematicBlock = new SchematicBlock(location, block.getTypeId(), block.getData());
                    blockList.add(schematicBlock);
                }
            }
        }

        //tiles =========================================================================================
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        for (TileEntity tiles : nmsWorld.getTileEntities(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ())) {
            Vector location = new Vector(tiles.getPosition().getX(), tiles.getPosition().getY(), tiles.getPosition().getZ()).subtract(min);
            NBTTagCompound nbt = new NBTTagCompound();
            tiles.b(nbt);
            SchematicTile schematicTile = new SchematicTile(location, nbt);
            schematicTile.updateNBTCoordinates(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            tileList.add(schematicTile);
        }

        //entities =========================================================================================
        for (Entity entities : world.getEntities()) {

            if (entities instanceof Item || entities instanceof Player) {
                continue;
            }

            Vector location = new Vector(entities.getLocation());
            if (!region.isInside(location)) {
                continue;
            }

            net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) entities).getHandle();
            NBTTagCompound nbt = new NBTTagCompound();
            nmsEntity.c(nbt);
            SchematicEntity schematicEntity = new SchematicEntity(entities.getType(), location, nbt);
            entityList.add(schematicEntity);
        }
    }

    public Schematic(File file) throws IOException {

        FileInputStream stream = new FileInputStream(file);

        NBTTagCompound nbt = NBTCompressedStreamTools.a(stream);

        stream.close();

        short width = nbt.getShort("Width");
        short height = nbt.getShort("Height");
        short length = nbt.getShort("Length");

        int offsetX = nbt.getInt("WEOffsetX");
        int offsetY = nbt.getInt("WEOffsetY");
        int offsetZ = nbt.getInt("WEOffsetZ");
        Vector offset = new Vector(offsetX, offsetY, offsetZ);

        int originX = nbt.getInt("WEOriginX");
        int originY = nbt.getInt("WEOriginY");
        int originZ = nbt.getInt("WEOriginZ");
        Vector origin = new Vector(originX, originY, originZ);

        region = new Region(origin, offset, width, height, length);

        //blocks =========================================================================================
        byte[] blockId = nbt.getByteArray("Blocks");
        byte[] blockData = nbt.getByteArray("Data");

        byte[] addId = new byte[0];
        short[] blocks = new short[blockId.length];

        if (nbt.hasKey("AddBlocks")) {
            addId = nbt.getByteArray("AddBlocks");
        }

        for (int index = 0; index < blockId.length; index++) {
            if ((index >> 1) >= addId.length) {
                blocks[index] = (short) (blockId[index] & 0xFF);
            } else {
                if ((index & 1) == 0) {
                    blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockId[index] & 0xFF));
                } else {
                    blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockId[index] & 0xFF));
                }
            }
        }

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    int index = y * width * length + z * width + x;
                    SchematicBlock block = new SchematicBlock(new Vector(x, y, z), blocks[index], blockData[index]);
                    blockList.add(block);
                }
            }
        }

        //tiles =========================================================================================
        NBTTagList tiles = nbt.getList("TileEntities", 10);
        for (int i = 0; i < tiles.size(); i++) {

            NBTTagCompound nbtData = tiles.get(i);

            int x = nbtData.getInt("x");
            int y = nbtData.getInt("y");
            int z = nbtData.getInt("z");

            tileList.add(new SchematicTile(new Vector(x, y, z), nbtData));
        }

        //entities =========================================================================================
        NBTTagList entities = nbt.getList("Entities", 10);
        for (int i = 0; i < entities.size(); i++) {

            NBTTagCompound nbtData = entities.get(i);

            String typeName = nbtData.getString("id");
            EntityType type = EntityType.fromName(typeName);

            NBTTagList position = nbtData.getList("Pos", 6);
            double x = position.d(0);
            double y = position.d(1);
            double z = position.d(2);

            NBTTagList rotation = nbtData.getList("Rotation", 5);
            float yaw = rotation.e(0);
            float pitch = rotation.e(1);

            Vector location = new Vector(x, y, z, yaw, pitch);

            entityList.add(new SchematicEntity(type, location, nbtData));
        }

        //locations =========================================================================================
        NBTTagList locations = nbt.getList("Locations", 8);
        for (int i = 0; i < locations.size(); i++) {
            SchematicLocation location = new SchematicLocation(locations.getString(i));
            locationList.add(location);
        }
    }

    public Region getRegion() {
        return region;
    }

    public void addLocation(SchematicLocation location) {
        locationList.add(location);
    }

    public List<Location> getConvertedLocation(String key, Location pasteLocation) {
        List<Location> result = new ArrayList<>();
        for (SchematicLocation locations : locationList) {
            if (locations.getKey().equals(key)) {
                Vector vector = locations.getLocation().clone().add(new Vector(pasteLocation).add(region.getOffset()).subtract(region.getMinLocation()));
                result.add(vector.toLocation(pasteLocation.getWorld()));
            }
        }
        return result;
    }

    public File save(File file) throws IOException {
        NBTTagCompound nbt = new NBTTagCompound();

        short width = (short) region.getWidth();
        short height = (short) region.getHeight();
        short length = (short) region.getLength();

        nbt.setShort("Width", width);
        nbt.setShort("Height", height);
        nbt.setShort("Length", length);

        nbt.setInt("WEOffsetX", region.getOffset().getBlockX());
        nbt.setInt("WEOffsetY", region.getOffset().getBlockY());
        nbt.setInt("WEOffsetZ", region.getOffset().getBlockZ());

        nbt.setInt("WEOriginX", region.getMinLocation().getBlockX());
        nbt.setInt("WEOriginY", region.getMinLocation().getBlockY());
        nbt.setInt("WEOriginZ", region.getMinLocation().getBlockZ());

        //blocks =========================================================================================
        byte[] blocks = new byte[width * height * length];
        byte[] blockData = new byte[width * height * length];
        byte[] addBlocks = null;
        for (SchematicBlock block : blockList) {

            Vector location = block.getLocation();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            int index = y * width * length + z * width + x;

            if (block.getID() > 255) {
                if (addBlocks == null) {
                    addBlocks = new byte[(blocks.length >> 1) + 1];
                }
                addBlocks[index >> 1] = (byte) (((index & 1) == 0)
                        ? addBlocks[index >> 1] & 0xF0 | (block.getID() >> 8) & 0xF
                        : addBlocks[index >> 1] & 0xF | ((block.getID() >> 8) & 0xF) << 4);
            }

            blocks[index] = (byte) block.getID();
            blockData[index] = block.getData();

        }

        nbt.setByteArray("Blocks", blocks);
        nbt.setByteArray("Data", blockData);
        if (addBlocks != null) {
            nbt.setByteArray("AddBlocks", addBlocks);
        }

        //tiles =========================================================================================
        NBTTagList tiles = new NBTTagList();
        for (SchematicTile tile : tileList) {
            tiles.add(tile.getNBT());
        }
        nbt.set("TileEntities", tiles);

        //entities =========================================================================================
        NBTTagList entities = new NBTTagList();
        for (SchematicEntity entity : entityList) {
            entities.add(entity.getNBT());
        }
        nbt.set("Entities", entities);

        //locations =========================================================================================
        NBTTagList locations = new NBTTagList();
        for (SchematicLocation location : locationList) {
            locations.add(new NBTTagString(location.toString()));
        }
        if (!locations.isEmpty()) {
            nbt.set("Locations", locations);
        }

        FileOutputStream stream = new FileOutputStream(file);
        NBTCompressedStreamTools.a(nbt, stream);
        stream.close();

        return file;
    }

    public void paste(Location location, int bpt, Consumer<Long> consumer) {

        Vector finalLocation = new Vector(location).add(region.getOffset());
        World world = location.getWorld();

        long start = System.currentTimeMillis();

        pasteBlocks(world, finalLocation, bpt, (Long end) -> {
            pasteTiles(world, finalLocation);
            pasteEntities(world, finalLocation);
            consumer.accept(end - start);
        });
    }

    //blocks =========================================================================================
    private void pasteBlocks(World world, Vector pasteLocation, int bpt, Consumer<Long> consumer) {

        List<Chunk> chunksToReload = new ArrayList<>();
        task = Bukkit.getScheduler().runTaskTimer(SchematicAPI.instance, () -> {

            Iterator<SchematicBlock> iterator = blockList.iterator();

            for (int i = 0; i < bpt; i++) {

                if (!iterator.hasNext()) {

                    for (Chunk chunks : chunksToReload) {
                        if (chunks != null) {
                            world.refreshChunk(chunks.getX(), chunks.getZ());
                        }
                    }
                    task.cancel();
                    consumer.accept(System.currentTimeMillis());
                    return;
                }

                SchematicBlock block = iterator.next();

                Vector finalLocation = block.getLocation().clone().add(pasteLocation);

                Chunk chunk = setBlockFast(world, finalLocation.getBlockX(), finalLocation.getBlockY(), finalLocation.getBlockZ(), block.getID(), block.getData());

                if (!chunksToReload.contains(chunk)) {
                    chunksToReload.add(chunk);
                }

                iterator.remove();
            }
        }, 0L, 1L);
    }

    /*
    private void setBlock(World world, int x, int y, int z, int id, byte data) {
        world.getBlockAt(x, y, z).setTypeIdAndData(id, data, false);
    }
     */
    private Chunk setBlockFast(World world, int x, int y, int z, int id, byte data) {
        net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();
        net.minecraft.server.v1_8_R3.Chunk chunk = nmsWorld.getChunkAt(x >> 4, z >> 4);
        try {
            chunk.a(new net.minecraft.server.v1_8_R3.BlockPosition(x & 0xF, y, z & 0xF), net.minecraft.server.v1_8_R3.Block.getById(id).fromLegacyData(data));
        } catch (Exception ex) {
            //TODO FIX (Attempted to place a tile entity at x,y,z where there was no entity tile!)
        }
        return chunk.bukkitChunk;
    }

    //tiles =========================================================================================
    private void pasteTiles(World world, Vector pasteLocation) {
        for (SchematicTile tiles : tileList) {
            Vector finalLocation = tiles.getLocation().clone().add(pasteLocation);
            tiles.updateNBTCoordinates(finalLocation.getBlockX(), finalLocation.getBlockY(), finalLocation.getBlockZ());
            setTile(world, finalLocation.getBlockX(), finalLocation.getBlockY(), finalLocation.getBlockZ(), tiles.getNBT());
        }
    }

    private void setTile(World world, int x, int y, int z, NBTTagCompound nbt) {
        TileEntity tileEntity = ((CraftWorld) world).getTileEntityAt(x, y, z);
        if (tileEntity == null) {
            return;
        }
        tileEntity.a(nbt);
        world.getBlockAt(x, y, z).getState().update();
    }

    //entities =========================================================================================
    private void pasteEntities(World world, Vector pasteLocation) {
        for (SchematicEntity entities : entityList) {
            Vector finalLocation = entities.getLocation().clone().add(pasteLocation).subtract(region.getMinLocation());
            entities.updateNBTCoordinates(finalLocation.getX(), finalLocation.getY(), finalLocation.getZ());
            spawnEntity(world, finalLocation.getX(), finalLocation.getY(), finalLocation.getZ(), entities.getType(), entities.getNBT());
        }
    }

    private void spawnEntity(World world, double x, double y, double z, EntityType type, NBTTagCompound nbt) {
        Entity entity = world.spawnEntity(new Location(world, x, y, z), type);
        ((CraftEntity) entity).getHandle().f(nbt);
    }

}
