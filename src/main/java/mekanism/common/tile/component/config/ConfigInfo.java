package mekanism.common.tile.component.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.tile.component.config.slot.ChemicalSlotInfo;
import mekanism.common.tile.component.config.slot.FluidSlotInfo;
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.util.EnumUtils;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigInfo implements IPersistentConfigInfo {

    private final Supplier<Direction> facingSupplier;
    //TODO: Ejecting/can eject, how do we want to use these
    private boolean canEject;
    private boolean ejecting;
    private final Map<RelativeSide, DataType> sideConfig;
    private final Map<DataType, ISlotInfo> slotInfo;
    // used so slot & tank GUIs can quickly reference which color overlay to render
    private final Map<Object, List<DataType>> containerTypeMapping;
    //Not final so that it can be lazily initialized
    private Set<RelativeSide> disabledSides;

    public ConfigInfo(@NotNull Supplier<Direction> facingSupplier) {
        this.facingSupplier = facingSupplier;
        canEject = true;
        ejecting = false;
        sideConfig = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            sideConfig.put(side, DataType.NONE);
        }
        slotInfo = new EnumMap<>(DataType.class);
        containerTypeMapping = new HashMap<>();
    }

    public boolean canEject() {
        return canEject;
    }

    public void setCanEject(boolean canEject) {
        this.canEject = canEject;
    }

    @Override
    public boolean isEjecting() {
        return ejecting;
    }

    @Override
    public void setEjecting(boolean ejecting) {
        this.ejecting = ejecting;
    }

    public void addDisabledSides(@NotNull RelativeSide... sides) {
        if (disabledSides == null) {
            disabledSides = EnumSet.noneOf(RelativeSide.class);
        }
        for (RelativeSide side : sides) {
            disabledSides.add(side);
            sideConfig.put(side, DataType.NONE);
        }
    }

    public boolean isSideEnabled(@NotNull RelativeSide side) {
        if (disabledSides == null) {
            return true;
        }
        return !disabledSides.contains(side);
    }

    @NotNull
    @Override
    public DataType getDataType(@NotNull RelativeSide side) {
        return sideConfig.get(side);
    }

    @Override
    public boolean setDataType(@NotNull DataType dataType, @NotNull RelativeSide side) {
        return isSideEnabled(side) && sideConfig.put(side, dataType) != dataType;
    }

    @NotNull
    public Set<DataType> getSupportedDataTypes() {
        Set<DataType> dataTypes = EnumSet.of(DataType.NONE);
        dataTypes.addAll(slotInfo.keySet());
        return dataTypes;
    }

    public boolean supports(DataType type) {
        return type == DataType.NONE || slotInfo.containsKey(type);
    }

    public void fill(@NotNull DataType dataType) {
        for (RelativeSide side : EnumUtils.SIDES) {
            setDataType(dataType, side);
        }
    }

    @Nullable
    public ISlotInfo getSlotInfo(@NotNull RelativeSide side) {
        return getSlotInfo(getDataType(side));
    }

    @Nullable
    public ISlotInfo getSlotInfo(@NotNull DataType dataType) {
        return slotInfo.get(dataType);
    }

    public void addSlotInfo(@NotNull DataType dataType, @NotNull ISlotInfo info) {
        slotInfo.put(dataType, info);
        // set up mapping
        if (info instanceof ChemicalSlotInfo<?, ?, ?> slotInfo) {
            for (IChemicalTank<?, ?> tank : slotInfo.getTanks()) {
                containerTypeMapping.computeIfAbsent(tank, t -> new ArrayList<>()).add(dataType);
            }
        } else if (info instanceof FluidSlotInfo slotInfo) {
            for (IExtendedFluidTank tank : slotInfo.getTanks()) {
                containerTypeMapping.computeIfAbsent(tank, t -> new ArrayList<>()).add(dataType);
            }
        } else if (info instanceof InventorySlotInfo slotInfo) {
            for (IInventorySlot slot : slotInfo.getSlots()) {
                containerTypeMapping.computeIfAbsent(slot, t -> new ArrayList<>()).add(dataType);
            }
        }
    }

    public List<DataType> getDataTypeForContainer(Object container) {
        return containerTypeMapping.getOrDefault(container, new ArrayList<>());
    }

    public void setDefaults() {
        if (slotInfo.containsKey(DataType.INPUT)) {
            fill(DataType.INPUT);
        }
        if (slotInfo.containsKey(DataType.OUTPUT)) {
            setDataType(DataType.OUTPUT, RelativeSide.RIGHT);
        }
        if (slotInfo.containsKey(DataType.EXTRA)) {
            setDataType(DataType.EXTRA, RelativeSide.BOTTOM);
        }
        if (slotInfo.containsKey(DataType.ENERGY)) {
            setDataType(DataType.ENERGY, RelativeSide.BACK);
        }
    }

    public Set<Direction> getSidesForData(@NotNull DataType dataType) {
        return getSides(type -> type == dataType);
    }

    public Set<Direction> getSides(Predicate<DataType> predicate) {
        Direction facing = facingSupplier.get();
        Set<Direction> directions = null;
        for (Map.Entry<RelativeSide, DataType> entry : sideConfig.entrySet()) {
            if (predicate.test(entry.getValue())) {
                if (directions == null) {
                    //Lazy init the set so that if there are none that match we can just use an empty set
                    // instead of having to initialize an enum set
                    directions = EnumSet.noneOf(Direction.class);
                }
                directions.add(entry.getKey().getDirection(facing));
            }
        }
        return directions == null ? Collections.emptySet() : directions;
    }

    public Set<Direction> getAllOutputtingSides() {
        return getSides(DataType::canOutput);
    }

    public Set<Direction> getSidesForOutput(DataType outputType) {
        return getSides(type -> type == outputType || type == DataType.INPUT_OUTPUT);
    }

    /**
     * @return The new data type
     */
    @NotNull
    public DataType incrementDataType(@NotNull RelativeSide relativeSide) {
        DataType current = getDataType(relativeSide);
        if (isSideEnabled(relativeSide)) {
            DataType newType = current.getNext(this::supports);
            sideConfig.put(relativeSide, newType);
            return newType;
        }
        return current;
    }

    /**
     * @return The new data type
     */
    @NotNull
    public DataType decrementDataType(@NotNull RelativeSide relativeSide) {
        DataType current = getDataType(relativeSide);
        if (isSideEnabled(relativeSide)) {
            DataType newType = current.getPrevious(this::supports);
            sideConfig.put(relativeSide, newType);
            return newType;
        }
        return current;
    }
}