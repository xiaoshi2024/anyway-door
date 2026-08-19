package xiaoshi2022.anywaydoor.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModEntities;

public class DuolabSpawnEggItem {

    // 直接创建 SpawnEggItem 实例，不继承
    public static final Item DUOLAB_SPAWN_EGG = new SpawnEggItem(
            ModEntities.DUOLAB,
            0x2196F3,  // 蓝色
            0xFFEB3B,  // 黄色
            new Item.Properties()
    ) {
        // 如果需要自定义行为，在这里重写方法（不需要 @Override）
        // 但默认的 SpawnEggItem 已经足够了
    };
}