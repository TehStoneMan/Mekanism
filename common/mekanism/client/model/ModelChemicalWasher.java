package mekanism.client.model;

import mekanism.client.render.MekanismRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelChemicalWasher extends ModelBase
{
	ModelRenderer Base;
	ModelRenderer Base2;
	ModelRenderer IO1;
	ModelRenderer IO2;
	ModelRenderer IO3;
	ModelRenderer Base3;
	ModelRenderer Base4;
	ModelRenderer Base5;
	ModelRenderer Base6;
	ModelRenderer Can1B;
	ModelRenderer Can1T;
	ModelRenderer Can1Side3;
	ModelRenderer Can1Side1;
	ModelRenderer Can1Side2;
	ModelRenderer Can1Side4;
	ModelRenderer Can2Side4;
	ModelRenderer Can3Side3;
	ModelRenderer Can2Side2;
	ModelRenderer Can2Side3;
	ModelRenderer Can2Side1;
	ModelRenderer Can4Side3;
	ModelRenderer Can4Side1;
	ModelRenderer Can4Sjde4;
	ModelRenderer Can4Side2;
	ModelRenderer Can3Side4;
	ModelRenderer Can4B;
	ModelRenderer Can3Side2;
	ModelRenderer Can3Side1;
	ModelRenderer Can2B;
	ModelRenderer Can4T;
	ModelRenderer Can2T;
	ModelRenderer Can3T;
	ModelRenderer Can3B;
	
	public ModelChemicalWasher()
	{
		textureWidth = 128;
		textureHeight = 64;
		
		Base = new ModelRenderer(this, 16, 0);
		Base.addBox(0F, 0F, 0F, 14, 1, 14);
		Base.setRotationPoint(-7F, 23F, -7F);
		Base.setTextureSize(128, 64);
		Base.mirror = true;
		setRotation(Base, 0F, 0F, 0F);
		Base2 = new ModelRenderer(this, 72, 0);
		Base2.addBox(0F, 0F, 0F, 14, 10, 1);
		Base2.setRotationPoint(-7F, 8F, 6F);
		Base2.setTextureSize(128, 64);
		Base2.mirror = true;
		setRotation(Base2, 0F, 0F, 0F);
		Base2.mirror = false;
		IO1 = new ModelRenderer(this, 9, 17);
		IO1.addBox(0F, 0F, 0F, 1, 8, 8);
		IO1.setRotationPoint(-8F, 12F, -4F);
		IO1.setTextureSize(128, 64);
		IO1.mirror = true;
		setRotation(IO1, 0F, 0F, 0F);
		IO2 = new ModelRenderer(this, 9, 17);
		IO2.addBox(0F, 0F, 0F, 1, 8, 8);
		IO2.setRotationPoint(7F, 12F, -4F);
		IO2.setTextureSize(128, 64);
		IO2.mirror = true;
		setRotation(IO2, 0F, 0F, 0F);
		IO3 = new ModelRenderer(this, 0, 42);
		IO3.addBox(0F, 0F, 0F, 12, 1, 8);
		IO3.setRotationPoint(-6F, 8F, -4F);
		IO3.setTextureSize(128, 64);
		IO3.mirror = true;
		setRotation(IO3, 0F, 0F, 0F);
		Base3 = new ModelRenderer(this, 72, 0);
		Base3.addBox(0F, 0F, 0F, 14, 10, 1);
		Base3.setRotationPoint(-7F, 8F, -7F);
		Base3.setTextureSize(128, 64);
		Base3.mirror = true;
		setRotation(Base3, 0F, 0F, 0F);
		Base4 = new ModelRenderer(this, 72, 12);
		Base4.addBox(0F, 0F, 0F, 1, 10, 12);
		Base4.setRotationPoint(6F, 8F, -6F);
		Base4.setTextureSize(128, 64);
		Base4.mirror = true;
		setRotation(Base4, 0F, 0F, 0F);
		Base5 = new ModelRenderer(this, 72, 12);
		Base5.addBox(0F, 0F, 0F, 1, 10, 12);
		Base5.setRotationPoint(-7F, 8F, -6F);
		Base5.setTextureSize(128, 64);
		Base5.mirror = true;
		setRotation(Base5, 0F, 0F, 0F);
		Base6 = new ModelRenderer(this, 16, 0);
		Base6.addBox(0F, 0F, 0F, 14, 1, 14);
		Base6.setRotationPoint(-7F, 18F, -7F);
		Base6.setTextureSize(128, 64);
		Base6.mirror = true;
		setRotation(Base6, 0F, 0F, 0F);
		Can1B = new ModelRenderer(this, 0, 0);
		Can1B.addBox(0F, 0F, 0F, 4, 1, 4);
		Can1B.setRotationPoint(2F, 22F, 2F);
		Can1B.setTextureSize(128, 64);
		Can1B.mirror = true;
		setRotation(Can1B, 0F, 0F, 0F);
		Can1T = new ModelRenderer(this, 0, 0);
		Can1T.addBox(0F, 0F, 0F, 4, 1, 4);
		Can1T.setRotationPoint(2F, 19F, 2F);
		Can1T.setTextureSize(128, 64);
		Can1T.mirror = true;
		setRotation(Can1T, 0F, 0F, 0F);
		Can1Side3 = new ModelRenderer(this, 0, 0);
		Can1Side3.addBox(0F, 0F, 0F, 1, 2, 2);
		Can1Side3.setRotationPoint(5F, 20F, 3F);
		Can1Side3.setTextureSize(128, 64);
		Can1Side3.mirror = true;
		setRotation(Can1Side3, 0F, 0F, 0F);
		Can1Side1 = new ModelRenderer(this, 0, 0);
		Can1Side1.addBox(0F, 0F, 0F, 4, 2, 1);
		Can1Side1.setRotationPoint(2F, 20F, 5F);
		Can1Side1.setTextureSize(128, 64);
		Can1Side1.mirror = true;
		setRotation(Can1Side1, 0F, 0F, 0F);
		Can1Side2 = new ModelRenderer(this, 0, 0);
		Can1Side2.addBox(0F, 0F, 0F, 4, 2, 1);
		Can1Side2.setRotationPoint(2F, 20F, 2F);
		Can1Side2.setTextureSize(128, 64);
		Can1Side2.mirror = true;
		setRotation(Can1Side2, 0F, 0F, 0F);
		Can1Side4 = new ModelRenderer(this, 0, 0);
		Can1Side4.addBox(0F, 0F, 0F, 1, 2, 2);
		Can1Side4.setRotationPoint(2F, 20F, 3F);
		Can1Side4.setTextureSize(128, 64);
		Can1Side4.mirror = true;
		setRotation(Can1Side4, 0F, 0F, 0F);
		Can2Side4 = new ModelRenderer(this, 0, 0);
		Can2Side4.addBox(0F, 0F, 0F, 1, 2, 2);
		Can2Side4.setRotationPoint(-6F, 20F, 3F);
		Can2Side4.setTextureSize(128, 64);
		Can2Side4.mirror = true;
		setRotation(Can2Side4, 0F, 0F, 0F);
		Can3Side3 = new ModelRenderer(this, 0, 0);
		Can3Side3.addBox(0F, 0F, 0F, 1, 2, 2);
		Can3Side3.setRotationPoint(5F, 20F, -5F);
		Can3Side3.setTextureSize(128, 64);
		Can3Side3.mirror = true;
		setRotation(Can3Side3, 0F, 0F, 0F);
		Can2Side2 = new ModelRenderer(this, 0, 0);
		Can2Side2.addBox(0F, 0F, 0F, 4, 2, 1);
		Can2Side2.setRotationPoint(-6F, 20F, 2F);
		Can2Side2.setTextureSize(128, 64);
		Can2Side2.mirror = true;
		setRotation(Can2Side2, 0F, 0F, 0F);
		Can2Side3 = new ModelRenderer(this, 0, 0);
		Can2Side3.addBox(0F, 0F, 0F, 1, 2, 2);
		Can2Side3.setRotationPoint(-3F, 20F, 3F);
		Can2Side3.setTextureSize(128, 64);
		Can2Side3.mirror = true;
		setRotation(Can2Side3, 0F, 0F, 0F);
		Can2Side1 = new ModelRenderer(this, 0, 0);
		Can2Side1.addBox(0F, 0F, 0F, 4, 2, 1);
		Can2Side1.setRotationPoint(-6F, 20F, 5F);
		Can2Side1.setTextureSize(128, 64);
		Can2Side1.mirror = true;
		setRotation(Can2Side1, 0F, 0F, 0F);
		Can4Side3 = new ModelRenderer(this, 0, 0);
		Can4Side3.addBox(0F, 0F, 0F, 1, 2, 2);
		Can4Side3.setRotationPoint(-3F, 20F, -5F);
		Can4Side3.setTextureSize(128, 64);
		Can4Side3.mirror = true;
		setRotation(Can4Side3, 0F, 0F, 0F);
		Can4Side1 = new ModelRenderer(this, 0, 0);
		Can4Side1.addBox(0F, 0F, 0F, 4, 2, 1);
		Can4Side1.setRotationPoint(-6F, 20F, -3F);
		Can4Side1.setTextureSize(128, 64);
		Can4Side1.mirror = true;
		setRotation(Can4Side1, 0F, 0F, 0F);
		Can4Sjde4 = new ModelRenderer(this, 0, 0);
		Can4Sjde4.addBox(0F, 0F, 0F, 1, 2, 2);
		Can4Sjde4.setRotationPoint(-6F, 20F, -5F);
		Can4Sjde4.setTextureSize(128, 64);
		Can4Sjde4.mirror = true;
		setRotation(Can4Sjde4, 0F, 0F, 0F);
		Can4Side2 = new ModelRenderer(this, 0, 0);
		Can4Side2.addBox(0F, 0F, 0F, 4, 2, 1);
		Can4Side2.setRotationPoint(-6F, 20F, -6F);
		Can4Side2.setTextureSize(128, 64);
		Can4Side2.mirror = true;
		setRotation(Can4Side2, 0F, 0F, 0F);
		Can3Side4 = new ModelRenderer(this, 0, 0);
		Can3Side4.addBox(0F, 0F, 0F, 1, 2, 2);
		Can3Side4.setRotationPoint(2F, 20F, -5F);
		Can3Side4.setTextureSize(128, 64);
		Can3Side4.mirror = true;
		setRotation(Can3Side4, 0F, 0F, 0F);
		Can4B = new ModelRenderer(this, 0, 0);
		Can4B.addBox(0F, 0F, 0F, 4, 1, 4);
		Can4B.setRotationPoint(-6F, 22F, -6F);
		Can4B.setTextureSize(128, 64);
		Can4B.mirror = true;
		setRotation(Can4B, 0F, 0F, 0F);
		Can3Side2 = new ModelRenderer(this, 0, 0);
		Can3Side2.addBox(0F, 0F, 0F, 4, 2, 1);
		Can3Side2.setRotationPoint(2F, 20F, -6F);
		Can3Side2.setTextureSize(128, 64);
		Can3Side2.mirror = true;
		setRotation(Can3Side2, 0F, 0F, 0F);
		Can3Side1 = new ModelRenderer(this, 0, 0);
		Can3Side1.addBox(0F, 0F, 0F, 4, 2, 1);
		Can3Side1.setRotationPoint(2F, 20F, -3F);
		Can3Side1.setTextureSize(128, 64);
		Can3Side1.mirror = true;
		setRotation(Can3Side1, 0F, 0F, 0F);
		Can2B = new ModelRenderer(this, 0, 0);
		Can2B.addBox(0F, 0F, 0F, 4, 1, 4);
		Can2B.setRotationPoint(-6F, 22F, 2F);
		Can2B.setTextureSize(128, 64);
		Can2B.mirror = true;
		setRotation(Can2B, 0F, 0F, 0F);
		Can4T = new ModelRenderer(this, 0, 0);
		Can4T.addBox(0F, 0F, 0F, 4, 1, 4);
		Can4T.setRotationPoint(-6F, 19F, -6F);
		Can4T.setTextureSize(128, 64);
		Can4T.mirror = true;
		setRotation(Can4T, 0F, 0F, 0F);
		Can2T = new ModelRenderer(this, 0, 0);
		Can2T.addBox(0F, 0F, 0F, 4, 1, 4);
		Can2T.setRotationPoint(-6F, 19F, 2F);
		Can2T.setTextureSize(128, 64);
		Can2T.mirror = true;
		setRotation(Can2T, 0F, 0F, 0F);
		Can3T = new ModelRenderer(this, 0, 0);
		Can3T.addBox(0F, 0F, 0F, 4, 1, 4);
		Can3T.setRotationPoint(2F, 19F, -6F);
		Can3T.setTextureSize(128, 64);
		Can3T.mirror = true;
		setRotation(Can3T, 0F, 0F, 0F);
		Can3B = new ModelRenderer(this, 0, 0);
		Can3B.addBox(0F, 0F, 0F, 4, 1, 4);
		Can3B.setRotationPoint(2F, 22F, -6F);
		Can3B.setTextureSize(128, 64);
		Can3B.mirror = true;
		setRotation(Can3B, 0F, 0F, 0F);
	}
	
	public void render(float size)
	{
		MekanismRenderer.blendOn();
		
		Base.render(size);
		Base2.render(size);
		IO1.render(size);
		IO2.render(size);
		IO3.render(size);
		Base3.render(size);
		Base4.render(size);
		Base5.render(size);
		Base6.render(size);
		Can1B.render(size);
		Can1T.render(size);
		Can1Side3.render(size);
		Can1Side1.render(size);
		Can1Side2.render(size);
		Can1Side4.render(size);
		Can2Side4.render(size);
		Can3Side3.render(size);
		Can2Side2.render(size);
		Can2Side3.render(size);
		Can2Side1.render(size);
		Can4Side3.render(size);
		Can4Side1.render(size);
		Can4Sjde4.render(size);
		Can4Side2.render(size);
		Can3Side4.render(size);
		Can4B.render(size);
		Can3Side2.render(size);
		Can3Side1.render(size);
		Can2B.render(size);
		Can4T.render(size);
		Can2T.render(size);
		Can3T.render(size);
		Can3B.render(size);
		
		MekanismRenderer.blendOff();
	}
	
	private void setRotation(ModelRenderer model, float x, float y, float z)
	{
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
