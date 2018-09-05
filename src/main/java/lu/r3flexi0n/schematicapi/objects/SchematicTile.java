package lu.r3flexi0n.schematicapi.objects;

import lu.r3flexi0n.schematicapi.utils.Vector;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagInt;

public class SchematicTile {

    private final Vector location;

    private final NBTTagCompound nbt;

    public SchematicTile(Vector location, NBTTagCompound nbt) {
        this.location = location;
        this.nbt = nbt;
    }

    public Vector getLocation() {
        return location;
    }

    public NBTTagCompound getNBT() {
        return nbt;
    }

    public void updateNBTCoordinates(int x, int y, int z) {
        nbt.set("x", new NBTTagInt(x));
        nbt.set("y", new NBTTagInt(y));
        nbt.set("z", new NBTTagInt(z));
    }
}
