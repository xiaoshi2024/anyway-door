package xiaoshi2022.anywaydoor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.command.StructureNameArgumentType;
import xiaoshi2022.anywaydoor.listener.ChatListener;
import xiaoshi2022.anywaydoor.regsiter.*;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AnywayDoor implements ModInitializer {
	public static final String MOD_ID = "anyway-door";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final CopyOnWriteArrayList<DimensionalDoorBlockEntity> DOORS = new CopyOnWriteArrayList<>();

	// ========== 玩家名称补全提供器 ==========
	public static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS =
			(context, builder) -> {
				CommandSourceStack source = context.getSource();
				for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
					builder.suggest(player.getName().getString());
				}
				return builder.buildFuture();
			};

	// ========== 结构名称补全提供器（支持所有模组） ==========
	public static final SuggestionProvider<CommandSourceStack> STRUCTURE_SUGGESTIONS =
			(context, builder) -> {
				CommandSourceStack source = context.getSource();
				Registry<Structure> structureRegistry = source.getServer().registryAccess()
						.registryOrThrow(Registries.STRUCTURE);

				for (ResourceKey<Structure> key : structureRegistry.registryKeySet()) {
					ResourceLocation location = key.location();
					String id = location.toString();
					builder.suggest(id);

					if (location.getNamespace().equals("minecraft")) {
						builder.suggest(location.getPath());
					}
				}
				return builder.buildFuture();
			};

	public static void registerDoor(DimensionalDoorBlockEntity door) {
		if (!DOORS.contains(door)) {
			DOORS.add(door);
			LOGGER.debug("注册任意门: {}", door.getBlockPos());
		}
	}

	public static void unregisterDoor(DimensionalDoorBlockEntity door) {
		DOORS.remove(door);
		LOGGER.debug("注销任意门: {}", door.getBlockPos());
	}

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModBlockEntities.init();
		ModArgumentTypes.init();
		ModItems.init();          // 注册物品
		ModEntities.init();       // 注册实体
		ModItemGroups.init();     // 注册创造模式物品栏
		registerCommands();
		registerCreativeTabs();

		// ========== 注册聊天监听器 ==========
		ChatListener.register();

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world instanceof ServerLevel serverLevel) {
				for (DimensionalDoorBlockEntity door : DOORS) {
					if (!door.isRemoved() && door.getLevel() == serverLevel) {
						door.tick();
					}
				}
			}
		});

		LOGGER.info("任意门 (Dimensional Door) initialized.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	private void registerCreativeTabs() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
				.register(entries -> entries.accept(ModBlocks.DIMENSIONAL_DOOR));
	}

	/**
	 * 注册指令：
	 * /rym set <维度> <x> <y> <z>     - 设置目标坐标
	 * /rym set <x> <y> <z>            - 设置目标坐标（当前维度）
	 * /rym tp <玩家名>                - 设置目标为指定玩家附近
	 * /rym locate <结构名> [半径]     - 定位结构（默认100区块，原版标准）
	 * /rym list_structures            - 列出所有可用的结构
	 * /rym info                       - 查看当前门信息
	 * /rym close                      - 关闭门
	 */
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("rym")
						// ========== set 指令 ==========
						.then(literal("set")
								.then(argument("dimension", StringArgumentType.word())
										.then(argument("x", IntegerArgumentType.integer())
												.then(argument("y", IntegerArgumentType.integer())
														.then(argument("z", IntegerArgumentType.integer())
																.executes(ctx -> {
																	String dimName = StringArgumentType.getString(ctx, "dimension");
																	int x = IntegerArgumentType.getInteger(ctx, "x");
																	int y = IntegerArgumentType.getInteger(ctx, "y");
																	int z = IntegerArgumentType.getInteger(ctx, "z");
																	return setTargetOfLookedAtDoor(
																			ctx.getSource().getPlayerOrException(),
																			dimName, x, y, z
																	);
																})))))
								.then(argument("x", IntegerArgumentType.integer())
										.then(argument("y", IntegerArgumentType.integer())
												.then(argument("z", IntegerArgumentType.integer())
														.executes(ctx -> {
															int x = IntegerArgumentType.getInteger(ctx, "x");
															int y = IntegerArgumentType.getInteger(ctx, "y");
															int z = IntegerArgumentType.getInteger(ctx, "z");
															return setTargetOfLookedAtDoor(
																	ctx.getSource().getPlayerOrException(),
																	null, x, y, z
															);
														})))))
						// ========== tp 指令 ==========
						.then(literal("tp")
								.then(argument("targetPlayer", StringArgumentType.word())
										.suggests(PLAYER_SUGGESTIONS)
										.executes(ctx -> {
											String targetName = StringArgumentType.getString(ctx, "targetPlayer");
											return setTargetToPlayer(
													ctx.getSource().getPlayerOrException(),
													targetName
											);
										})))
						// ========== locate 指令 - 使用自定义参数类型 ==========
						.then(literal("locate")
								.then(argument("structure", StructureNameArgumentType.structure())
										.suggests(STRUCTURE_SUGGESTIONS)
										.executes(ctx -> {
											String structure = StructureNameArgumentType.getStructure(ctx, "structure");
											return locateStructure(ctx.getSource().getPlayerOrException(), structure, 100, null);
										})
										.then(argument("radius", IntegerArgumentType.integer(1, 10000))
												.executes(ctx -> {
													String structure = StructureNameArgumentType.getStructure(ctx, "structure");
													int radius = IntegerArgumentType.getInteger(ctx, "radius");
													return locateStructure(ctx.getSource().getPlayerOrException(), structure, radius, null);
												})
												.then(argument("dimension", StringArgumentType.word())
														.executes(ctx -> {
															String structure = StructureNameArgumentType.getStructure(ctx, "structure");
															int radius = IntegerArgumentType.getInteger(ctx, "radius");
															String dimension = StringArgumentType.getString(ctx, "dimension");
															return locateStructure(ctx.getSource().getPlayerOrException(), structure, radius, dimension);
														})
												)
										)
								)
						)
						// ========== list_structures 指令 ==========
						.then(literal("list_structures")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									return listStructures(player);
								}))
						// ========== info 指令 ==========
						.then(literal("info")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									DimensionalDoorBlockEntity door = findLookedAtDoor(player);
									if (door == null) {
										player.displayClientMessage(
												Component.translatable("command.rym.no_door_looking"),
												true
										);
										return 0;
									}
									String dimName = door.getTargetDimension().location().toString();
									player.displayClientMessage(
											Component.literal("§a目标: §b" + dimName + " §a(§b" +
													door.getTargetX() + ", " +
													door.getTargetY() + ", " +
													door.getTargetZ() + "§a)  状态: " +
													(door.isOpen() ? "§2开启中" : "§c已关闭")),
											true
									);
									return 1;
								}))
						// ========== close 指令 ==========
						.then(literal("close")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									DimensionalDoorBlockEntity door = findLookedAtDoor(player);
									if (door == null) {
										player.displayClientMessage(
												Component.translatable("command.rym.no_door_looking"),
												true
										);
										return 0;
									}
									if (!door.isOpen()) {
										player.displayClientMessage(
												Component.translatable("command.rym.already_closed"),
												true
										);
										return 0;
									}
									door.cleanupPortal();
									door.setOpen(false);
									player.displayClientMessage(
											Component.translatable("command.rym.door_closed"),
											true
									);
									return 1;
								}))
		));
	}

	/**
	 * 列出所有可用的结构
	 */
	private int listStructures(ServerPlayer player) {
		Registry<Structure> structureRegistry = player.server.registryAccess()
				.registryOrThrow(Registries.STRUCTURE);

		StringBuilder message = new StringBuilder();
		message.append("§6=== 可用结构列表 ===\n");
		int count = 0;

		for (ResourceKey<Structure> key : structureRegistry.registryKeySet()) {
			ResourceLocation location = key.location();
			String modName = location.getNamespace();
			String structureName = location.getPath();

			if (count == 0) {
				message.append("§e").append(modName).append(":\n");
			}

			message.append("  §7- §f").append(structureName).append("\n");
			count++;
		}

		if (count == 0) {
			player.displayClientMessage(
					Component.translatable("command.rym.list_structures_empty"),
					true
			);
		} else {
			String[] lines = message.toString().split("\n");
			int totalLines = lines.length;
			int pageSize = 20;

			StringBuilder display = new StringBuilder();
			display.append(Component.translatable("command.rym.list_structures_title", count).getString());
			int end = Math.min(pageSize, totalLines);
			for (int i = 0; i < end; i++) {
				display.append(lines[i]).append("\n");
			}

			player.displayClientMessage(
					Component.literal(display.toString()),
					false
			);
		}

		return 1;
	}

	/**
	 * 定位结构并设置为目标（支持跨维度）
	 */
	private int locateStructure(ServerPlayer player, String input, int radiusChunks, String dimensionName) {
		DimensionalDoorBlockEntity door = findLookedAtDoor(player);
		if (door == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.no_door_looking"),
					true
			);
			return 0;
		}

		// ========== 解析结构名和维度 ==========
		// input 可能包含: "minecraft:fortress 50 the_nether"
		String structureName = input;
		int radius = radiusChunks;
		String dimName = dimensionName;

		// 如果没有指定维度，尝试从 input 中解析
		if (dimName == null) {
			String[] parts = input.trim().split("\\s+");
			if (parts.length >= 2) {
				// 尝试解析: "structure radius" 或 "structure dimension"
				try {
					radius = Integer.parseInt(parts[parts.length - 1]);
					structureName = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
				} catch (NumberFormatException ignored) {
					// 最后一个不是数字，可能是维度名
					dimName = parts[parts.length - 1];
					structureName = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
				}
			}
		}

		// ========== 解析结构 ID ==========
		ResourceLocation structureId;
		if (structureName.contains(":")) {
			structureId = ResourceLocation.tryParse(structureName);
		} else {
			structureId = ResourceLocation.tryParse("minecraft:" + structureName);
		}

		if (structureId == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.structure_invalid", structureName),
					true
			);
			return 0;
		}

		// ========== 解析目标维度 ==========
		ServerLevel targetLevel;
		if (dimName == null || dimName.isEmpty()) {
			targetLevel = player.serverLevel();
		} else {
			ResourceLocation dimId = ResourceLocation.tryParse(dimName);
			if (dimId == null) {
				// 尝试补全 minecraft:
				dimId = ResourceLocation.tryParse("minecraft:" + dimName);
			}
			if (dimId == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.invalid_dimension", dimName),
						true
				);
				return 0;
			}
			ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
			ServerLevel level = player.server.getLevel(dimKey);
			if (level == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.dimension_not_exist", dimName),
						true
				);
				return 0;
			}
			targetLevel = level;
		}

		BlockPos playerPos = player.blockPosition();

		try {
			Registry<Structure> structureRegistry = player.server.registryAccess()
					.registryOrThrow(Registries.STRUCTURE);

			Optional<Holder.Reference<Structure>> structureHolder = structureRegistry
					.getHolder(ResourceKey.create(Registries.STRUCTURE, structureId));

			if (structureHolder.isEmpty()) {
				player.displayClientMessage(
						Component.translatable("command.rym.structure_not_exist", structureId),
						true
				);
				return 0;
			}

			HolderSet<Structure> holderSet = HolderSet.direct(structureHolder.get());

			// ========== 在目标维度中查找结构 ==========
			com.mojang.datafixers.util.Pair<BlockPos, Holder<Structure>> result = targetLevel.getChunkSource()
					.getGenerator()
					.findNearestMapStructure(
							targetLevel,
							holderSet,
							playerPos,
							radius,
							false
					);

			if (result == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.structure_not_found", structureId, radius),
						true
				);
				return 0;
			}

			BlockPos foundPos = result.getFirst();
			BlockPos safePos = findStructureSurface(targetLevel, foundPos);

			if (safePos == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.structure_no_safe_pos", structureId),
						true
				);
				return 0;
			}

			// ========== 设置门的目标 ==========
			door.setTarget(targetLevel.dimension(), safePos.getX(), safePos.getY(), safePos.getZ());

			float distance = Mth.sqrt((float) (
					(safePos.getX() - playerPos.getX()) * (safePos.getX() - playerPos.getX()) +
							(safePos.getZ() - playerPos.getZ()) * (safePos.getZ() - playerPos.getZ())
			));

			String distanceStr = distance < 1000 ?
					String.format("%.0f", distance) + "m" :
					String.format("%.1f", distance / 1000) + "km";

			String modName = structureId.getNamespace();
			String structureDisplayName = structureId.getPath();
			if (!modName.equals("minecraft")) {
				try {
					var modContainer = net.fabricmc.loader.api.FabricLoader.getInstance()
							.getModContainer(modName);
					if (modContainer.isPresent()) {
						structureDisplayName = structureDisplayName + " §7(" +
								modContainer.get().getMetadata().getName() + ")";
					}
				} catch (Exception ignored) {}
			}

			String dimDisplay = targetLevel.dimension().location().toString();
			if (!dimDisplay.equals(player.serverLevel().dimension().location().toString())) {
				player.displayClientMessage(
						Component.translatable("command.rym.structure_located_dim",
								structureDisplayName, dimDisplay, distanceStr, safePos.getX(), safePos.getY(), safePos.getZ()),
						true
				);
			} else {
				player.displayClientMessage(
						Component.translatable("command.rym.structure_located",
								structureDisplayName, distanceStr, safePos.getX(), safePos.getY(), safePos.getZ()),
						true
				);
			}

			if (door.isOpen()) {
				player.displayClientMessage(
						Component.translatable("command.rym.door_reopen_warning"),
						true
				);
			}

			return 1;

		} catch (Exception e) {
			LOGGER.error("定位结构时发生错误: {}", structureId, e);
			player.displayClientMessage(
					Component.translatable("command.rym.structure_error", e.getMessage()),
					true
			);
			return 0;
		}
	}

	/**
	 * 在结构位置附近找到地面可站立位置
	 */
	private BlockPos findStructureSurface(ServerLevel level, BlockPos structurePos) {
		int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
				structurePos.getX(), structurePos.getZ());

		BlockPos surfacePos = new BlockPos(structurePos.getX(), surfaceY, structurePos.getZ());

		if (isSafePosition(level, surfacePos)) {
			return surfacePos;
		}

		for (int radius = 1; radius <= 10; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

					int checkX = structurePos.getX() + dx;
					int checkZ = structurePos.getZ() + dz;
					int checkY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, checkX, checkZ);

					BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);
					if (isSafePosition(level, checkPos)) {
						return checkPos;
					}
				}
			}
		}

		for (int dy = -10; dy <= 50; dy++) {
			BlockPos testPos = structurePos.above(dy);
			if (testPos.getY() > level.getMaxBuildHeight()) break;
			if (isSafePosition(level, testPos)) {
				return testPos;
			}
		}

		return structurePos.above(15);
	}

	/**
	 * 设置目标为指定玩家附近的安全位置
	 */
	private int setTargetToPlayer(ServerPlayer player, String targetName) {
		DimensionalDoorBlockEntity door = findLookedAtDoor(player);
		if (door == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.no_door_looking"),
					true
			);
			return 0;
		}

		ServerPlayer targetPlayer = player.server.getPlayerList().getPlayerByName(targetName);
		if (targetPlayer == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.player_not_found", targetName),
					true
			);
			return 0;
		}

		ServerLevel targetLevel = targetPlayer.serverLevel();
		BlockPos targetPos = targetPlayer.blockPosition();
		ResourceKey<Level> dimension = targetLevel.dimension();

		BlockPos safePos = findSafePosition(targetLevel, targetPos, 5);

		if (safePos == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.player_no_safe_pos", targetName),
					true
			);
			return 0;
		}

		door.setTarget(dimension, safePos.getX(), safePos.getY(), safePos.getZ());
		String dimDisplay = dimension.location().toString();

		player.displayClientMessage(
				Component.translatable("command.rym.player_target_set",
						targetName, dimDisplay, safePos.getX(), safePos.getY(), safePos.getZ()),
				true
		);

		if (door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.door_reopen_warning"),
					true
			);
		}

		return 1;
	}

	/**
	 * 在指定位置附近查找安全位置
	 */
	private BlockPos findSafePosition(ServerLevel level, BlockPos center, int radius) {
		for (int r = 2; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
					if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;

					BlockPos checkPos = center.offset(dx, 0, dz);
					for (int dy = -1; dy <= 1; dy++) {
						BlockPos testPos = checkPos.offset(0, dy, 0);
						if (isSafePosition(level, testPos)) {
							return testPos;
						}
					}
				}
			}
		}

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos checkPos = center.offset(dx, 0, dz);
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos testPos = checkPos.offset(0, dy, 0);
					if (isSafePosition(level, testPos)) {
						return testPos;
					}
				}
			}
		}

		for (int dy = -5; dy <= 5; dy++) {
			BlockPos testPos = center.offset(0, dy, 0);
			if (isSafePosition(level, testPos)) {
				return testPos;
			}
		}

		return null;
	}

	/**
	 * 检查位置是否安全（可站立）
	 */
	private boolean isSafePosition(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		if (!level.getBlockState(pos.above()).isAir()) {
			return false;
		}
		if (level.getBlockState(pos.below()).isAir()) {
			return false;
		}
		return true;
	}

	private int setTargetOfLookedAtDoor(ServerPlayer player, String dimName, int x, int y, int z) {
		DimensionalDoorBlockEntity door = findLookedAtDoor(player);
		if (door == null) {
			player.displayClientMessage(
					Component.translatable("command.rym.no_door_looking"),
					true
			);
			return 0;
		}

		ResourceKey<Level> dimension;
		if (dimName == null || dimName.isEmpty()) {
			dimension = player.level().dimension();
		} else {
			ResourceLocation dimId = ResourceLocation.tryParse(dimName);
			if (dimId == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.invalid_dimension", dimName),
						true
				);
				return 0;
			}
			dimension = ResourceKey.create(Registries.DIMENSION, dimId);
			if (player.server.getLevel(dimension) == null) {
				player.displayClientMessage(
						Component.translatable("command.rym.dimension_not_exist", dimName),
						true
				);
				return 0;
			}
		}

		door.setTarget(dimension, x, y, z);
		String dimDisplay = dimension.location().toString();
		player.displayClientMessage(
				Component.translatable("command.rym.target_set_success", dimDisplay, x, y, z),
				true
		);

		if (door.isOpen()) {
			player.displayClientMessage(
					Component.translatable("command.rym.door_reopen_warning"),
					true
			);
		}

		return 1;
	}

	private DimensionalDoorBlockEntity findLookedAtDoor(ServerPlayer player) {
		Level level = player.level();
		HitResult hit = player.pick(8.0, 1.0F, false);
		BlockPos pos = hit instanceof BlockHitResult bhr ? bhr.getBlockPos() : null;
		if (pos == null) {
			return null;
		}
		if (level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door) {
			return door;
		}
		return null;
	}

	// ========== 工具方法 ==========
	private static class Mth {
		public static float sqrt(float value) {
			return (float) Math.sqrt(value);
		}
	}
}