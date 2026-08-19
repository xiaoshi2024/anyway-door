package xiaoshi2022.anywaydoor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import xiaoshi2022.anywaydoor.client.renderer.entity.DuolabRenderer;
import xiaoshi2022.anywaydoor.regsiter.ModBlockEntities;
import xiaoshi2022.anywaydoor.client.renderer.block.DimensionalDoorRenderer;
import xiaoshi2022.anywaydoor.regsiter.ModEntities;

public class AnywayDoorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerEntityRenderers();
        // 注册任意门方块实体的 GeckoLib 渲染器(缺了进存档会崩)
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_DOOR, DimensionalDoorRenderer::new);
    }

    private void registerEntityRenderers() {

        EntityRendererRegistry.register(ModEntities.DUOLAB,
                DuolabRenderer::new);
    }

}