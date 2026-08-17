package xiaoshi2022.anywaydoor.client.renderer;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import xiaoshi2022.anywaydoor.AnywayDoor;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

/**
 * 任意门的 GeckoLib 模型:geo 模型、动画、纹理三个资源位置。
 */
public class DimensionalDoorModel extends GeoModel<DimensionalDoorBlockEntity> {

	@Override
	public ResourceLocation getModelResource(DimensionalDoorBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID,"geo/dimensional_door.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DimensionalDoorBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID,"textures/block/dimensional_door.png");
	}

	@Override
	public ResourceLocation getAnimationResource(DimensionalDoorBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID,"animations/dimensional_door.animation.json");
	}
}
