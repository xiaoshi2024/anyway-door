package xiaoshi2022.anywaydoor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.regsiter.ModBlockEntities;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;

import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AnywayDoor implements ModInitializer {
	public static final String MOD_ID = "anyway-door";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final CopyOnWriteArrayList<DimensionalDoorBlockEntity> DOORS = new CopyOnWriteArrayList<>();

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
		registerCommands();
		registerCreativeTabs();

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
	 * 注册指令：/rym set <维度> <x> <y> <z>
	 *          /rym set <x> <y> <z> (默认当前维度)
	 *          /rym info
	 *          /rym close
	 */
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				// ========== 指令改为 rym（任意门拼音首字母） ==========
				literal("rym")
						// ========== set 指令 ==========
						.then(literal("set")
								// 用法: /rym set <维度> <x> <y> <z>
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
								// 用法: /rym set <x> <y> <z> (默认当前维度)
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
						// ========== info 指令 ==========
						.then(literal("info")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayerOrException();
									DimensionalDoorBlockEntity door = findLookedAtDoor(player);
									if (door == null) {
										player.displayClientMessage(
												Component.literal("§c没有正在注视任意门"),
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
												Component.literal("§c没有正在注视任意门"),
												true
										);
										return 0;
									}
									if (!door.isOpen()) {
										player.displayClientMessage(
												Component.literal("§e任意门已经关闭了"),
												true
										);
										return 0;
									}
									door.cleanupPortal();
									door.setOpen(false);
									player.displayClientMessage(
											Component.literal("§a✦ 已关闭任意门"),
											true
									);
									return 1;
								}))
		));
	}

	private int setTargetOfLookedAtDoor(ServerPlayer player, String dimName, int x, int y, int z) {
		DimensionalDoorBlockEntity door = findLookedAtDoor(player);
		if (door == null) {
			player.displayClientMessage(
					Component.literal("§c没有正在注视任意门"),
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
						Component.literal("§c无效的维度名称: " + dimName),
						true
				);
				return 0;
			}
			dimension = ResourceKey.create(Registries.DIMENSION, dimId);
			if (player.server.getLevel(dimension) == null) {
				player.displayClientMessage(
						Component.literal("§c维度不存在: " + dimName),
						true
				);
				return 0;
			}
		}

		door.setTarget(dimension, x, y, z);
		String dimDisplay = dimension.location().toString();
		player.displayClientMessage(
				Component.literal("§a✓ 目标已设置为 §b" + dimDisplay + " §a(§b" + x + ", " + y + ", " + z + "§a)"),
				true
		);
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
}