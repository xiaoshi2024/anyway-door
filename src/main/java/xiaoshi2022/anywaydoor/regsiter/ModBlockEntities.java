package xiaoshi2022.anywaydoor.regsiter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

public final class ModBlockEntities {

	public static final BlockEntityType<DimensionalDoorBlockEntity> DIMENSIONAL_DOOR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "dimensional_door"),
		BlockEntityType.Builder.of(DimensionalDoorBlockEntity::new, ModBlocks.DIMENSIONAL_DOOR).build(null)
	);

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
