package com.mastereric.meta.common.blocks.tile;

import com.mastereric.meta.common.blocks.BlockMETA;
import com.mastereric.meta.common.inventory.CompatItemStackHandler;
import com.mastereric.meta.init.ModBlocks;
import com.mastereric.meta.init.ModConfig;
import com.mastereric.meta.init.ModItems;
import com.mastereric.meta.util.ItemUtility;
import com.mastereric.meta.util.LangUtility;
import com.mastereric.meta.util.LogUtility;
import mcjty.lib.tools.ItemStackTools;
import mcjty.lib.tools.WorldTools;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileMETA extends TileEntity implements ITickable, IEnergyStorage {

    public final int TICKS_PER_MOD; // 2500 ticks per mod.

    private CompatItemStackHandler inventoryItemHandler = new CompatItemStackHandler(1);
    private int currentRemainingTicks;
    private String customName;

    private int currentEnergyStorage;

    public TileMETA() {
        currentRemainingTicks = 0;
        customName = "";
        TICKS_PER_MOD = ModConfig.META_FE_PER_MOD / ModConfig.META_FE_PER_TICK;
    }

    // TODO add JEI energy display

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        //TODO add CommonCapabilites wrench to META
        //TODO add CommonCapabilites working to META
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {

        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventoryItemHandler);
        if (capability == CapabilityEnergy.ENERGY)
            return CapabilityEnergy.ENERGY.cast(this);

        return super.getCapability(capability, facing);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, getBlockMetadata(), getUpdateTag());
    }

    @Override
    @SuppressWarnings("MethodCallSideOnly")
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        if (net.getDirection() == EnumPacketDirection.CLIENTBOUND) {
            readFromNBT(pkt.getNbtCompound());
            markDirty();
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setString("CustomName", customName);
        tag.setTag("Inventory", inventoryItemHandler.serializeNBT());
        tag.setInteger("RemainingTicks", getTicksRemaining());
        tag.setInteger("EnergyStorage", currentEnergyStorage);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        customName = tag.getString("CustomName");
        inventoryItemHandler.deserializeNBT(tag.getCompoundTag("Inventory"));
        currentRemainingTicks = tag.getInteger("RemainingTicks");
        currentEnergyStorage = tag.getInteger("EnergyStorage");
        markDirty();
    }

    public String getName() {
        return this.hasCustomName() ? this.customName : LangUtility.getTranslation("container.meta.meta");
    }
    public boolean hasCustomName() {
        return this.customName != null && !this.customName.isEmpty();
    }
    public void setCustomInventoryName(String name) {
        this.customName = name;
    }

    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        currentRemainingTicks = compound.getInteger("RemainingTicks");
        currentEnergyStorage = compound.getInteger("EnergyStorage");
        inventoryItemHandler.deserializeNBT(compound.getCompoundTag("Inventory"));
        markDirty();

        if (compound.hasKey("CustomName")) {
            this.customName = compound.getString("CustomName");
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("RemainingTicks", this.currentRemainingTicks);
        compound.setInteger("EnergyStorage", this.currentEnergyStorage);
        compound.setTag("Inventory", inventoryItemHandler.serializeNBT());

        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    public boolean isActive() {
        // Whether the M.E.T.A. is active for external visual purposes.
        return getTicksRemaining() > 0 && currentEnergyStorage < ModConfig.META_MAX_ENERGY_STORED;
    }

    private void tryConsumeMod() {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (!ItemStackTools.isEmpty(inventoryItemHandler.getStackInSlot(0))) {
                if (inventoryItemHandler.getStackInSlot(0).getItem().equals(ModItems.itemMod) && getTicksRemaining() == 0) {
                    LogUtility.infoSided("Consuming mod in META...");
                    currentRemainingTicks = TICKS_PER_MOD;
                    inventoryItemHandler.setStackInSlot(0, ItemStackTools.getEmptyStack());
                    markDirty();
                }
            }
        }
    }

    private boolean wasActive = false;

    public void update() {
        if (!getWorld().isRemote) {
            tryConsumeMod();

            if (getTicksRemaining() > 0) {
                if (currentEnergyStorage < ModConfig.META_MAX_ENERGY_STORED) {
                    currentRemainingTicks--;
                    currentEnergyStorage = (Math.min(currentEnergyStorage + ModConfig.META_FE_PER_TICK, ModConfig.META_MAX_ENERGY_STORED));
                }
            }
            if (currentEnergyStorage > 0) { //Tesla or IC2 should handle this if enabled, so only do this without tesla
                for (EnumFacing side : EnumFacing.values()) {
                    TileEntity tile = world.getTileEntity(pos.offset(side));
                    if (tile != null) {
                        IEnergyStorage energyStorage = tile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                        if (energyStorage.canReceive()) {
                            int result = energyStorage.receiveEnergy(Math.min(this.currentEnergyStorage, ModConfig.META_OUTPUT), false);
                            this.currentEnergyStorage -= result;
                        }
                    }
                }
            }
            if (wasActive != isActive()) {
                LogUtility.info("Switching block state to %b", isActive());
                getWorld().markBlockRangeForRenderUpdate(pos, pos);
                WorldTools.notifyNeighborsOfStateChange(getWorld(), pos, ModBlocks.blockMETA);
                getWorld().markAndNotifyBlock(pos, getWorld().getChunkFromBlockCoords(pos), getWorld().getBlockState(pos), getWorld().getBlockState(pos).withProperty(BlockMETA.PROPERTY_ACTIVE, isActive()), 3);
            }

            wasActive = isActive();
        }
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.getWorld().getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    public void openInventory(EntityPlayer player) {
    }

    public void closeInventory(EntityPlayer player) {
    }

    /**
     * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For
     * guis use Slot.isItemValid
     */
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return (stack.getItem().equals(ModItems.itemMod));
    }

    public int getTicksRemaining() {
        //LogUtility.infoSided("Remaining: %d", currentRemainingTicks);
        return currentRemainingTicks;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // Never receive energy.
        return 0;
    }
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract())
            return 0;

        int energyExtracted = Math.min(currentEnergyStorage, Math.min(this.currentEnergyStorage, maxExtract));
        if (!simulate)
            currentEnergyStorage -= energyExtracted;
        return energyExtracted;
    }
    @Override
    public int getEnergyStored() {
        // Tell others what my current energy storage is.
        //LogUtility.infoSided("Energy Stored: %d", currentEnergyStorage);
        return currentEnergyStorage;
    }
    @Override
    public int getMaxEnergyStored() {
        return ModConfig.META_MAX_ENERGY_STORED;
    }
    @Override
    public boolean canExtract() {
        return true;
    }
    @Override
    public boolean canReceive() {
        return false;
    }

    public void setCurrentEnergyStorage(int currentEnergyStorage) {
        this.currentEnergyStorage = currentEnergyStorage;
        markDirty();
    }

    public void setRemainingTicks(int remainingTicks) {
        this.currentRemainingTicks = remainingTicks;
        markDirty();
    }

    public void dropItemsFromInventory() {
        ItemUtility.dropItemsFromInventory(getWorld(), getPos(), inventoryItemHandler);
    }

}