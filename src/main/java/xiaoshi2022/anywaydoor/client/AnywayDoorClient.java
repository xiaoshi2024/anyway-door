package xiaoshi2022.anywaydoor.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import xiaoshi2022.anywaydoor.regsiter.ModBlockEntities;
import xiaoshi2022.anywaydoor.client.renderer.DimensionalDoorRenderer;

public class AnywayDoorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册任意门方块实体的 GeckoLib 渲染器(缺了进存档会崩)
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_DOOR, DimensionalDoorRenderer::new);
    }
}