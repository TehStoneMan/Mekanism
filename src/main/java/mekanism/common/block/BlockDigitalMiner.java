package mekanism.common.block;

import mekanism.common.Mekanism;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockDigitalMiner extends Block
{
	public BlockDigitalMiner()
	{
		super(Material.IRON);
		setHardness(3.5F);
		setResistance(16F);
		setCreativeTab(Mekanism.tabMekanism);
	}
}
