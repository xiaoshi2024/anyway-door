package xiaoshi2022.anywaydoor.regsiter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import xiaoshi2022.anywaydoor.AnywayDoor;
import xiaoshi2022.anywaydoor.item.DuolabSpawnEggItem;

public class ModItems {

    // ========== 刷怪蛋 ==========
    public static final Item DUOLAB_SPAWN_EGG = register(
            "duolab_spawn_egg",
            DuolabSpawnEggItem.DUOLAB_SPAWN_EGG
    );

    // ========== 铜锣烧 ==========
    public static final Item DORAYAKI = register(
            "dorayaki",
            new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(0.8f)
                            .alwaysEdible()
                            .build()
                    )
                    .stacksTo(16)
            )
    );

    // ========== 任意门物品（BlockItem） ==========
    public static final Item DIMENSIONAL_DOOR_ITEM = register(
            "dimensional_door",
            new BlockItem(ModBlocks.DIMENSIONAL_DOOR, new Item.Properties().stacksTo(1))
    );

    public static void init() {
        // 用于加载类
    }

    private static Item register(String path, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AnywayDoor.MOD_ID, path),
                item
        );
    }
}