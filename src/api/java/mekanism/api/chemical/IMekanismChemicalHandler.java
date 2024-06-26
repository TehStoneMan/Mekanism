package mekanism.api.chemical;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public interface IMekanismChemicalHandler<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, TANK extends IChemicalTank<CHEMICAL, STACK>>
      extends ISidedChemicalHandler<CHEMICAL, STACK>, IContentsListener {

    /**
     * Used to check if an instance of {@link IMekanismChemicalHandler} actually has the ability to handle chemicals.
     *
     * @return True if we are actually capable of handling chemicals.
     *
     * @apiNote If for some reason you are comparing to {@link IMekanismChemicalHandler} without having gotten the object via the chemical handler capability, then you
     * must call this method to make sure that it really can handle chemicals. As most mekanism tiles have this class in their hierarchy.
     * @implNote If this returns false the capability should not be exposed AND methods should turn reasonable defaults for not doing anything.
     */
    default boolean canHandle() {
        return true;
    }

    /**
     * Returns the list of TANKs that this chemical handler exposes on the given side.
     *
     * @param side The side we are interacting with the handler from (null for internal).
     *
     * @return The list of all TANKs that this {@link IMekanismChemicalHandler} contains for the given side. If there are no tanks for the side or {@link #canHandle()} is
     * false then it returns an empty list.
     *
     * @implNote When side is null (an internal request), this method <em>MUST</em> return all tanks in the handler. Additionally, if {@link #canHandle()} is false, this
     * <em>MUST</em> return an empty list.
     */
    List<TANK> getChemicalTanks(@Nullable Direction side);

    /**
     * Returns the {@link TANK} that has the given index from the list of tanks on the given side.
     *
     * @param tank The index of the tank to retrieve.
     * @param side The side we are interacting with the handler from (null for internal).
     *
     * @return The {@link TANK} that has the given index from the list of tanks on the given side.
     */
    @Nullable
    default TANK getChemicalTank(int tank, @Nullable Direction side) {
        List<TANK> tanks = getChemicalTanks(side);
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank) : null;
    }

    @Override
    default int getTanks(@Nullable Direction side) {
        return getChemicalTanks(side).size();
    }

    @Override
    default STACK getChemicalInTank(int tank, @Nullable Direction side) {
        TANK chemicalTank = getChemicalTank(tank, side);
        return chemicalTank == null ? getEmptyStack() : chemicalTank.getStack();
    }

    @Override
    default void setChemicalInTank(int tank, STACK stack, @Nullable Direction side) {
        TANK chemicalTank = getChemicalTank(tank, side);
        if (chemicalTank != null) {
            chemicalTank.setStack(stack);
        }
    }

    @Override
    default long getTankCapacity(int tank, @Nullable Direction side) {
        TANK chemicalTank = getChemicalTank(tank, side);
        return chemicalTank == null ? 0 : chemicalTank.getCapacity();
    }

    @Override
    default boolean isValid(int tank, STACK stack, @Nullable Direction side) {
        TANK chemicalTank = getChemicalTank(tank, side);
        return chemicalTank != null && chemicalTank.isValid(stack);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Any overrides to this should also override {@link #insertChemical(ChemicalStack, Direction, Action)} as it bypasses calling this method in order to skip
     * looking up the containers for every sub operation.
     */
    @Override
    default STACK insertChemical(int tank, STACK stack, @Nullable Direction side, Action action) {
        TANK chemicalTank = getChemicalTank(tank, side);
        return chemicalTank == null ? stack : chemicalTank.insert(stack, action, AutomationType.handler(side));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Any overrides to this should also override {@link #extractChemical(long, Direction, Action)} and
     * {@link #extractChemical(ChemicalStack, Direction, Action)} as they bypass calling this method in order to skip looking up the containers for every sub
     * operation.
     */
    @Override
    default STACK extractChemical(int tank, long amount, @Nullable Direction side, Action action) {
        TANK chemicalTank = getChemicalTank(tank, side);
        return chemicalTank == null ? getEmptyStack() : chemicalTank.extract(amount, action, AutomationType.handler(side));
    }

    @Override
    default STACK insertChemical(STACK stack, @Nullable Direction side, Action action) {
        return ChemicalUtils.insert(stack, side, this::getChemicalTanks, action, AutomationType.handler(side), getEmptyStack());
    }

    @Override
    default STACK extractChemical(long amount, @Nullable Direction side, Action action) {
        return ChemicalUtils.extract(amount, side, this::getChemicalTanks, action, AutomationType.handler(side), getEmptyStack());
    }

    @Override
    default STACK extractChemical(STACK stack, @Nullable Direction side, Action action) {
        return ChemicalUtils.extract(stack, side, this::getChemicalTanks, action, AutomationType.handler(side), getEmptyStack());
    }
}