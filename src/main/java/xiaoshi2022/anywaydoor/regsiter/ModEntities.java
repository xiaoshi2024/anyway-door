package xiaoshi2022.anywaydoor.regsiter;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import xiaoshi2022.anywaydoor.AnywayDoor;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;

public class ModEntities {

    public static final EntityType<DuolabEntity> DUOLAB = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AnywayDoor.MOD_ID, "duolab"),
            FabricEntityTypeBuilder.<DuolabEntity>createLiving()
                    .entityFactory(DuolabEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    // 移除 trackRangeChunks，使用默认 32 区块
                    .defaultAttributes(DuolabEntity::createAttributes)
                    .build()
    );

    public static void init() {
        // 用于加载类
    }
}