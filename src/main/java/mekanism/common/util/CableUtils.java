package mekanism.common.util;

import java.util.Collection;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.common.content.network.distribution.EnergyAcceptorTarget;
import mekanism.common.integration.energy.BlockEnergyCapabilityCache;

public final class CableUtils {

    private CableUtils() {
    }

    public static void emit(Collection<BlockEnergyCapabilityCache> targets, IEnergyContainer energyContainer) {
        emit(targets, energyContainer, energyContainer.getMaxEnergy());
    }

    public static void emit(Collection<BlockEnergyCapabilityCache> targets, IEnergyContainer energyContainer, FloatingLong maxOutput) {
        if (!energyContainer.isEmpty() && !maxOutput.isZero()) {
            energyContainer.extract(emit(targets, energyContainer.extract(maxOutput, Action.SIMULATE, AutomationType.INTERNAL)), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    /**
     * Emits energy from a central block by splitting the received stack among the sides given.
     *
     * @param targets - the list of capabilities to output to
     * @param energyToSend - the energy to output
     *
     * @return the amount of energy emitted
     */
    public static FloatingLong emit(Collection<BlockEnergyCapabilityCache> targets, FloatingLong energyToSend) {
        if (energyToSend.isZero() || targets.isEmpty()) {
            return FloatingLong.ZERO;
        }
        EnergyAcceptorTarget target = new EnergyAcceptorTarget(targets.size());
        for (BlockEnergyCapabilityCache capability : targets) {
            IStrictEnergyHandler handler = capability.getCapability();
            if (handler != null) {
                target.addHandler(handler);
            }
        }
        if (target.getHandlerCount() > 0) {
            return EmitUtils.sendToAcceptors(target, energyToSend);
        }
        return FloatingLong.ZERO;
    }
}