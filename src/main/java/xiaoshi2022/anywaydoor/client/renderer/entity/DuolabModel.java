package xiaoshi2022.anywaydoor.client.renderer.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import xiaoshi2022.anywaydoor.AnywayDoor;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;

public class DuolabModel extends GeoModel<DuolabEntity> {

    @Override
    public ResourceLocation getModelResource(DuolabEntity animatable) {
        return AnywayDoor.id("geo/entity/duolab.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DuolabEntity animatable) {
        return AnywayDoor.id("textures/entity/duolab.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DuolabEntity animatable) {
        return AnywayDoor.id("animations/entity/duolab.animation.json");
    }
}