package xiaoshi2022.anywaydoor.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;

/**
 * 任意门的 GeckoLib 方块实体渲染器,负责播放 door_open / door_close 动画并渲染模型。
 */
public class DimensionalDoorRenderer extends GeoBlockRenderer<DimensionalDoorBlockEntity> {

	public DimensionalDoorRenderer(BlockEntityRendererProvider.Context context) {
		super(new DimensionalDoorModel());
	}
}
