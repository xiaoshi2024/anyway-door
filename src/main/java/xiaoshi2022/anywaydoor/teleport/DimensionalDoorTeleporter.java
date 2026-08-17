package xiaoshi2022.anywaydoor.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;

public final class DimensionalDoorTeleporter {

	private DimensionalDoorTeleporter() {
	}

	public static boolean openDoor(Level level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door)) {
			return false;
		}

		if (door.isOpen()) {
			player.displayClientMessage(
					Component.literal("§e任意门已经打开了"),
					true
			);
			return false;
		}

		door.setOpen(true);

		// ========== 播放开门音效 ==========
		level.playSound(
				null,
				pos,
				SoundEvents.IRON_DOOR_OPEN,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);

		double tx = door.getTargetX() + 0.5;
		double ty = door.getTargetY() + 0.5;
		double tz = door.getTargetZ() + 0.5;
		ResourceKey<Level> targetDim = door.getTargetDimension();

		if (level instanceof ServerLevel serverLevel) {
			ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDim);
			if (targetLevel == null) {
				player.displayClientMessage(
						Component.literal("§c目标维度不存在: " + targetDim.location()),
						true
				);
				door.setOpen(false);
				return false;
			}

			Vec3 origin = new Vec3(
					pos.getX() + 0.5,
					pos.getY() + 19.0 / 16.0,
					pos.getZ() + 0.5 + (-7.0 / 16.0)
			);

			Portal portal = new Portal(Portal.ENTITY_TYPE, serverLevel);
			portal.setOriginPos(origin);
			portal.setDestinationDimension(targetLevel.dimension());
			portal.setDestination(new Vec3(tx, ty, tz));
			portal.setWidth(1.0);
			portal.setHeight(2.0);
			portal.setAxisW(new Vec3(-1, 0, 0));
			portal.setAxisH(new Vec3(0, 1, 0));
			portal.setTeleportable(true);
			portal.setInteractable(true);

			PortalAPI.spawnServerEntity(portal);
			door.setPortalEntity(portal);
		}

		player.displayClientMessage(
				Component.literal("§a✦ 任意门已开启！走进门即可传送"),
				true
		);

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
					Component.literal("§e任意门已经关闭了"),
					true
			);
			return false;
		}

		door.cleanupPortal();
		door.setOpen(false);

		// ========== 播放关门音效 ==========
		level.playSound(
				null,
				pos,
				SoundEvents.IRON_DOOR_CLOSE,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);

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
					Component.literal("§c任意门已经关闭了"),
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
					Component.literal("§c目标维度不存在: " + targetDim.location()),
					true
			);
			return false;
		}

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