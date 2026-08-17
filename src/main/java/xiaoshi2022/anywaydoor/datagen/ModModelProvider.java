package xiaoshi2022.anywaydoor.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;

public class ModModelProvider extends FabricModelProvider {

	public ModModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators gen) {
		// 方块在世界里由 GeckoLib BlockEntity 渲染,这里仍产出一个简单模型供 datagen 校验 +
		// 供物品栏方块图标使用
		gen.createTrivialCube(ModBlocks.DIMENSIONAL_DOOR);
	}

	@Override
	public void generateItemModels(ItemModelGenerators gen) {
		// 方块物品模型由 ModelProvider 自动补(DelegatedModel -> block 模型)
	}
}
