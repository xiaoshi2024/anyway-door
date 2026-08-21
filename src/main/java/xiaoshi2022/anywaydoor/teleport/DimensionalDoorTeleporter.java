package xiaoshi2022.anywaydoor.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		ResourceKey<Level> targetDim = door.getTargetDimension();
		ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDim);
		if (targetLevel == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.dimension_not_found", targetDim.location()),
					true
			);
			return false;
		}

		BlockPos targetPos = new BlockPos(door.getTargetX(), door.getTargetY(), door.getTargetZ());

		// ============================================================
		// ========== 1. 强制加载目标区块 ==========
		// ============================================================
		ChunkPos targetChunkPos = new ChunkPos(targetPos);

		// 方法1: 直接加载区块
		targetLevel.getChunk(targetPos);

		// 方法2: 使用 Ticket 强制保持加载
		targetLevel.getChunkSource().addRegionTicket(
				TicketType.PORTAL,
				targetChunkPos,
				3,
				targetPos
		);

		// 方法3: 确保区块完全加载（多次尝试）
		int attempts = 0;
		while (!targetLevel.isLoaded(targetPos) && attempts < 10) {
			targetLevel.getChunk(targetPos);
			attempts++;
			// 不要用 Thread.sleep，用 tick 延迟
		}

		// ============================================================
		// ========== 2. 检查目标位置并创建/关联反向门 ==========
		// ============================================================
		BlockState targetBlockState = targetLevel.getBlockState(targetPos);
		boolean targetDoorCreated = false;
		boolean targetIsExistingDoor = false;

		Direction facing = level.getBlockState(pos).getValue(DimensionalDoorBlock.FACING);
		Vec3 axisW = getAxisW(facing);
		Vec3 axisH = new Vec3(0, 1, 0);

		// ---------- 情况1: 目标位置已经是任意门 ----------
		if (targetLevel.getBlockEntity(targetPos) instanceof DimensionalDoorBlockEntity existingTargetDoor) {
			targetIsExistingDoor = true;
			targetDoorCreated = true;

			// 检查这个门是否已经关联到当前门
			BlockPos existingParentPos = existingTargetDoor.getParentDoorPos();
			ResourceKey<Level> existingParentDim = existingTargetDoor.getParentDoorDimension();

			// 如果还没有关联或者关联的不是当前门，则重新建立关联
			if (existingParentPos == null || !existingParentPos.equals(pos) ||
					existingParentDim == null || !existingParentDim.equals(serverLevel.dimension())) {

				// 如果这个门已经有父门，先断开旧关联
				if (existingParentPos != null && existingParentDim != null) {
					ServerLevel oldParentLevel = serverLevel.getServer().getLevel(existingParentDim);
					if (oldParentLevel != null) {
						BlockEntity oldParentBE = oldParentLevel.getBlockEntity(existingParentPos);
						if (oldParentBE instanceof DimensionalDoorBlockEntity oldParentDoor) {
							oldParentDoor.setTargetDoorPos(null, null);
							oldParentDoor.cleanupPortal();
							oldParentDoor.setOpen(false);
							oldParentDoor.setChanged();
						}
					}
				}

				// 建立新关联
				existingTargetDoor.setAsReverseDoor(pos, serverLevel.dimension());
				existingTargetDoor.setTarget(serverLevel.dimension(), pos.getX(), pos.getY(), pos.getZ());
				existingTargetDoor.setOpen(true);
				existingTargetDoor.setChanged();
				targetLevel.sendBlockUpdated(targetPos, targetBlockState, targetBlockState, 3);

				// 设置当前门的 targetDoorPos 指向目标门
				door.setTargetDoorPos(targetPos, targetDim);

				// 清理目标门的旧传送门
				existingTargetDoor.cleanupPortal();

				// 为目标门创建传送门
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
				existingTargetDoor.setPortalEntity(reversePortal);
			}

			player.displayClientMessage(
					Component.translatable("command.rym.linked_existing_door"),
					true
			);

			// ---------- 情况2: 目标位置为空，创建新门 ----------
		} else if (targetBlockState.isAir() || targetBlockState.canBeReplaced()) {
			Direction targetFacing = facing.getOpposite();
			BlockState doorState = ModBlocks.DIMENSIONAL_DOOR.defaultBlockState()
					.setValue(DimensionalDoorBlock.FACING, targetFacing);
			targetLevel.setBlockAndUpdate(targetPos, doorState);

			if (targetLevel.getBlockEntity(targetPos) instanceof DimensionalDoorBlockEntity targetDoor) {
				targetDoorCreated = true;

				// 设置反向门
				targetDoor.setAsReverseDoor(pos, serverLevel.dimension());
				targetDoor.setTarget(serverLevel.dimension(), pos.getX(), pos.getY(), pos.getZ());
				targetDoor.setOpen(true);

				// 设置父门的 targetDoorPos 指向反向门
				door.setTargetDoorPos(targetPos, targetDim);

				// 创建反向传送门
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

				targetDoor.setPortalEntity(reversePortal);

				player.displayClientMessage(
						Component.translatable("command.rym.target_set"),
						true
				);
			}

			// ---------- 情况3: 目标被其他方块占用 ----------
		} else {
			String blockName = targetBlockState.getBlock().getName().getString();
			player.displayClientMessage(
					Component.translatable("command.rym.target_occupied", blockName),
					true
			);
			return false;
		}

		// ============================================================
		// ========== 3. 当前位置生成正向传送门 ==========
		// ============================================================
		Vec3 origin = getDoorCenter(pos, facing);

		Portal forwardPortal = new Portal(Portal.ENTITY_TYPE, serverLevel);
		forwardPortal.setOriginPos(origin);
		forwardPortal.setDestinationDimension(targetLevel.dimension());

		if (targetDoorCreated) {
			Direction targetFacing = targetLevel.getBlockState(targetPos).getValue(DimensionalDoorBlock.FACING);
			forwardPortal.setDestination(getDoorCenter(targetPos, targetFacing));
		} else {
			forwardPortal.setDestination(new Vec3(
					door.getTargetX() + 0.5,
					door.getTargetY() + 0.5,
					door.getTargetZ() + 0.5
			));
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

		door.setPortalEntity(forwardPortal);
		door.setOpen(true);

		// ============================================================
		// ========== 4. 显示提示信息 ==========
		// ============================================================
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

		level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);

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

		// ========== 清理当前门的传送门 ==========
		door.cleanupPortal();
		door.setOpen(false);

		// ========== 清理目标门的传送门（保留方块） ==========
		BlockPos targetDoorPos = door.getTargetDoorPos();
		ResourceKey<Level> targetDoorDimension = door.getTargetDoorDimension();
		if (targetDoorPos != null && targetDoorDimension != null && level.getServer() != null) {
			ServerLevel targetLevel = level.getServer().getLevel(targetDoorDimension);
			if (targetLevel != null) {
				BlockEntity targetBE = targetLevel.getBlockEntity(targetDoorPos);
				if (targetBE instanceof DimensionalDoorBlockEntity targetDoor) {
					// 只清理传送门，不删除方块！
					targetDoor.cleanupPortal();
					targetDoor.setOpen(false);
					targetDoor.setChanged();
					targetLevel.sendBlockUpdated(targetDoorPos, targetLevel.getBlockState(targetDoorPos),
							targetLevel.getBlockState(targetDoorPos), 3);
				}
			}
		}

		// ========== 如果当前门是反向门，也清理父门的传送门 ==========
		if (door.isReverseDoor()) {
			BlockPos parentDoorPos = door.getParentDoorPos();
			ResourceKey<Level> parentDoorDimension = door.getParentDoorDimension();
			if (parentDoorPos != null && parentDoorDimension != null && level.getServer() != null) {
				ServerLevel parentLevel = level.getServer().getLevel(parentDoorDimension);
				if (parentLevel != null) {
					BlockEntity parentBE = parentLevel.getBlockEntity(parentDoorPos);
					if (parentBE instanceof DimensionalDoorBlockEntity parentDoor) {
						parentDoor.cleanupPortal();
						parentDoor.setOpen(false);
						parentDoor.setChanged();
						parentLevel.sendBlockUpdated(parentDoorPos, parentLevel.getBlockState(parentDoorPos),
								parentLevel.getBlockState(parentDoorPos), 3);
					}
				}
			}
		}

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

	public static boolean teleportPlayer(ServerPlayer player, DimensionalDoorBlockEntity door) {
		if (!door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.door_closed_for_teleport"),
					true
			);
			return false;
		}

		Level level = door.getLevel();
		if (level == null) return false;

		BlockPos doorPos = door.getBlockPos();
		BlockState state = level.getBlockState(doorPos);
		if (!(state.getBlock() instanceof DimensionalDoorBlock)) return false;

		Direction facing = state.getValue(DimensionalDoorBlock.FACING);
		Vec3 normal = getNormal(facing);
		Vec3 doorCenter = getDoorCenter(doorPos, facing);

		Vec3 playerPos = player.position();
		Vec3 relativePos = playerPos.subtract(doorCenter);
		double dotProduct = relativePos.dot(normal);

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

		// ========== 确保目标区块加载 ==========
		BlockPos targetPos = new BlockPos((int) tx, (int) ty, (int) tz);
		targetLevel.getChunk(targetPos);
		if (!targetLevel.isLoaded(targetPos)) {
			targetLevel.getChunkSource().addRegionTicket(
					TicketType.PORTAL,
					new ChunkPos(targetPos),
					3,
					targetPos
			);
			targetLevel.getChunk(targetPos);
		}

		PortalAPI.teleportEntity(player, targetLevel, new Vec3(tx, ty, tz));

		// ========== 传送音效 ==========
		targetLevel.playSound(
				null,
				new BlockPos((int) tx, (int) ty, (int) tz),
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);

		player.displayClientMessage(
				Component.translatable("command.rym.teleport_success"),
				true
		);
		return true;
	}
}