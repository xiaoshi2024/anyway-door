package xiaoshi2022.anywaydoor.client.renderer.armor;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import xiaoshi2022.anywaydoor.item.XiaofuMaskItem;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

public class XiaofuMaskModel extends GeoModel<XiaofuMaskItem> {

    @Override
    public ResourceLocation getModelResource(XiaofuMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "geo/armor/xiaofu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(XiaofuMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/armor/xiaofu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(XiaofuMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "animations/armor/xiaofu.animation.json");
    }
}