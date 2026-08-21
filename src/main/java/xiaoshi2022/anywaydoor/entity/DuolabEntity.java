package xiaoshi2022.anywaydoor.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import xiaoshi2022.anywaydoor.block.DimensionalDoorBlock;
import xiaoshi2022.anywaydoor.entity.goal.FindDorayakiGoal;
import xiaoshi2022.anywaydoor.entity.goal.FollowDorayakiPlayerGoal;
import xiaoshi2022.anywaydoor.entity.goal.JumpHelperGoal;
import xiaoshi2022.anywaydoor.entity.handler.ChatHandler;
import xiaoshi2022.anywaydoor.entity.handler.DoorHandler;
import xiaoshi2022.anywaydoor.entity.handler.PocketHandler;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;
import xiaoshi2022.anywaydoor.regsiter.ModEntities;
import xiaoshi2022.anywaydoor.regsiter.ModItems;

public class DuolabEntity extends PathfinderMob implements GeoEntity {

    // ===== 数据同步 =====
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(DuolabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SITTING =
            SynchedEntityData.defineId(DuolabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_JUMPING =
            SynchedEntityData.defineId(DuolabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND =
            SynchedEntityData.defineId(DuolabEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_OFF_HAND =
            SynchedEntityData.defineId(DuolabEntity.class, EntityDataSerializers.ITEM_STACK);

    // ===== 动画 =====
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlay("idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenPlay("walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ANIM_JUMP = RawAnimation.begin().thenPlay("jump");
    private static final RawAnimation ANIM_DOWN = RawAnimation.begin().thenPlayAndHold("down");
    private static final RawAnimation ANIM_OPEN_BAG = RawAnimation.begin().thenPlay("open_bag");
    private static final RawAnimation ANIM_PLAY = RawAnimation.begin().thenPlay("play");

    // ===== 处理器 =====
    public final PocketHandler pocket;
    public final DoorHandler door;
    public final ChatHandler chat;

    // ===== 其他 =====
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean isFlying = false;
    private float flyHeight = 0;
    public int jumpCooldown = 0;
    private int talkCooldown = 0;
    private int spawnProtectionTicks = 40;

    // ===== 新增：持久化状态标记 =====
    private boolean isPersistent = true;

    public DuolabEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setBoundingBox(new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
        this.setNoGravity(false);

        // ===== 标记为持久化 =====
        this.isPersistent = true;
        this.setPersistenceRequired();

        this.pocket = new PocketHandler(this);
        this.door = new DoorHandler(this, this.pocket);
        this.chat = new ChatHandler(this, this.pocket, this.door);
    }

    // ===== 属性 =====
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.JUMP_STRENGTH, 0.42D);
    }

    // ===== AI =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new JumpHelperGoal(this));
        this.goalSelector.addGoal(2, new FindDorayakiGoal(this));
        this.goalSelector.addGoal(2, new FollowDorayakiPlayerGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D) {
            @Override public boolean canUse() { return !isSitting() && super.canUse(); }
        });
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (p) -> p != null && getLastHurtByMob() != null && getLastHurtByMob() instanceof Player));
    }

    // ===== 同步数据 =====
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_WALKING, false);
        builder.define(DATA_IS_SITTING, false);
        builder.define(DATA_IS_JUMPING, false);
        builder.define(DATA_MAIN_HAND, ItemStack.EMPTY);
        builder.define(DATA_OFF_HAND, ItemStack.EMPTY);
    }

    // ===== 手持物品 =====
    public ItemStack getMainHandItem() { return entityData.get(DATA_MAIN_HAND); }
    public void setMainHandItem(ItemStack s) { entityData.set(DATA_MAIN_HAND, s.copy()); }
    public ItemStack getOffhandItem() { return entityData.get(DATA_OFF_HAND); }
    public void setOffhandItem(ItemStack s) { entityData.set(DATA_OFF_HAND, s.copy()); }

    // ===== 状态 =====
    public boolean isWalking() { return entityData.get(DATA_IS_WALKING); }
    public void setWalking(boolean b) { entityData.set(DATA_IS_WALKING, b); }
    public boolean isSitting() { return entityData.get(DATA_IS_SITTING); }
    public void setSitting(boolean b) {
        entityData.set(DATA_IS_SITTING, b);
        setNoGravity(b);
        if (b) { setDeltaMovement(Vec3.ZERO); getNavigation().stop(); }
    }
    public boolean isJumping() { return entityData.get(DATA_IS_JUMPING); }
    public void setJumping(boolean b) { entityData.set(DATA_IS_JUMPING, b); }

    public boolean isFlying() { return isFlying; }
    public void setFlying(boolean b) { isFlying = b; setNoGravity(b); }

    // ===== 动作触发 =====
    public void playAttack() { if (!level().isClientSide) triggerAnim("attack_controller", "attack"); }
    public void playJump() { if (!level().isClientSide) triggerAnim("jump_controller", "jump"); }
    public void playOpenBag() { if (!level().isClientSide) triggerAnim("bag_controller", "open_bag"); }
    public void playPlayAnimation() { if (!level().isClientSide) triggerAnim("main_controller", "play"); }

    // ===== GeckoLib =====
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (isSitting()) return state.setAndContinue(ANIM_DOWN);
            if (isJumping()) return state.setAndContinue(ANIM_JUMP);
            if (isWalking()) return state.setAndContinue(ANIM_WALK);
            return state.setAndContinue(ANIM_IDLE);
        }));
        c.add(new AnimationController<>(this, "attack_controller", 0, state -> state.setAndContinue(ANIM_ATTACK))
                .triggerableAnim("attack", ANIM_ATTACK));
        c.add(new AnimationController<>(this, "jump_controller", 0, state -> state.setAndContinue(ANIM_JUMP))
                .triggerableAnim("jump", ANIM_JUMP));
        c.add(new AnimationController<>(this, "bag_controller", 0, state -> state.setAndContinue(ANIM_OPEN_BAG))
                .triggerableAnim("open_bag", ANIM_OPEN_BAG));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o) { return tickCount; }

    // ===== Tick =====
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // ===== 确保持久化（每 100 tick 检查一次） =====
        if (isPersistent && tickCount % 100 == 0) {
            this.setPersistenceRequired();
        }

        if (spawnProtectionTicks > 0) spawnProtectionTicks--;

        // ===== 如果站在任意门上，自动走开 =====
        BlockPos blockPos = blockPosition();
        if (level().getBlockState(blockPos).getBlock() instanceof DimensionalDoorBlock ||
                level().getBlockState(blockPos.below()).getBlock() instanceof DimensionalDoorBlock) {
            if (tickCount % 5 == 0) {
                Vec3 dir = new Vec3(
                        random.nextDouble() - 0.5,
                        0,
                        random.nextDouble() - 0.5
                ).normalize().scale(0.5);
                setDeltaMovement(dir.x, 0.3, dir.z);
            }
        }

        if (talkCooldown > 0) talkCooldown--;
        if (jumpCooldown > 0) jumpCooldown--;
        chat.tick();
        pocket.tick();

        // 重力
        if (!isSitting() && !isFlying() && !isNoGravity() && !onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        }

        // 飞行
        if (isFlying) {
            flyHeight += 0.01;
            Vec3 currentPos = position();
            setPos(currentPos.x, currentPos.y + Math.sin(flyHeight) * 0.02, currentPos.z);
        }

        // 行走状态
        boolean moving = Math.abs(getDeltaMovement().x) > 0.01 ||
                Math.abs(getDeltaMovement().z) > 0.01 ||
                getNavigation().isInProgress();
        setWalking(isSitting() ? false : moving);
        if (onGround() && !moving) setJumping(false);

        // 自动跳跃
        if (!isSitting() && horizontalCollision && onGround()) {
            Vec3 vel = getDeltaMovement();
            if (Math.abs(vel.x) > 0.05 || Math.abs(vel.z) > 0.05) {
                double jp = getAttributeValue(Attributes.JUMP_STRENGTH);
                Vec3 look = getLookAngle();
                double bx = Math.abs(vel.x) > 0.1 ? vel.x * 1.1 : look.x * 0.6;
                double bz = Math.abs(vel.z) > 0.1 ? vel.z * 1.1 : look.z * 0.6;
                setDeltaMovement(bx + look.x * 0.1, jp * 1.1, bz + look.z * 0.1);
                playJump();
                setJumping(true);
                jumpCooldown = 10;
            }
        }
    }

    public static DuolabEntity create(Level level, Vec3 pos) {
        DuolabEntity entity = new DuolabEntity(ModEntities.DUOLAB, level);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setPersistenceRequired();
        entity.syncPacketPositionCodec(pos.x, pos.y, pos.z);
        return entity;
    }

    // ===== 交互 =====
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);

        if (pocket.isStoring()) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.storing_in_progress"), true);
            return InteractionResult.FAIL;
        }

        if (held.getItem() == ModItems.DORAYAKI) {
            if (pocket.tryStoreItem(held, player)) {
                held.shrink(1);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        if (held.getItem() == ModItems.DIMENSIONAL_DOOR_ITEM ||
                held.getItem() == ModBlocks.DIMENSIONAL_DOOR.asItem()) {
            if (pocket.tryStoreItem(held, player)) {
                held.shrink(1);
                player.displayClientMessage(
                        Component.translatable("entity.anyway-door.duolab.door_received"), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        if (held.isEmpty()) {
            setSitting(!isSitting());
            player.displayClientMessage(
                    Component.translatable(isSitting() ?
                            "entity.anyway-door.duolab.sit" :
                            "entity.anyway-door.duolab.stand"), true);
            return InteractionResult.SUCCESS;
        }

        if (pocket.isFull()) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.pocket_full"), true);
            return InteractionResult.FAIL;
        }

        ItemStack gift = held.copy();
        gift.setCount(1);
        if (pocket.tryStoreItem(gift, player)) {
            held.shrink(1);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    // ===== 战斗 =====
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            setTarget(player);
            playAttack();
            player.hurt(damageSources().mobAttack(this), 4.0F);
        }
        return super.hurt(source, amount);
    }

    // ===== 死亡 =====
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            for (ItemStack s : pocket.getItems()) spawnAtLocation(s);
            pocket.clear();
        }
        super.die(source);
    }

    // ===== NBT =====
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        isFlying = tag.getBoolean("IsFlying");
        flyHeight = tag.getFloat("FlyHeight");
        isPersistent = tag.getBoolean("IsPersistent");
        spawnProtectionTicks = tag.getInt("SpawnProtectionTicks");

        // ===== 关键：重新设置持久化 =====
        if (isPersistent) {
            this.setPersistenceRequired();
        }

        pocket.load(tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsFlying", isFlying);
        tag.putFloat("FlyHeight", flyHeight);
        tag.putBoolean("IsPersistent", isPersistent);
        tag.putInt("SpawnProtectionTicks", spawnProtectionTicks);
        pocket.save(tag);
    }

    // ===== 对外接口 =====
    public void setDoorTarget(Player p, String dim, int x, int y, int z) {
        if (!pocket.hasDoor()) {
            p.displayClientMessage(Component.translatable("entity.anyway-door.duolab.no_door"), false);
            return;
        }
        if (!(p instanceof ServerPlayer sp)) return;
    }

    public void tpToPlayer(Player p, String name) {}
    public void closeDoor(Player p) {}
    public void getDoorInfo(Player p) {}

    public void locateStructure(Player p, String name, int radius, String dim) {
        door.locateAndOpen(p, name, radius, dim);
    }

    // ===== 网络包 =====
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity e) {
        return super.getAddEntityPacket(e);
    }
}