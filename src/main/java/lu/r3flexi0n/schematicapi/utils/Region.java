package lu.r3flexi0n.schematicapi.utils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class Region {

    private World world;

    private final Vector minLocation, maxLocation;

    private final Vector offset;
    private final Vector origin;

    public Region(World world, Vector origin, Vector position1, Vector position2) {
        this.world = world;

        double minX = Math.min(position1.getX(), position2.getX());
        double minY = Math.min(position1.getY(), position2.getY());
        double minZ = Math.min(position1.getZ(), position2.getZ());
        minLocation = new Vector(minX, minY, minZ);

        double maxX = Math.max(position1.getX(), position2.getX());
        double maxY = Math.max(position1.getY(), position2.getY());
        double maxZ = Math.max(position1.getZ(), position2.getZ());
        maxLocation = new Vector(maxX, maxY, maxZ);

        offset = minLocation.clone().subtract(origin);
        this.origin = origin;
    }

    public Region(Vector minLocation, Vector offset, int width, int height, int length) {
        this.minLocation = minLocation;

        double maxX = width + minLocation.getX() - 1;
        double maxY = height + minLocation.getY() - 1;
        double maxZ = length + minLocation.getZ() - 1;
        this.maxLocation = new Vector(maxX, maxY, maxZ);

        this.offset = offset;
        origin = minLocation.clone().subtract(offset);
    }

    public Vector origin() {
        return origin;
    }

    public World getWorld() {
        return world;
    }

    public Vector getMinLocation() {
        return minLocation;
    }

    public Vector getMaxLocation() {
        return maxLocation;
    }

    public Vector getOffset() {
        return offset;
    }

    public int getWidth() {
        return maxLocation.getBlockX() - minLocation.getBlockX() + 1;
    }

    public int getHeight() {
        return maxLocation.getBlockY() - minLocation.getBlockY() + 1;
    }

    public int getLength() {
        return maxLocation.getBlockZ() - minLocation.getBlockZ() + 1;
    }

    public int getSize() {
        return getWidth() * getHeight() * getLength();
    }

    public boolean isInside(Vector location) {
        return location.getX() >= minLocation.getX() && location.getX() <= maxLocation.getX()
                && location.getY() >= minLocation.getY() && location.getY() <= maxLocation.getY()
                && location.getZ() >= minLocation.getZ() && location.getZ() <= maxLocation.getZ();
    }

    public Region getWithoutAir() {
        int lowestX = Integer.MAX_VALUE;
        int lowestY = Integer.MAX_VALUE;
        int lowestZ = Integer.MAX_VALUE;

        int highestX = Integer.MIN_VALUE;
        int highestY = Integer.MIN_VALUE;
        int highestZ = Integer.MIN_VALUE;

        for (int x = minLocation.getBlockX(); x < maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y < maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z < maxLocation.getBlockZ(); z++) {

                    if (world.getBlockAt(x, y, z).getType() == Material.AIR) {
                        continue;
                    }

                    if (x < lowestX) {
                        lowestX = x;
                    }
                    if (y < lowestY) {
                        lowestY = y;
                    }
                    if (z < lowestZ) {
                        lowestZ = z;
                    }

                    if (x > highestX) {
                        highestX = x;
                    }
                    if (y > highestY) {
                        highestY = y;
                    }
                    if (z > highestZ) {
                        highestZ = z;
                    }
                }
            }
        }

        for (Entity entities : world.getEntities()) {

            if (entities instanceof Item || entities instanceof Player) {
                continue;
            }

            Vector location = new Vector(entities.getLocation());
            if (!isInside(location)) {
                continue;
            }
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            if (x < lowestX) {
                lowestX = x;
            }
            if (y < lowestY) {
                lowestY = y;
            }
            if (z < lowestZ) {
                lowestZ = z;
            }

            if (x > highestX) {
                highestX = x;
            }
            if (y > highestY) {
                highestY = y;
            }
            if (z > highestZ) {
                highestZ = z;
            }
        }

        Vector lowest = new Vector(lowestX, lowestY, lowestZ);
        Vector highest = new Vector(highestX, highestY, highestZ);

        return new Region(world, origin, lowest, highest);
    }
}
