package xiaoshi2022.anywaydoor.entity.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.anywaydoor.block.DimensionalDoorBlock;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;
import xiaoshi2022.anywaydoor.regsiter.ModItems;
import xiaoshi2022.anywaydoor.teleport.DimensionalDoorTeleporter;

import java.util.Optional;

public class DoorHandler {
    private final DuolabEntity entity;
    private final PocketHandler pocket;

    public DoorHandler(DuolabEntity entity, PocketHandler pocket) {
        this.entity = entity;
        this.pocket = pocket;
    }

    public BlockPos placeDoorInFront(Player player) {
        // ===== 先检查有没有门可以消耗 =====
        if (!pocket.consumeDoor()) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.no_door"), false);
            return null;
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 pos = player.position();

        BlockPos doorPos = BlockPos.containing(
                pos.x + lookVec.x * 2.5,
                pos.y + 0.5,
                pos.z + lookVec.z * 2.5
        );

        // 找地面
        while (doorPos.getY() > -60 &&
                entity.level().getBlockState(doorPos.below()).isAir() &&
                !entity.level().getBlockState(doorPos.below()).canBeReplaced()) {
            doorPos = doorPos.below();
        }

        // 检查位置
        if (!entity.level().getBlockState(doorPos).isAir() &&
                !entity.level().getBlockState(doorPos).canBeReplaced()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos tryPos = doorPos.offset(dx, 0, dz);
                    if (entity.level().getBlockState(tryPos).isAir() ||
                            entity.level().getBlockState(tryPos).canBeReplaced()) {
                        doorPos = tryPos;
                        break;
                    }
                }
            }
            if (!entity.level().getBlockState(doorPos).isAir() &&
                    !entity.level().getBlockState(doorPos).canBeReplaced()) {
                // 位置被占用，但门已经消耗了... 需要补偿
                // 简单补偿：重新给一个门
                ItemStack refund = new ItemStack(ModItems.DIMENSIONAL_DOOR_ITEM);
                pocket.addItem(refund);
                return null;
            }
        }

        Direction facing = player.getDirection().getOpposite();
        BlockState doorState = ModBlocks.DIMENSIONAL_DOOR.defaultBlockState()
                .setValue(DimensionalDoorBlock.FACING, facing);
        entity.level().setBlock(doorPos, doorState, 3);

        return doorPos;
    }

    public boolean locateAndOpen(Player player, String structureName, int radius, String dimension) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (!pocket.hasDoor()) return false;

        if (!(entity.level() instanceof ServerLevel currentLevel)) return false;

        // 1. 放置门
        BlockPos doorPos = placeDoorInFront(player);
        if (doorPos == null) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.no_space"), false);
            return false;
        }

        // 2. 获取门的BlockEntity
        if (!(entity.level().getBlockEntity(doorPos) instanceof DimensionalDoorBlockEntity door)) {
            return false;
        }

        // 3. 解析结构
        ResourceLocation structureId = parseStructureId(structureName);
        if (structureId == null) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.invalid_structure", structureName), false);
            return false;
        }

        // 4. 获取目标维度
        ServerLevel targetLevel = getTargetLevel(dimension);
        if (targetLevel == null) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.invalid_dimension", dimension), false);
            return false;
        }

        // 5. 定位结构
        BlockPos foundPos = findStructure(targetLevel, structureId, player.blockPosition(), radius);
        if (foundPos == null) {
            // 尝试所有村庄变体
            if (structureName.equals("village")) {
                foundPos = findVillage(targetLevel, player.blockPosition(), radius);
            }
        }

        if (foundPos == null) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.structure_not_found_radius", structureName, radius), false);
            return false;
        }

        // 6. 找安全位置
        BlockPos safePos = findSafePosition(targetLevel, foundPos);

        // 7. 设置目标并开门
        door.setTarget(targetLevel.dimension(), safePos.getX(), safePos.getY(), safePos.getZ());
        DimensionalDoorTeleporter.openDoor(entity.level(), doorPos, serverPlayer);

        player.displayClientMessage(
                Component.translatable("entity.anyway-door.duolab.door_to_structure",
                        structureName, safePos.getX(), safePos.getY(), safePos.getZ()), false);

        return true;
    }

    private ResourceLocation parseStructureId(String name) {
        if (name.contains(":")) {
            return ResourceLocation.tryParse(name);
        }
        return ResourceLocation.tryParse("minecraft:" + name);
    }

    private ServerLevel getTargetLevel(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return (ServerLevel) entity.level();
        }
        ResourceLocation dimId = ResourceLocation.tryParse(dimension);
        if (dimId == null) {
            dimId = ResourceLocation.tryParse("minecraft:" + dimension);
        }
        if (dimId == null) return null;
        return entity.level().getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, dimId)
        );
    }

    private BlockPos findStructure(ServerLevel level, ResourceLocation id, BlockPos playerPos, int radius) {
        Registry<Structure> registry = entity.level().getServer().registryAccess()
                .registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder =
                registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) return null;

        HolderSet<Structure> set = HolderSet.direct(holder.get());
        var result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, set, playerPos, radius, false);
        return result != null ? result.getFirst() : null;
    }

    private BlockPos findVillage(ServerLevel level, BlockPos playerPos, int radius) {
        String[] types = {"village_plains", "village_desert", "village_savanna",
                "village_taiga", "village_snowy", "village"};
        for (String type : types) {
            BlockPos pos = findStructure(level,
                    ResourceLocation.tryParse("minecraft:" + type), playerPos, Math.max(radius, 200));
            if (pos != null) return pos;
        }
        return null;
    }

    private BlockPos findSafePosition(ServerLevel level, BlockPos pos) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                pos.getX(), pos.getZ());
        BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
        if (isSafe(level, check)) return check;

        for (int r = 1; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int cy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            pos.getX() + dx, pos.getZ() + dz);
                    BlockPos cp = new BlockPos(pos.getX() + dx, cy, pos.getZ() + dz);
                    if (isSafe(level, cp)) return cp;
                }
            }
        }
        return pos.above(5);
    }

    private boolean isSafe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() &&
                level.getBlockState(pos.above()).isAir() &&
                !level.getBlockState(pos.below()).isAir();
    }
}