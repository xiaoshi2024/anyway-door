package xiaoshi2022.anywaydoor.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;

public class JumpHelperGoal extends Goal {
    private final DuolabEntity entity;
    private int delay = 0;

    public JumpHelperGoal(DuolabEntity entity) { this.entity = entity; }

    @Override
    public boolean canUse() {
        if (entity.isSitting()) return false;
        if (!entity.onGround()) return false;
        if (entity.jumpCooldown > 0) return false;
        if (delay > 0) { delay--; return false; }
        return isBlockedAhead();
    }

    private boolean isBlockedAhead() {
        Vec3 look = entity.getLookAngle();
        Vec3 pos = entity.position();
        for (double y = 0.5; y <= 1.5; y += 0.5) {
            BlockPos check = BlockPos.containing(
                    pos.x + look.x * 0.8, pos.y + y, pos.z + look.z * 0.8);
            BlockState state = entity.level().getBlockState(check);
            if (!state.isAir() && !state.canBeReplaced()) {
                Block block = state.getBlock();
                if (block instanceof LeavesBlock || block instanceof CarpetBlock ||
                        block instanceof TallGrassBlock || block instanceof DoublePlantBlock ||
                        block instanceof BushBlock || block instanceof VineBlock) continue;
                if (y >= 1.0) return true;
                if (y == 0.5) {
                    if (block instanceof SlabBlock || block instanceof StairBlock) return false;
                    if (!entity.level().getBlockState(check.below()).isAir()) return true;
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (!entity.onGround()) return;
        Vec3 vel = entity.getDeltaMovement();
        double power = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
        Vec3 look = entity.getLookAngle();
        double mx = Math.abs(vel.x) < 0.1 ? look.x * 0.6 : vel.x;
        double mz = Math.abs(vel.z) < 0.1 ? look.z * 0.6 : vel.z;
        double boost = entity.getNavigation().isInProgress() ? 1.3 : 1.0;
        entity.setDeltaMovement(mx * boost + look.x * 0.2, power * 1.2, mz * boost + look.z * 0.2);
        entity.playJump();
        entity.setJumping(true);
        entity.jumpCooldown = 12;
        delay = 3;
    }

    @Override public boolean canContinueToUse() { return false; }
}