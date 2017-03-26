package mekanism.common.block;

import java.util.Random;
import java.util.logging.Logger;

import buildcraft.api.tools.IToolWrench;
import mekanism.api.MekanismConfig.client;
import mekanism.api.energy.IEnergizedItem;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IFactory;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ISustainedInventory;
import mekanism.common.base.ISustainedTank;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.TileEntityBasicBlock;
import mekanism.common.tile.TileEntityContainerBlock;
import mekanism.common.tile.TileEntityFactory;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDigitalMiner extends Block implements ITileEntityProvider
{
	/*
	 * In order to more easily facilitate support for standard Forge/Minecraft blockstates and model,
	 * the Digital Miner has been separated from the machine blocks.

	 * The machine block version has been depreciated, and is only used as a place-holder for any old
	 * Digital Miners that may still be in a player's inventory.
	 */
	
	public static final PropertyDirection PROPERTYFACING = PropertyDirection.create( "facing", EnumFacing.Plane.HORIZONTAL );
	public static final PropertyBool PROPERTYACTIVE = PropertyBool.create( "active" );
	
	public BlockDigitalMiner()
	{
		super( Material.IRON );
		setHardness(3.5F);
		setResistance(16F);
		setCreativeTab(Mekanism.tabMekanism);
	}
	
	@Override
	public BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, new IProperty[] { PROPERTYFACING, PROPERTYACTIVE } );
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty( PROPERTYFACING, EnumFacing.getHorizontal( meta ));
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue( PROPERTYFACING ).getHorizontalIndex();
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
	{
		TileEntity tile = worldIn.getTileEntity(pos);
		
		if(tile instanceof TileEntityBasicBlock && ((TileEntityBasicBlock)tile).facing != null)
		{
			state = state.withProperty( PROPERTYFACING, ((TileEntityBasicBlock)tile).facing);
		}
		
		if(tile instanceof IActiveState)
		{
			state = state.withProperty( PROPERTYACTIVE, ((IActiveState)tile).getActive());
		}
		
		return state;
	}
	
	@Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
		return this.getDefaultState().withProperty( PROPERTYFACING, placer.getHorizontalFacing().getOpposite() );
    }
	
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		world.setBlockState( pos, state.withProperty( PROPERTYFACING, placer.getHorizontalFacing().getOpposite() ), 2 );
		
		TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);
		int side = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		int height = Math.round(placer.rotationPitch);
		int change = 3;

		if(tileEntity == null)
		{
			return;
		}

		if(tileEntity.canSetFacing(0) && tileEntity.canSetFacing(1))
		{
			if(height >= 65)
			{
				change = 1;
			} else if(height <= -65)
			{
				change = 0;
			}
		}

		if(change != 0 && change != 1)
		{
			switch(side)
			{
				case 0:
					change = 2;
					break;
				case 1:
					change = 5;
					break;
				case 2:
					change = 3;
					break;
				case 3:
					change = 4;
					break;
			}
		}

		tileEntity.setFacing((short)change);
		tileEntity.redstone = world.isBlockIndirectlyGettingPowered(pos) > 0;

		if(tileEntity instanceof IBoundingBlock)
		{
			((IBoundingBlock)tileEntity).onPlace();
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random)
	{
		TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);
		
		if(MekanismUtils.isActive(world, pos) && ((IActiveState)tileEntity).renderUpdate() && client.machineEffects)
		{
			float xRandom = (float)pos.getX() + 0.5F;
			float yRandom = (float)pos.getY() + 0.0F + random.nextFloat() * 6.0F / 16.0F;
			float zRandom = (float)pos.getZ() + 0.5F;
			float iRandom = 0.52F;
			float jRandom = random.nextFloat() * 0.6F - 0.3F;

			EnumFacing side = tileEntity.facing;

			switch(side)
			{
				case WEST:
					world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (xRandom - iRandom), yRandom, (zRandom + jRandom), 0.0D, 0.0D, 0.0D);
					world.spawnParticle(EnumParticleTypes.REDSTONE, (xRandom - iRandom), yRandom, (zRandom + jRandom), 0.0D, 0.0D, 0.0D);
					break;
				case EAST:
					world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (xRandom + iRandom), yRandom, (zRandom + jRandom), 0.0D, 0.0D, 0.0D);
					world.spawnParticle(EnumParticleTypes.REDSTONE, (xRandom + iRandom), yRandom, (zRandom + jRandom), 0.0D, 0.0D, 0.0D);
					break;
				case NORTH:
					world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (xRandom + jRandom), yRandom, (zRandom - iRandom), 0.0D, 0.0D, 0.0D);
					world.spawnParticle(EnumParticleTypes.REDSTONE, (xRandom + jRandom), yRandom, (zRandom - iRandom), 0.0D, 0.0D, 0.0D);
					break;
				case SOUTH:
					world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (xRandom + jRandom), yRandom, (zRandom + iRandom), 0.0D, 0.0D, 0.0D);
					world.spawnParticle(EnumParticleTypes.REDSTONE, (xRandom + jRandom), yRandom, (zRandom + iRandom), 0.0D, 0.0D, 0.0D);
					break;
				default:
					break;
			}
		}
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		if(client.enableAmbientLighting)
		{
			TileEntity tileEntity = world.getTileEntity(pos);

			if(tileEntity instanceof IActiveState)
			{
				if(((IActiveState)tileEntity).getActive() && ((IActiveState)tileEntity).lightUpdate())
				{
					return client.ambientLightingLevel;
				}
			}
		}

		return 0;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, ItemStack stack, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if(world.isRemote)
		{
			return true;
		}

		TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);
		int metadata = state.getBlock().getMetaFromState(state);

		if(stack != null)
		{
			Item tool = stack.getItem();

			if(MekanismUtils.hasUsableWrench(entityplayer, pos))
			{
				if(SecurityUtils.canAccess(entityplayer, tileEntity))
				{
					if(entityplayer.isSneaking())
					{
						dismantleBlock(state, world, pos, false);

						return true;
					}

					if(MekanismUtils.isBCWrench(tool))
					{
						((IToolWrench)tool).wrenchUsed(entityplayer, pos);
					}

					int change = tileEntity.facing.rotateY().ordinal();

					tileEntity.setFacing((short)change);
					world.notifyNeighborsOfStateChange(pos, this);
				} 
				else {
					SecurityUtils.displayNoAccess(entityplayer);
				}

				return true;
			}
		}

		if(tileEntity != null)
		{
			if(!entityplayer.isSneaking())
			{
				if(SecurityUtils.canAccess(entityplayer, tileEntity))
				{
					entityplayer.openGui(Mekanism.instance, MachineType.DIGITAL_MINER.guiId, world, pos.getX(), pos.getY(), pos.getZ());
				} 
				else {
					SecurityUtils.displayNoAccess(entityplayer);
				}

				return true;
			}
		}

		return false;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return MachineType.DIGITAL_MINER.create();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata)
	{
		return null;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune)
	{
		return null;
	}

	@Override
	public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos)
	{
		TileEntity tile = world.getTileEntity(pos);
		
		return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
	}
	
	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion)
    {
		return blockResistance;
    }

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
	{
		if(!player.capabilities.isCreativeMode && !world.isRemote && willHarvest)
		{
			TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);

			float motion = 0.7F;
			double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

			EntityItem entityItem = new EntityItem(world, pos.getX() + motionX, pos.getY() + motionY, pos.getZ() + motionZ, getPickBlock(state, null, world, pos, player));

			world.spawnEntityInWorld(entityItem);
		}

		return world.setBlockToAir(pos);
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock)
	{
		if(!world.isRemote)
		{
			TileEntity tileEntity = world.getTileEntity(pos);

			if(tileEntity instanceof TileEntityBasicBlock)
			{
				((TileEntityBasicBlock)tileEntity).onNeighborChange(neighborBlock);
			}
		}
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);
		ItemStack itemStack = new ItemStack(this);

		if(itemStack.getTagCompound() == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}
		
		if(tileEntity instanceof ISecurityTile)
		{
			ISecurityItem securityItem = (ISecurityItem)itemStack.getItem();
			
			if(securityItem.hasSecurity(itemStack))
			{
				securityItem.setOwner(itemStack, ((ISecurityTile)tileEntity).getSecurity().getOwner());
				securityItem.setSecurity(itemStack, ((ISecurityTile)tileEntity).getSecurity().getMode());
			}
		}

		if(tileEntity instanceof IUpgradeTile)
		{
			((IUpgradeTile)tileEntity).getComponent().write(ItemDataUtils.getDataMap(itemStack));
		}

		if(tileEntity instanceof ISideConfiguration)
		{
			ISideConfiguration config = (ISideConfiguration)tileEntity;

			config.getConfig().write(ItemDataUtils.getDataMap(itemStack));
			config.getEjector().write(ItemDataUtils.getDataMap(itemStack));
		}
		
		if(tileEntity instanceof ISustainedData)
		{
			((ISustainedData)tileEntity).writeSustainedData(itemStack);
		}

		if(tileEntity instanceof IRedstoneControl)
		{
			IRedstoneControl control = (IRedstoneControl)tileEntity;
			ItemDataUtils.setInt(itemStack, "controlType", control.getControlType().ordinal());
		}

		if(tileEntity instanceof IStrictEnergyStorage)
		{
			IEnergizedItem energizedItem = (IEnergizedItem)itemStack.getItem();
			energizedItem.setEnergy(itemStack, ((IStrictEnergyStorage)tileEntity).getEnergy());
		}

		if(tileEntity instanceof TileEntityContainerBlock && ((TileEntityContainerBlock)tileEntity).inventory.length > 0)
		{
			ISustainedInventory inventory = (ISustainedInventory)itemStack.getItem();
			inventory.setInventory(((ISustainedInventory)tileEntity).getInventory(), itemStack);
		}

		if(tileEntity instanceof TileEntityFactory)
		{
			IFactory factoryItem = (IFactory)itemStack.getItem();
			factoryItem.setRecipeType(((TileEntityFactory)tileEntity).recipeType.ordinal(), itemStack);
		}

		return itemStack;
	}

	public ItemStack dismantleBlock(IBlockState state, World world, BlockPos pos, boolean returnBlock)
	{
		ItemStack itemStack = getPickBlock(state, null, world, pos, null);

		world.setBlockToAir(pos);

		if(!returnBlock)
		{
			float motion = 0.7F;
			double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

			EntityItem entityItem = new EntityItem(world, pos.getX() + motionX, pos.getY() + motionY, pos.getZ() + motionZ, itemStack);

			world.spawnEntityInWorld(entityItem);
		}

		return itemStack;
	}

	@Override
	public boolean isFullCube(IBlockState state)
    {
        return false;
    }
	
	@Override
	public EnumFacing[] getValidRotations(World world, BlockPos pos)
	{
		TileEntity tile = world.getTileEntity(pos);
		EnumFacing[] valid = new EnumFacing[6];
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			for(EnumFacing dir : EnumFacing.VALUES)
			{
				if(basicTile.canSetFacing(dir.ordinal()))
				{
					valid[dir.ordinal()] = dir;
				}
			}
		}
		
		return valid;
	}

	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
	{
		TileEntity tile = world.getTileEntity(pos);
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			if(basicTile.canSetFacing(axis.ordinal()))
			{
				basicTile.setFacing((short)axis.ordinal());
				return true;
			}
		}
		
		return false;
	}
}
