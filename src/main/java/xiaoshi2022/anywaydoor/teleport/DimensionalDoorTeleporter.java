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
		// door2 的 pivot 在 (8, 19, -7)，门板中心在 (0, 19, -7) 相对方块原点
		// 根据朝向旋转偏移
		double offsetX = 0;
		double offsetZ = -7.0 / 16.0;  // 默认朝北，门板在 Z 负方向

		// 根据朝向旋转偏移
		switch (facing) {
			case NORTH -> {
				// 不变: (0, 0, -7)
			}
			case SOUTH -> {
				// 绕 Y 旋转 180°: (0, 0, 7)
				offsetZ = 7.0 / 16.0;
			}
			case EAST -> {
				// 绕 Y 旋转 90°: (7, 0, 0)
				offsetX = 7.0 / 16.0;
				offsetZ = 0;
			}
			case WEST -> {
				// 绕 Y 旋转 -90°: (-7, 0, 0)
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

	public static boolean openDoor(Level level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door)) {
			return false;
		}

		// ========== 检查目标是否已设置 ==========
		if (!door.isTargetSet()) {
			player.displayClientMessage(
					Component.literal("§c✦ 请先使用 /rym set 设置目标地点！"),
					true
			);
			return false;
		}

		if (door.isOpen()) {
			player.displayClientMessage(
					Component.literal("§e✦ 任意门已经打开了"),
					true
			);
			return false;
		}

		door.setOpen(true);

		// ========== 播放开门音效 ==========
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
					Component.literal("§c✦ 目标维度不存在: " + targetDim.location()),
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

				// 设置目标门的传送目标为当前门的位置
				targetDoor.setTarget(serverLevel.dimension(), pos.getX(), pos.getY(), pos.getZ());
				targetDoor.setOpen(true);

				Direction targetDoorFacing = targetLevel.getBlockState(targetPos).getValue(DimensionalDoorBlock.FACING);
				Vec3 targetAxisW = getAxisW(targetDoorFacing);

				// 反向传送门位置
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

				PortalAPI.spawnServerEntity(reversePortal);
				targetDoor.setPortalEntity(reversePortal);

				door.setTargetDoorPos(targetPos, targetDim);

				player.displayClientMessage(
						Component.literal("§a✦ 目标位置已生成返回门"),
						true
				);
			}
		} else {
			String blockName = targetBlockState.getBlock().getName().getString();
			player.displayClientMessage(
					Component.literal("§c✦ 目标位置被 " + blockName + " 占用，无法生成返回门"),
					true
			);
		}

		// ========== 2. 当前位置生成正向传送门 ==========
		Vec3 origin = getDoorCenter(pos, facing);

		Portal forwardPortal = new Portal(Portal.ENTITY_TYPE, serverLevel);
		forwardPortal.setOriginPos(origin);
		forwardPortal.setDestinationDimension(targetLevel.dimension());

		// 如果生成了反向门，传送到反向门表面；否则传送到目标坐标
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

		PortalAPI.spawnServerEntity(forwardPortal);
		door.setPortalEntity(forwardPortal);

		// ========== 显示提示信息 ==========
		String dimDisplay = targetDim.location().toString();
		String targetInfo = String.format("§a✦ 任意门已开启 → §b%s §a(§b%d, %d, %d§a) §e走进门传送",
				dimDisplay, door.getTargetX(), door.getTargetY(), door.getTargetZ());
		player.displayClientMessage(Component.literal(targetInfo), true);

		level.players().forEach(p -> {
			if (p != player && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100) {
				p.displayClientMessage(
						Component.literal("§7✦ 附近一扇任意门被打开了"),
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
					Component.literal("§e✦ 任意门已经关闭了"),
					true
			);
			return false;
		}

		// 清理当前门的传送门
		door.cleanupPortal();
		door.setOpen(false);

		// 清理目标位置的反向门
		door.cleanupTargetDoor();

		// ========== 播放关门音效 ==========
		level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);

		player.displayClientMessage(
				Component.literal("§a✦ 已关闭任意门"),
				true
		);

		level.players().forEach(p -> {
			if (p != player && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100) {
				p.displayClientMessage(
						Component.literal("§7✦ 附近一扇任意门被关闭了"),
						true
				);
			}
		});

		return true;
	}

	public static boolean teleportPlayer(ServerPlayer player, DimensionalDoorBlockEntity door) {
		if (!door.isOpen()) {
			player.displayClientMessage(
					Component.literal("§c✦ 任意门已经关闭了"),
					true
			);
			return false;
		}

		ResourceKey<Level> targetDim = door.getTargetDimension();
		double tx = door.getTargetX() + 0.5;
		double ty = door.getTargetY() + 0.5;
		double tz = door.getTargetZ() + 0.5;

		ServerLevel targetLevel = player.server.getLevel(targetDim);
		if (targetLevel == null) {
			player.displayClientMessage(
					Component.literal("§c✦ 目标维度不存在: " + targetDim.location()),
					true
			);
			return false;
		}

		// ========== 强制加载目标区块 ==========
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

		player.displayClientMessage(
				Component.literal("§a✦ 传送成功！"),
				true
		);
		return true;
	}
}