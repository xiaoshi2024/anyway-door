package xiaoshi2022.anywaydoor.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import qouteall.imm_ptl.core.portal.Portal;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import xiaoshi2022.anywaydoor.AnywayDoor;
import xiaoshi2022.anywaydoor.regsiter.ModBlockEntities;
import xiaoshi2022.anywaydoor.teleport.DimensionalDoorTeleporter;

import java.util.*;

public class DimensionalDoorBlockEntity extends BlockEntity implements GeoBlockEntity {

	private static final String TAG_OPEN = "door_open";
	private static final String TAG_TARGET_DIMENSION = "target_dimension";
	private static final String TAG_TARGET_X = "target_x";
	private static final String TAG_TARGET_Y = "target_y";
	private static final String TAG_TARGET_Z = "target_z";
	private static final String TAG_PORTAL_ID = "portal_id";
	private static final String TAG_TARGET_DOOR_DIMENSION = "target_door_dimension";
	private static final String TAG_TARGET_DOOR_X = "target_door_x";
	private static final String TAG_TARGET_DOOR_Y = "target_door_y";
	private static final String TAG_TARGET_DOOR_Z = "target_door_z";

	private static final RawAnimation DOOR_OPEN = RawAnimation.begin().thenPlayAndHold("door_open");
	private static final RawAnimation DOOR_CLOSE = RawAnimation.begin().thenPlayAndHold("door_close");

	private static final long TELEPORT_COOLDOWN_TICKS = 20L;
	private final Map<UUID, Long> lastTeleportTime = new HashMap<>();

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	private boolean open = false;
	private ResourceKey<Level> targetDimension = Level.OVERWORLD;
	private int targetX = 0;
	private int targetY = 64;
	private int targetZ = 0;
	private int portalEntityId = -1;

	private ResourceKey<Level> targetDoorDimension = null;
	private BlockPos targetDoorPos = null;

	public DimensionalDoorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DIMENSIONAL_DOOR, pos, state);
		AnywayDoor.registerDoor(this);
		this.open = false;

		// ========== 生成随机目标 ==========
		generateRandomTarget();
	}

	// ========== 生成随机目标 ==========
	private void generateRandomTarget() {
		Random random = new Random();

		// 随机范围：-1000 到 1000
		int range = 1000;
		this.targetX = random.nextInt(range * 2) - range;
		this.targetZ = random.nextInt(range * 2) - range;
		this.targetY = 64 + random.nextInt(32); // 64 ~ 96

		// 10% 概率传送到下界
		if (random.nextDouble() < 0.1) {
			this.targetDimension = Level.NETHER;
			this.targetX = random.nextInt(200) - 100;
			this.targetZ = random.nextInt(200) - 100;
			this.targetY = 40 + random.nextInt(50); // 下界安全高度
		}

		// 5% 概率传送到末地
		if (random.nextDouble() < 0.05) {
			this.targetDimension = Level.END;
			this.targetX = random.nextInt(200) - 100;
			this.targetZ = random.nextInt(200) - 100;
			this.targetY = 50 + random.nextInt(30);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		AnywayDoor.unregisterDoor(this);
		cleanupTargetDoor();
		cleanupPortal();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "door_controller", 10, state -> {
			if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
				if (this.open) {
					return state.setAndContinue(DOOR_OPEN);
				} else {
					return state.setAndContinue(DOOR_CLOSE);
				}
			}
			return state.setAndContinue(this.open ? DOOR_OPEN : DOOR_CLOSE);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public double getTick(Object object) {
		return this.level == null ? 0 : this.level.getGameTime();
	}

	public boolean isOpen() { return this.open; }

	public void setOpen(boolean open) {
		this.open = open;
		this.setChanged();
		Level level = this.level;
		if (level != null) {
			level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public void setTarget(ResourceKey<Level> dimension, int x, int y, int z) {
		this.targetDimension = dimension;
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
		this.setChanged();
	}

	public ResourceKey<Level> getTargetDimension() { return this.targetDimension; }
	public int getTargetX() { return this.targetX; }
	public int getTargetY() { return this.targetY; }
	public int getTargetZ() { return this.targetZ; }

	public boolean canTeleport(UUID playerId) {
		if (level == null) return true;
		long currentTick = level.getGameTime();
		Long lastTick = lastTeleportTime.get(playerId);
		if (lastTick == null) return true;
		return currentTick - lastTick >= TELEPORT_COOLDOWN_TICKS;
	}

	public void markTeleported(UUID playerId) {
		if (level != null) {
			lastTeleportTime.put(playerId, level.getGameTime());
		}
	}

	// ==================== 目标门管理 ====================

	public void setTargetDoorPos(BlockPos pos, ResourceKey<Level> dimension) {
		this.targetDoorPos = pos;
		this.targetDoorDimension = dimension;
		this.setChanged();
	}

	public void cleanupTargetDoor() {
		if (targetDoorPos != null && targetDoorDimension != null) {
			ServerLevel targetLevel = null;
			if (level != null && level.getServer() != null) {
				targetLevel = level.getServer().getLevel(targetDoorDimension);
			}

			if (targetLevel != null) {
				BlockEntity targetBE = targetLevel.getBlockEntity(targetDoorPos);
				if (targetBE instanceof DimensionalDoorBlockEntity targetDoor) {
					targetDoor.cleanupPortal();
					targetDoor.setOpen(false);
				}
				targetLevel.removeBlock(targetDoorPos, false);
			}

			targetDoorPos = null;
			targetDoorDimension = null;
			this.setChanged();
		}
	}

	// ==================== 传送门管理 ====================

	public void setPortalEntity(Portal portal) {
		this.portalEntityId = portal == null ? -1 : portal.getId();
		this.setChanged();
		if (level != null) {
			level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public int getPortalEntityId() { return this.portalEntityId; }

	public void cleanupPortal() {
		if (portalEntityId != -1 && level != null) {
			net.minecraft.world.entity.Entity entity = level.getEntity(portalEntityId);
			if (entity instanceof Portal portal) {
				portal.discard();
			}
			portalEntityId = -1;
			this.setChanged();
		}
	}

	public void cleanupAllPortals() {
		cleanupPortal();
		cleanupTargetDoor();
	}

	// ==================== 玩家检测与传送 ====================

	public void tick() {
		if (level == null || level.isClientSide || !open) {
			return;
		}

		AABB detectionBox = new AABB(
				worldPosition.getX() + 0.05,
				worldPosition.getY() + 0.15,
				worldPosition.getZ() + 0.05,
				worldPosition.getX() + 0.95,
				worldPosition.getY() + 2.2,
				worldPosition.getZ() + 0.95
		);

		List<ServerPlayer> players = ((ServerLevel) level).getPlayers(
				p -> p.isAlive() &&
						detectionBox.intersects(p.getBoundingBox()) &&
						canTeleport(p.getUUID())
		);

		for (ServerPlayer player : players) {
			markTeleported(player.getUUID());
			DimensionalDoorTeleporter.teleportPlayer(player, this);
		}
	}

	// ==================== NBT 保存/加载 ====================

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean(TAG_OPEN, this.open);
		tag.putString(TAG_TARGET_DIMENSION, this.targetDimension.location().toString());
		tag.putInt(TAG_TARGET_X, this.targetX);
		tag.putInt(TAG_TARGET_Y, this.targetY);
		tag.putInt(TAG_TARGET_Z, this.targetZ);
		tag.putInt(TAG_PORTAL_ID, this.portalEntityId);

		if (targetDoorPos != null && targetDoorDimension != null) {
			tag.putString(TAG_TARGET_DOOR_DIMENSION, targetDoorDimension.location().toString());
			tag.putInt(TAG_TARGET_DOOR_X, targetDoorPos.getX());
			tag.putInt(TAG_TARGET_DOOR_Y, targetDoorPos.getY());
			tag.putInt(TAG_TARGET_DOOR_Z, targetDoorPos.getZ());
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.open = tag.getBoolean(TAG_OPEN);

		String dimName = tag.getString(TAG_TARGET_DIMENSION);
		if (!dimName.isEmpty()) {
			this.targetDimension = ResourceKey.create(
					net.minecraft.core.registries.Registries.DIMENSION,
					ResourceLocation.tryParse(dimName)
			);
		} else {
			// 如果 NBT 没有维度信息，生成随机目标
			generateRandomTarget();
		}

		// 只有当 NBT 有坐标时才读取，否则保持随机生成的值
		if (tag.contains(TAG_TARGET_X)) {
			this.targetX = tag.getInt(TAG_TARGET_X);
			this.targetY = tag.getInt(TAG_TARGET_Y);
			this.targetZ = tag.getInt(TAG_TARGET_Z);
		}

		this.portalEntityId = tag.getInt(TAG_PORTAL_ID);

		if (tag.contains(TAG_TARGET_DOOR_DIMENSION) && tag.contains(TAG_TARGET_DOOR_X)) {
			String targetDimName = tag.getString(TAG_TARGET_DOOR_DIMENSION);
			if (!targetDimName.isEmpty()) {
				this.targetDoorDimension = ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						ResourceLocation.tryParse(targetDimName)
				);
				this.targetDoorPos = new BlockPos(
						tag.getInt(TAG_TARGET_DOOR_X),
						tag.getInt(TAG_TARGET_DOOR_Y),
						tag.getInt(TAG_TARGET_DOOR_Z)
				);
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putBoolean(TAG_OPEN, this.open);
		return tag;
	}

	@Override
	@org.jetbrains.annotations.Nullable
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}