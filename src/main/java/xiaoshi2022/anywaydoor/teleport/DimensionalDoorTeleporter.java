package xiaoshi2022.anywaydoor.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import xiaoshi2022.anywaydoor.block.DimensionalDoorBlock;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;

public final class DimensionalDoorTeleporter {

	private DimensionalDoorTeleporter() {
	}

	// ========== 获取门板中心位置（根据朝向） ==========
	private static Vec3 getDoorCenter(BlockPos pos, Direction facing) {
		double offsetX = 0;
		double offsetZ = -7.0 / 16.0;

		switch (facing) {
			case NORTH -> {}
			case SOUTH -> offsetZ = 7.0 / 16.0;
			case EAST -> {
				offsetX = 7.0 / 16.0;
				offsetZ = 0;
			}
			case WEST -> {
				offsetX = -7.0 / 16.0;
				offsetZ = 0;
			}
			default -> {}
		}

		return new Vec3(
				pos.getX() + 0.5 + offsetX,
				pos.getY() + 19.0 / 16.0,
				pos.getZ() + 0.5 + offsetZ
		);
	}

	// ========== 根据门的朝向获取宽度方向向量 ==========
	private static Vec3 getAxisW(Direction facing) {
		return switch (facing) {
			case NORTH -> new Vec3(-1, 0, 0);
			case SOUTH -> new Vec3(1, 0, 0);
			case EAST -> new Vec3(0, 0, -1);
			case WEST -> new Vec3(0, 0, 1);
			default -> new Vec3(-1, 0, 0);
		};
	}

	// ========== 获取门的法线方向（正面朝向） ==========
	private static Vec3 getNormal(Direction facing) {
		return switch (facing) {
			case NORTH -> new Vec3(0, 0, -1);
			case SOUTH -> new Vec3(0, 0, 1);
			case EAST -> new Vec3(1, 0, 0);
			case WEST -> new Vec3(-1, 0, 0);
			default -> new Vec3(0, 0, -1);
		};
	}

	public static boolean openDoor(Level level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door)) {
			return false;
		}

		if (!door.isTargetSet()) {
			player.displayClientMessage(
					Component.translatable("command.rym.set.usage"),
					true
			);
			return false;
		}

		if (door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.already_open"),
					true
			);
			return false;
		}

		door.setOpen(true);
		level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);

		double tx = door.getTargetX() + 0.5;
		double ty = door.getTargetY() + 0.5;
		double tz = door.getTargetZ() + 0.5;
		ResourceKey<Level> targetDim = door.getTargetDimension();

		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDim);
		if (targetLevel == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.dimension_not_found", targetDim.location()),
					true
			);
			door.setOpen(false);
			return false;
		}

		Direction facing = level.getBlockState(pos).getValue(DimensionalDoorBlock.FACING);
		Vec3 axisW = getAxisW(facing);
		Vec3 axisH = new Vec3(0, 1, 0);

		BlockPos targetPos = new BlockPos(door.getTargetX(), door.getTargetY(), door.getTargetZ());

		// ========== 1. 目标位置生成反向门 ==========
		targetLevel.getChunk(targetPos);

		BlockState targetBlockState = targetLevel.getBlockState(targetPos);
		boolean targetDoorCreated = false;

		if (targetBlockState.isAir() || targetBlockState.canBeReplaced()) {
			Direction targetFacing = facing.getOpposite();
			BlockState doorState = ModBlocks.DIMENSIONAL_DOOR.defaultBlockState()
					.setValue(DimensionalDoorBlock.FACING, targetFacing);
			targetLevel.setBlockAndUpdate(targetPos, doorState);

			if (targetLevel.getBlockEntity(targetPos) instanceof DimensionalDoorBlockEntity targetDoor) {
				targetDoorCreated = true;

				targetDoor.setAsReverseDoor(pos, serverLevel.dimension());
				targetDoor.setTarget(serverLevel.dimension(), pos.getX(), pos.getY(), pos.getZ());
				targetDoor.setOpen(true);

				Direction targetDoorFacing = targetLevel.getBlockState(targetPos).getValue(DimensionalDoorBlock.FACING);
				Vec3 targetAxisW = getAxisW(targetDoorFacing);

				Vec3 targetOrigin = getDoorCenter(targetPos, targetDoorFacing);

				Portal reversePortal = new Portal(Portal.ENTITY_TYPE, targetLevel);
				reversePortal.setOriginPos(targetOrigin);
				reversePortal.setDestinationDimension(serverLevel.dimension());
				reversePortal.setDestination(getDoorCenter(pos, facing));
				reversePortal.setWidth(1.0);
				reversePortal.setHeight(2.0);
				reversePortal.setAxisW(targetAxisW);
				reversePortal.setAxisH(axisH);
				reversePortal.setTeleportable(true);
				reversePortal.setInteractable(true);
				reversePortal.setIsVisible(true);
				reversePortal.specificPlayerId = null;

				PortalAPI.spawnServerEntity(reversePortal);
				McHelper.resendSpawnPacketToTrackers(reversePortal);

				targetDoor.setPortalEntity(reversePortal);
				door.setTargetDoorPos(targetPos, targetDim);

				player.displayClientMessage(
						Component.translatable("command.rym.target_set"),
						true
				);
			}
		} else {
			String blockName = targetBlockState.getBlock().getName().getString();
			player.displayClientMessage(
					Component.translatable("command.rym.target_occupied", blockName),
					true
			);
		}

		// ========== 2. 当前位置生成正向传送门 ==========
		Vec3 origin = getDoorCenter(pos, facing);

		Portal forwardPortal = new Portal(Portal.ENTITY_TYPE, serverLevel);
		forwardPortal.setOriginPos(origin);
		forwardPortal.setDestinationDimension(targetLevel.dimension());

		if (targetDoorCreated) {
			Direction targetFacing = facing.getOpposite();
			forwardPortal.setDestination(getDoorCenter(targetPos, targetFacing));
		} else {
			forwardPortal.setDestination(new Vec3(tx, ty, tz));
		}

		forwardPortal.setWidth(1.0);
		forwardPortal.setHeight(2.0);
		forwardPortal.setAxisW(axisW);
		forwardPortal.setAxisH(axisH);
		forwardPortal.setTeleportable(true);
		forwardPortal.setInteractable(true);
		forwardPortal.setIsVisible(true);
		forwardPortal.specificPlayerId = null;

		PortalAPI.spawnServerEntity(forwardPortal);
		McHelper.resendSpawnPacketToTrackers(forwardPortal);

		door.setPortalEntity(forwardPortal);

		// ========== 显示提示信息 ==========
		String dimDisplay = targetDim.location().toString();
		String targetInfo = String.format("§a✦ 任意门已开启 → §b%s §a(§b%d, %d, %d§a) §e走进门传送",
				dimDisplay, door.getTargetX(), door.getTargetY(), door.getTargetZ());
		player.displayClientMessage(Component.literal(targetInfo), true);

		level.players().forEach(p -> {
			if (p != player && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100) {
				p.displayClientMessage(
						Component.translatable("command.rym.nearby_opened"),
						true
				);
			}
		});

		return true;
	}

	public static boolean closeDoor(Level level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door)) {
			return false;
		}

		if (!door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.already_closed"),
					true
			);
			return false;
		}

		door.cleanupPortal();
		door.setOpen(false);
		door.cleanupTargetDoor();

		level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);

		player.displayClientMessage(
				Component.translatable("command.rym.door_closed"),
				true
		);

		level.players().forEach(p -> {
			if (p != player && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100) {
				p.displayClientMessage(
						Component.translatable("command.rym.nearby_closed"),
						true
				);
			}
		});

		return true;
	}

	/**
	 * 传送玩家 - 添加方向检测，防止从背面进入
	 */
	public static boolean teleportPlayer(ServerPlayer player, DimensionalDoorBlockEntity door) {
		if (!door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.door_closed_for_teleport"),
					true
			);
			return false;
		}

		// ========== 检测玩家是否从正面进入 ==========
		Level level = door.getLevel();
		if (level == null) return false;

		BlockPos doorPos = door.getBlockPos();
		BlockState state = level.getBlockState(doorPos);
		if (!(state.getBlock() instanceof DimensionalDoorBlock)) return false;

		Direction facing = state.getValue(DimensionalDoorBlock.FACING);
		Vec3 normal = getNormal(facing);
		Vec3 doorCenter = getDoorCenter(doorPos, facing);

		// 计算玩家相对于门的位置
		Vec3 playerPos = player.position();
		Vec3 relativePos = playerPos.subtract(doorCenter);
		double dotProduct = relativePos.dot(normal);

		// ========== 如果玩家在门的背面（法线方向相反），拒绝传送 ==========
		if (dotProduct < 0) {
			player.displayClientMessage(
					Component.translatable("command.rym.wrong_side"),
					true
			);
			return false;
		}

		// ========== 执行传送 ==========
		ResourceKey<Level> targetDim = door.getTargetDimension();
		double tx = door.getTargetX() + 0.5;
		double ty = door.getTargetY() + 0.5;
		double tz = door.getTargetZ() + 0.5;

		ServerLevel targetLevel = player.server.getLevel(targetDim);
		if (targetLevel == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.dimension_not_found", targetDim.location()),
					true
			);
			return false;
		}

		targetLevel.getChunk(new BlockPos((int) tx, (int) ty, (int) tz));

		// ========== 传送音效 ==========
		player.level().playSound(
				null,
				player.blockPosition(),
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);

		player.teleportTo(targetLevel, tx, ty, tz, player.getYRot(), player.getXRot());

		targetLevel.playSound(
				null,
				new BlockPos((int) tx, (int) ty, (int) tz),
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);

		// ========== 修复：显示传送成功 ==========
		player.displayClientMessage(
				Component.translatable("command.rym.teleport_success"),
				true
		);
		return true;
	}
}