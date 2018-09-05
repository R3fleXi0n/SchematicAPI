package lu.r3flexi0n.schematicapi.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class Vector {

    private double x, y, z;
    private float yaw, pitch;

    public Vector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Vector(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public Vector(String string) {
        String[] data = string.split(",");
        this.x = Double.parseDouble(data[0]);
        this.y = Double.parseDouble(data[1]);
        this.z = Double.parseDouble(data[2]);
        this.yaw = Float.parseFloat(data[3]);
        this.pitch = Float.parseFloat(data[4]);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    private int round(double d) {
        return (int) Math.round(d);
    }

    public int getBlockX() {
        return round(x);
    }

    public int getBlockY() {
        return round(y);
    }

    public int getBlockZ() {
        return round(z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Vector add(Vector vector) {
        this.x += vector.getX();
        this.y += vector.getY();
        this.z += vector.getZ();
        return this;
    }

    public Vector subtract(Vector vector) {
        this.x -= vector.getX();
        this.y -= vector.getY();
        this.z -= vector.getZ();
        return this;
    }

    public Vector clone() {
        return new Vector(x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z + "," + yaw + "," + pitch;
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
