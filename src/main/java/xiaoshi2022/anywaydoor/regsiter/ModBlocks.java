package xiaoshi2022.anywaydoor.regsiter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import xiaoshi2022.anywaydoor.block.DimensionalDoorBlock;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

public final class ModBlocks {

	public static final Block DIMENSIONAL_DOOR = register(
		"dimensional_door",
		new DimensionalDoorBlock(
			BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_LIGHT_BLUE)
				.strength(5.0F, 1200.0F)
				.lightLevel(state -> 8)
				.sound(SoundType.METAL)
				.requiresCorrectToolForDrops()
				.noOcclusion()
		)
	);

	private ModBlocks() {
	}

	private static Block register(String path, Block block) {
		Block registered = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, path), block);
		Item item = new BlockItem(registered, new Item.Properties());
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, path), item);
		return registered;
	}

	public static void init() {
	}
}
