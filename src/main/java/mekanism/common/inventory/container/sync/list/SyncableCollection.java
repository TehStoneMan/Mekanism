package mekanism.common.inventory.container.sync.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.common.inventory.container.sync.ISyncableData;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.container.property.ByteArrayPropertyData;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Version of {@link net.minecraft.world.inventory.DataSlot} for handling Collections
 */
public abstract class SyncableCollection<TYPE> implements ISyncableData {

    private final Supplier<? extends @NotNull Collection<TYPE>> getter;
    private final Consumer<@NotNull Collection<TYPE>> setter;
    private int lastKnownHashCode;

    protected SyncableCollection(Supplier<? extends @NotNull Collection<TYPE>> getter, Consumer<@NotNull Collection<TYPE>> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @NotNull
    public Collection<TYPE> get() {
        return getRaw();
    }

    @NotNull
    protected Collection<TYPE> getRaw() {
        return getter.get();
    }

    protected int getValueHashCode() {
        return getRaw().hashCode();
    }

    public void set(byte[] rawData) {
        setter.accept(PacketUtils.read(rawData, this::deserializeList));
    }

    protected abstract Collection<TYPE> deserializeList(FriendlyByteBuf buffer);

    protected abstract void serializeListElement(FriendlyByteBuf buffer, TYPE element);

    @Override
    public ByteArrayPropertyData getPropertyData(short property, DirtyType dirtyType) {
        //Note: We write it to a byte array so that we make sure to effectively copy it (force a serialization and deserialization)
        // whenever we send this as a packet rather than potentially allowing the list to leak from one side to the other in single player
        byte[] rawData = FriendlyByteBufUtil.writeCustomData(buffer -> buffer.writeCollection(getRaw(), this::serializeListElement));
        return new ByteArrayPropertyData(property, rawData);
    }

    @Override
    public DirtyType isDirty() {
        int valuesHashCode = getValueHashCode();
        if (lastKnownHashCode == valuesHashCode) {
            return DirtyType.CLEAN;
        }
        //TODO: Create a way to declare changes so we don't have to sync the entire list, when a single element changes
        // Both for removal as well as addition. Note that GuiFrequencySelector makes some assumptions based on the fact
        // that this is not currently possible so a new list will occur each time
        lastKnownHashCode = valuesHashCode;
        return DirtyType.DIRTY;
    }
}