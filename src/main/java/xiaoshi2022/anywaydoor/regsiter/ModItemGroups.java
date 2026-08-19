package xiaoshi2022.anywaydoor.regsiter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.anywaydoor.AnywayDoor;

public class ModItemGroups {

    public static final CreativeModeTab ANYWAY_DOOR_TAB = CreativeModeTab.builder(
                    CreativeModeTab.Row.TOP,   // 第一行（TOP）或第二行（BOTTOM）
                    CreativeModeTab.Type.CATEGORY.ordinal()  // CATEGORY 或 INVENTORY
            )
            .title(Component.translatable("itemGroup.anyway-door"))
            .icon(() -> new ItemStack(ModBlocks.DIMENSIONAL_DOOR))
            .displayItems((parameters, output) -> {
                // ========== 添加所有物品到自定义物品栏 ==========
                // 任意门方块
                output.accept(ModBlocks.DIMENSIONAL_DOOR);
                // 哆啦B梦刷怪蛋
                output.accept(ModItems.DUOLAB_SPAWN_EGG);

                output.accept(ModItems.DORAYAKI);
            })
            .build();

    public static void init() {
        // 注册到 BuiltInRegistries
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(AnywayDoor.MOD_ID, "anyway_door_tab"),
                ANYWAY_DOOR_TAB
        );
    }
}