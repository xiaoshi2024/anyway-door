package xiaoshi2022.anywaydoor.regsiter;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.anywaydoor.AnywayDoor;

public class ModItemGroups {

    // 使用 ResourceKey 方式（更安全，推荐）
    public static final ResourceKey<CreativeModeTab> ANYWAY_DOOR_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(AnywayDoor.MOD_ID, "main")
    );

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ANYWAY_DOOR_TAB,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.anyway-door.main"))
                        .icon(() -> new ItemStack(ModBlocks.DIMENSIONAL_DOOR))
                        .displayItems((context, entries) -> {
                            // 添加所有物品
                            entries.accept(ModItems.DIMENSIONAL_DOOR_ITEM);
                            entries.accept(ModItems.DUOLAB_SPAWN_EGG);
                            entries.accept(ModItems.DORAYAKI);
                            entries.accept(ModItems.XIAOFU_MASK);
                        })
                        .build()
        );
    }
}