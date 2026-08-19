package xiaoshi2022.anywaydoor.entity.goal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModItems;

import java.util.EnumSet;

public class FollowDorayakiPlayerGoal extends Goal {
    private final DuolabEntity entity;
    private Player targetPlayer;
    private final double speedModifier;
    private final float stopDistance;
    private int cooldown = 0;

    public FollowDorayakiPlayerGoal(DuolabEntity entity) {
        this.entity = entity;
        this.speedModifier = 0.8D;
        this.stopDistance = 2.0F;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.isSitting()) return false;
        if (entity.pocket.isFull()) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        targetPlayer = findNearestPlayerWithDorayaki();
        return targetPlayer != null;
    }

    private Player findNearestPlayerWithDorayaki() {
        double range = 16.0D;
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player player : entity.level().players()) {
            if (player == null || !player.isAlive()) continue;
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceToSqr(entity) > range * range) continue;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean hasDorayaki = !mainHand.isEmpty() && mainHand.getItem() == ModItems.DORAYAKI;
            if (!hasDorayaki) {
                hasDorayaki = !offHand.isEmpty() && offHand.getItem() == ModItems.DORAYAKI;
            }

            if (hasDorayaki) {
                double dist = player.distanceToSqr(entity);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = player;
                }
            }
        }

        return nearest;
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.isSitting()) return false;
        if (entity.pocket.isFull()) return false;
        if (targetPlayer == null || !targetPlayer.isAlive()) return false;

        ItemStack mainHand = targetPlayer.getMainHandItem();
        ItemStack offHand = targetPlayer.getOffhandItem();
        boolean hasDorayaki = (!mainHand.isEmpty() && mainHand.getItem() == ModItems.DORAYAKI) ||
                (!offHand.isEmpty() && offHand.getItem() == ModItems.DORAYAKI);
        if (!hasDorayaki) return false;

        if (entity.distanceToSqr(targetPlayer) > 20 * 20) return false;

        return true;
    }

    @Override
    public void start() {
        cooldown = 20;
    }

    @Override
    public void stop() {
        targetPlayer = null;
        cooldown = 40;
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;

        entity.getLookControl().setLookAt(targetPlayer, 10.0F, 10.0F);

        double dist = entity.distanceToSqr(targetPlayer);
        if (dist > stopDistance * stopDistance) {
            entity.getNavigation().moveTo(targetPlayer, speedModifier);
        } else {
            entity.getNavigation().stop();
        }

        if (dist < 2.5 * 2.5 && entity.tickCount % 20 == 0) {
            targetPlayer.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.want_dorayaki"),
                    true
            );
        }
        // 删除所有传送门检测代码
    }
}