package mekanism.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.MekanismConfig.general;
import mekanism.api.MekanismConfig.miner;
import mekanism.api.util.GeometryUtils;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.tile.TileEntityDigitalMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;

public final class MinerVisualRenderer 
{
	private static Minecraft mc = Minecraft.getMinecraft();
	
	private static Map<MinerRenderData, DisplayInteger> cachedVisuals = new HashMap<MinerRenderData, DisplayInteger>();
	
	private static final double offset = 0.01;
	
	public static void render(TileEntityDigitalMiner miner)
	{
		GlStateManager.pushMatrix();
		GlStateManager.translate( getX(miner.getPos().getX()), getY(miner.getPos().getY()), getZ(miner.getPos().getZ()) );

		MekanismRenderer.blendOn();
		MekanismRenderer.glowOn();

		GlStateManager.enableCull();
		GlStateManager.color( 1.0F, 1.0F, 1.0F, 0.8F );

		mc.getTextureManager().bindTexture(MekanismRenderer.getBlocksTexture());
		getList(new MinerRenderData(miner)).render();

		MekanismRenderer.resetColor();
		MekanismRenderer.glowOff();
		MekanismRenderer.blendOff();

		GlStateManager.popMatrix();
	}
	
	private static DisplayInteger getList(MinerRenderData data)
	{
		if(cachedVisuals.containsKey(data))
		{
			return cachedVisuals.get(data);
		}
		
		DisplayInteger display = DisplayInteger.createAndStart();
		cachedVisuals.put(data, display);
		
		List<Model3D> models = new ArrayList<Model3D>();
		
		if( !miner.doOldOperation )
		{
			// Spherical style operation
			for(int y = data.radius; y >= -data.yCoord; y--)
			{
				for(int z = -data.radius; z <= data.radius; z++)
				{
					for(int x = -data.radius; x <= data.radius; x++)
					{
						// Calculate if point is inside area of operation
						if( GeometryUtils.isInsideSphere( x, y, z, data.radius ) && GeometryUtils.isOnSurface( x, y, z, data.radius )  )
							models.add( createModel( new Coord4D( x, y, z, mc.theWorld.provider.getDimension() ) ) );
					}
				}
			}
		}
		else
		{
			// Cubic style operation:
			for(int x = -data.radius; x <= data.radius; x++)
			{
				for(int y = data.minY-data.yCoord; y <= data.maxY-data.yCoord; y++)
				{
					for(int z = -data.radius; z <= data.radius; z++)
					{
						models.add(createModel(new Coord4D(x, y, z, mc.theWorld.provider.getDimension())));
						/*if(x == -data.radius || x == data.radius || y == data.minY-data.yCoord || y == data.maxY-data.yCoord || z == -data.radius || z == data.radius)
						{
							models.add(createModel(new Coord4D(x, y, z, mc.theWorld.provider.dimensionId)));
						}*/
					}
				}
			}
		}
		
		for(Model3D model : models)
		{
			MekanismRenderer.renderObject(model);
		}
		
		display.endList();
		
		return display;
	}
	
	private static Model3D createModel(Coord4D rel)
	{
		Model3D toReturn = new Model3D();
		
		toReturn.setBlockBounds(rel.xCoord + 0.4, rel.yCoord + 0.4, rel.zCoord + 0.4, rel.xCoord + 0.6, rel.yCoord + 0.6, rel.zCoord + 0.6);
		toReturn.baseBlock = Blocks.WATER;
		toReturn.setTexture(MekanismRenderer.getColorIcon(EnumColor.WHITE));
		
		return toReturn;
	}
	
	private static double getX(int x)
	{
		return x - TileEntityRendererDispatcher.staticPlayerX;
	}

	private static double getY(int y)
	{
		return y - TileEntityRendererDispatcher.staticPlayerY;
	}

	private static double getZ(int z)
	{
		return z - TileEntityRendererDispatcher.staticPlayerZ;
	}
	
	public static class MinerRenderData
	{
		public int minY;
		public int maxY;
		public int radius;
		public int yCoord;
		
		public MinerRenderData(int min, int max, int rad, int y)
		{
			minY = min;
			maxY = max;
			radius = rad;
			yCoord = y;
		}
		
		public MinerRenderData(TileEntityDigitalMiner miner)
		{
			this(miner.minY, miner.maxY, miner.radius, miner.getPos().getY());
		}

		@Override
		public boolean equals(Object data)
		{
			return data instanceof MinerRenderData && ((MinerRenderData)data).minY == minY && 
					((MinerRenderData)data).maxY == maxY && ((MinerRenderData)data).radius == radius && 
					((MinerRenderData)data).yCoord == yCoord;
		}

		@Override
		public int hashCode()
		{
			int code = 1;
			code = 31 * code + minY;
			code = 31 * code + maxY;
			code = 31 * code + radius;
			code = 31 * code + yCoord;
			return code;
		}
	}
}
