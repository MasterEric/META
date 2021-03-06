package com.mastereric.meta.common.inventory;

import com.mastereric.meta.util.LogUtility;
import net.minecraftforge.energy.EnergyStorage;

public class METAEnergyStorageHandler extends EnergyStorage {
    //    private METAEnergyStorageHandler energyStorageHandler = new METAEnergyStorageHandler(MAX_ENERGY_STORED);

    // The maximum amount received per tick. Since this is a generator this is 0.
    public static final int MAX_RECEIVE = 0;
    // The maximum output per tick.
    public static final int MAX_EXTRACT = 1280;

    public METAEnergyStorageHandler(int capacity) {
        super(capacity, MAX_RECEIVE, MAX_EXTRACT);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive())
            return 0;

        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate)
            energy += energyReceived;
        return energyReceived;
    }



    public void setEnergyStored(int energy) {
        this.energy = energy;
    }

    @Override
    public int getEnergyStored() {
        LogUtility.infoSided("ENERGY: %d"+energy);
        return energy;
    }
}
