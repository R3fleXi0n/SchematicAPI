package lu.r3flexi0n.schematicapi.objects;

import lu.r3flexi0n.schematicapi.utils.Vector;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagDouble;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.entity.EntityType;

public class SchematicEntity {

    private final EntityType type;

    private final Vector location;

    private final NBTTagCompound nbt;

    public SchematicEntity(EntityType type, Vector location, NBTTagCompound nbt) {
        this.type = type;
        this.location = location;
        this.nbt = nbt;
    }

    public EntityType getType() {
        return type;
    }

    public Vector getLocation() {
        return location;
    }

    public NBTTagCompound getNBT() {
        return nbt;
    }

    public void updateNBTCoordinates(double x, double y, double z) {
        NBTTagList position = new NBTTagList();
        position.add(new NBTTagDouble(x));
        position.add(new NBTTagDouble(y));
        position.add(new NBTTagDouble(z));
        nbt.set("Pos", position);
    }
}
