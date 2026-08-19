package xiaoshi2022.anywaydoor.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModItems;

public class FindDorayakiGoal extends Goal {
    private final DuolabEntity entity;
    private ItemStack target = ItemStack.EMPTY;
    private int cooldown = 0;
    private int eatTimer = 0;
    private int tickCounter = 0;

    public FindDorayakiGoal(DuolabEntity entity) { this.entity = entity; }

    @Override
    public boolean canUse() {
        if (entity.isSitting()) return false;
        if (entity.pocket.isFull()) return false;
        if (cooldown > 0) { cooldown--; return false; }
        return findNearest();
    }

    private boolean findNearest() {
        AABB box = entity.getBoundingBox().inflate(8.0D);
        var items = entity.level().getEntitiesOfClass(ItemEntity.class, box,
                i -> i != null && !i.getItem().isEmpty() && i.getItem().getItem() == ModItems.DORAYAKI);
        if (items.isEmpty()) return false;
        ItemEntity nearest = null;
        double dist = Double.MAX_VALUE;
        Vec3 pos = entity.position();
        for (var i : items) {
            double d = i.distanceToSqr(pos);
            if (d < dist) { dist = d; nearest = i; }
        }
        if (nearest != null) {
            target = nearest.getItem().copy();
            entity.getNavigation().moveTo(nearest.getX(), nearest.getY(), nearest.getZ(), 0.8D);
            return true;
        }
        return false;
    }

    @Override
    public void start() { eatTimer = 0; cooldown = 20; tickCounter = 0; }

    @Override
    public void tick() {
        tickCounter++;
        if (tickCounter % 5 != 0) return;
        if (target.isEmpty()) return;

        AABB box = entity.getBoundingBox().inflate(1.5D);
        var items = entity.level().getEntitiesOfClass(ItemEntity.class, box,
                i -> i != null && !i.getItem().isEmpty() && i.getItem().getItem() == ModItems.DORAYAKI);
        if (!items.isEmpty()) {
            eatTimer++;
            if (eatTimer >= 20) {
                ItemEntity dorayaki = items.get(0);
                ItemStack stack = dorayaki.getItem();
                if (!stack.isEmpty() && entity.pocket.addItem(stack)) {
                    dorayaki.discard();
                    entity.setMainHandItem(stack);
                    entity.playOpenBag();
                }
                eatTimer = 0;
                target = ItemStack.EMPTY;
                cooldown = 40;
            }
        } else {
            target = ItemStack.EMPTY;
            cooldown = 10;
        }
    }

    @Override public boolean canContinueToUse() { return !target.isEmpty() && !entity.isSitting(); }
    @Override public void stop() { target = ItemStack.EMPTY; eatTimer = 0; }
}