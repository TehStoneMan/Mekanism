package mekanism.common.item;

import java.util.List;
import java.util.Map;

import cofh.api.energy.IEnergyContainerItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import mekanism.api.EnumColor;
import mekanism.api.MekanismConfig.general;
import mekanism.api.energy.IEnergizedItem;
import mekanism.client.MekKeyHandler;
import mekanism.client.MekanismKeyHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.FluidItemWrapper;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.IRedstoneControl.RedstoneControl;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ISustainedInventory;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.ItemCapabilityWrapper;
import mekanism.common.integration.IC2ItemManager;
import mekanism.common.integration.TeslaItemWrapper;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.security.ISecurityTile.SecurityMode;
import mekanism.common.tile.TileEntityBasicBlock;
import mekanism.common.tile.TileEntityElectricBlock;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/*
 * DIGITAL_MINER
 * typeBlock = MachineBlock.MACHINE_BLOCK_1;
 * meta = 4;
 * machineName = "DigitalMiner";
 * guiId = 2;
 * tileEntityClass = TileEntityDigitalMiner.class;
 * isElectric = true;
 * hasModel = true;
 * supportsUpgrades = true;
 * facingPredicate = Plane.HORIZONTAL;
 * activable = true;
 * 
 * usage.digitalMinerUsage;
 */
@InterfaceList({
	@Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = "IC2")
})
public class ItemBlockDigitalMiner extends ItemBlock implements IEnergizedItem, ISpecialElectricItem, ISustainedInventory, IEnergyContainerItem, ISecurityItem
{
	public Block metaBlock;

	public ItemBlockDigitalMiner( Block block )
	{
		super(block);
		metaBlock = block;
		setHasSubtypes(true);
		setNoRepair();
		setMaxStackSize(1);
	}


	@Override
	public int getMetadata(int i)
	{
		return i;
	}


	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemstack, EntityPlayer entityplayer, List<String> list, boolean flag)
	{
		if(!MekKeyHandler.getIsKeyPressed(MekanismKeyHandler.sneakKey))
		{
			list.add(LangUtils.localize("tooltip.hold") + " " + EnumColor.INDIGO + GameSettings.getKeyDisplayString(MekanismKeyHandler.sneakKey.getKeyCode()) + EnumColor.GREY + " " + LangUtils.localize("tooltip.forDetails") + ".");
			list.add(LangUtils.localize("tooltip.hold") + " " + EnumColor.AQUA + GameSettings.getKeyDisplayString(MekanismKeyHandler.sneakKey.getKeyCode()) + EnumColor.GREY + " " + LangUtils.localize("tooltip.and") + " " + EnumColor.AQUA + GameSettings.getKeyDisplayString(MekanismKeyHandler.modeSwitchKey.getKeyCode()) + EnumColor.GREY + " " + LangUtils.localize("tooltip.forDesc") + ".");
		}
		else if(!MekKeyHandler.getIsKeyPressed(MekanismKeyHandler.modeSwitchKey))
		{
			if(hasSecurity(itemstack))
			{
				list.add(SecurityUtils.getOwnerDisplay(entityplayer.getName(), getOwner(itemstack)));
				list.add(EnumColor.GREY + LangUtils.localize("gui.security") + ": " + SecurityUtils.getSecurityDisplay(itemstack, Side.CLIENT));
				
				if(SecurityUtils.isOverridden(itemstack, Side.CLIENT))
				{
					list.add(EnumColor.RED + "(" + LangUtils.localize("gui.overridden") + ")");
				}
			}
				
			list.add(EnumColor.BRIGHT_GREEN + LangUtils.localize("tooltip.storedEnergy") + ": " + EnumColor.GREY + MekanismUtils.getEnergyDisplay(getEnergy(itemstack)));

			list.add(EnumColor.AQUA + LangUtils.localize("tooltip.inventory") + ": " + EnumColor.GREY + LangUtils.transYesNo(getInventory(itemstack) != null && getInventory(itemstack).tagCount() != 0));

			if(ItemDataUtils.hasData(itemstack, "upgrades"))
			{
				Map<Upgrade, Integer> upgrades = Upgrade.buildMap(ItemDataUtils.getDataMap(itemstack));
				
				for(Map.Entry<Upgrade, Integer> entry : upgrades.entrySet())
				{
					list.add(entry.getKey().getColor() + "- " + entry.getKey().getName() + (entry.getKey().canMultiply() ? ": " + EnumColor.GREY + "x" + entry.getValue(): ""));
				}
			}
		}
		else {
			list.addAll(MekanismUtils.splitTooltip(MachineType.DIGITAL_MINER.getDescription(), itemstack));
		}
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state)
	{
		boolean place = true;
		
		for(int xPos = -1; xPos <= +1; xPos++)
		{
			for(int yPos = 0; yPos <= +1; yPos++)
			{
				for(int zPos = -1; zPos <= +1; zPos++)
				{
					BlockPos pos1 = pos.add(xPos, yPos, zPos);
					Block b = world.getBlockState(pos1).getBlock();

					if(pos1.getY() > 255 || !b.isReplaceable(world, pos1))
					{
						place = false;
					}
				}
			}
		}

		if(place && super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, state))
		{
			TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);

			if(tileEntity instanceof ISecurityTile)
			{
				ISecurityTile security = (ISecurityTile)tileEntity;
				security.getSecurity().setOwner(getOwner(stack));
				
				if(hasSecurity(stack))
				{
					security.getSecurity().setMode(getSecurity(stack));
				}
				
				if(getOwner(stack) == null)
				{
					security.getSecurity().setOwner(player.getName());
				}
			}
			
			if(tileEntity instanceof IUpgradeTile)
			{
				if(ItemDataUtils.hasData(stack, "upgrades"))
				{
					((IUpgradeTile)tileEntity).getComponent().read(ItemDataUtils.getDataMap(stack));
				}
			}

			if(tileEntity instanceof ISideConfiguration)
			{
				ISideConfiguration config = (ISideConfiguration)tileEntity;

				if(ItemDataUtils.hasData(stack, "sideDataStored"))
				{
					config.getConfig().read(ItemDataUtils.getDataMap(stack));
					config.getEjector().read(ItemDataUtils.getDataMap(stack));
				}
			}
			
			if(tileEntity instanceof ISustainedData)
			{
				if(stack.getTagCompound() != null)
				{
					((ISustainedData)tileEntity).readSustainedData(stack);
				}
			}

			if(tileEntity instanceof IRedstoneControl)
			{
				if(ItemDataUtils.hasData(stack, "controlType"))
				{
					((IRedstoneControl)tileEntity).setControlType(RedstoneControl.values()[ItemDataUtils.getInt(stack, "controlType")]);
				}
			}

			if(tileEntity instanceof ISustainedInventory)
			{
				((ISustainedInventory)tileEntity).setInventory(getInventory(stack));
			}

			if(tileEntity instanceof TileEntityElectricBlock)
			{
				((TileEntityElectricBlock)tileEntity).electricityStored = getEnergy(stack);
			}

			return true;
		}

		return false;
	}

	@Override
	public void setInventory(NBTTagList nbtTags, Object... data)
	{
		if(data[0] instanceof ItemStack)
		{
			ItemDataUtils.setList((ItemStack)data[0], "Items", nbtTags);
		}
	}

	@Override
	public NBTTagList getInventory(Object... data)
	{
		if(data[0] instanceof ItemStack)
		{
			return ItemDataUtils.getList((ItemStack)data[0], "Items");
		}

		return null;
	}

	@Override
	public double getEnergy(ItemStack itemStack)
	{
		return ItemDataUtils.getDouble(itemStack, "energyStored");
	}

	@Override
	public void setEnergy(ItemStack itemStack, double amount)
	{
		ItemDataUtils.setDouble(itemStack, "energyStored", Math.max(Math.min(amount, getMaxEnergy(itemStack)), 0));
	}

	@Override
	public double getMaxEnergy(ItemStack itemStack)
	{
		return MekanismUtils.getMaxEnergy(itemStack, MachineType.DIGITAL_MINER.baseEnergy);
	}

	@Override
	public double getMaxTransfer(ItemStack itemStack)
	{
		return getMaxEnergy(itemStack)*0.005;
	}

	@Override
	public boolean canReceive(ItemStack itemStack)
	{
		return true;
	}

	@Override
	public boolean canSend(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public int receiveEnergy(ItemStack theItem, int energy, boolean simulate)
	{
		if(canReceive(theItem))
		{
			double energyNeeded = getMaxEnergy(theItem)-getEnergy(theItem);
			double toReceive = Math.min(energy*general.FROM_RF, energyNeeded);

			if(!simulate)
			{
				setEnergy(theItem, getEnergy(theItem) + toReceive);
			}

			return (int)Math.round(toReceive*general.TO_RF);
		}

		return 0;
	}

	@Override
	public int extractEnergy(ItemStack theItem, int energy, boolean simulate)
	{
		if(canSend(theItem))
		{
			double energyRemaining = getEnergy(theItem);
			double toSend = Math.min((energy*general.FROM_RF), energyRemaining);

			if(!simulate)
			{
				setEnergy(theItem, getEnergy(theItem) - toSend);
			}

			return (int)Math.round(toSend*general.TO_RF);
		}

		return 0;
	}

	@Override
	public int getEnergyStored(ItemStack theItem)
	{
		return (int)(getEnergy(theItem)*general.TO_RF);
	}

	@Override
	public int getMaxEnergyStored(ItemStack theItem)
	{
		return (int)(getMaxEnergy(theItem)*general.TO_RF);
	}

	@Override
	@Method(modid = "IC2")
	public IElectricItemManager getManager(ItemStack itemStack)
	{
		return IC2ItemManager.getManager(this);
	}

	@Override
	public String getOwner(ItemStack stack) 
	{
		if(ItemDataUtils.hasData(stack, "owner"))
		{
			return ItemDataUtils.getString(stack, "owner");
		}
		
		return null;
	}

	@Override
	public void setOwner(ItemStack stack, String owner) 
	{
		if(owner == null || owner.isEmpty())
		{
			ItemDataUtils.removeData(stack, "owner");
			return;
		}
		
		ItemDataUtils.setString(stack, "owner", owner);
	}

	@Override
	public SecurityMode getSecurity(ItemStack stack) 
	{
		if(!general.allowProtection)
		{
			return SecurityMode.PUBLIC;
		}
		
		return SecurityMode.values()[ItemDataUtils.getInt(stack, "security")];
	}

	@Override
	public void setSecurity(ItemStack stack, SecurityMode mode) 
	{
		ItemDataUtils.setInt(stack, "security", mode.ordinal());
	}

	@Override
	public boolean hasSecurity(ItemStack stack) 
	{
		return true;
	}
	
	@Override
	public boolean hasOwner(ItemStack stack)
	{
		return hasSecurity(stack);
	}
	
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
    {
        return new ItemCapabilityWrapper(stack, new FluidItemWrapper(), new TeslaItemWrapper()) {
        	@Override
        	public boolean hasCapability(Capability<?> capability, EnumFacing facing) 
        	{
        		return super.hasCapability(capability, facing);
        	}
        };
    }
}
