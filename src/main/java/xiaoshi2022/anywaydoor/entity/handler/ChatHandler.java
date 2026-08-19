package xiaoshi2022.anywaydoor.entity.handler;

import net.minecraft.nbt.CompoundTag;  // ← 添加
import net.minecraft.nbt.ListTag;     // ← 添加
import net.minecraft.nbt.Tag;          // ← 添加
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;  // ← 添加
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ChatHandler {
    private final DuolabEntity entity;
    private final PocketHandler pocket;
    private final DoorHandler door;
    private int talkCooldown = 0;
    private boolean hasGivenDoor = false;
    private final Map<String, Integer> givenTracker = new HashMap<>();

    public ChatHandler(DuolabEntity entity, PocketHandler pocket, DoorHandler door) {
        this.entity = entity;
        this.pocket = pocket;
        this.door = door;
    }

    public void tick() {
        if (talkCooldown > 0) talkCooldown--;
    }

    public void onPlayerChat(Player player, String message) {
        if (talkCooldown > 0) return;

        // 铜锣烧
        if (message.contains("铜锣烧") || message.toLowerCase().contains("dorayaki")) {
            handleDorayaki(player);
            return;
        }

        // 任意门
        if (message.contains("任意门") || message.toLowerCase().contains("anywaydoor")) {
            handleDoorRequest(player);
            return;
        }

        // 去结构
        if (isStructureRequest(message)) {
            handleStructureRequest(player, message);
            return;
        }

        // 设置门目标
        if (message.contains("设置门") || message.contains("设定门")) {
            handleSetTarget(player, message);
            return;
        }

        // 传送玩家
        if (message.contains("传送到") && message.contains("玩家")) {
            handleTpPlayer(player, message);
            return;
        }

        // 关门
        if (message.contains("关门") || message.contains("关闭门")) {
            handleCloseDoor(player);
            return;
        }

        // 门信息
        if (message.contains("门信息") || message.contains("门状态")) {
            handleDoorInfo(player);
            return;
        }
    }

    private void handleDorayaki(Player player) {
        ItemStack dorayaki = new ItemStack(ModItems.DORAYAKI);  // ← 改成 ItemStack
        if (hasGivenItemBefore(dorayaki)) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.already_given"), false);
            talkCooldown = 40;
            return;
        }

        if (pocket.addItem(dorayaki)) {
            recordItemGiven(dorayaki);
            entity.playOpenBag();
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.dorayaki_given"), false);
            talkCooldown = 60;
        } else {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.pocket_full"), false);
            talkCooldown = 40;
        }
    }

    private void handleDoorRequest(Player player) {
        if (hasGivenDoor) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.no_more_door"), false);
            talkCooldown = 40;
            return;
        }

        // ===== 直接给玩家一个门，不消耗口袋 =====
        pocket.giveDoorToPlayer(player);

        hasGivenDoor = true;
        entity.playOpenBag();
        entity.playPlayAnimation();
        player.displayClientMessage(
                Component.translatable("entity.anyway-door.duolab.found_door"), false);
        talkCooldown = 60;
    }

    private boolean isStructureRequest(String message) {
        // 先检查是否包含结构名称
        String[] structures = {"村庄", "要塞", "神殿", "堡垒", "城市", "地牢", "遗迹", "塔", "矿井"};
        boolean hasStructure = false;
        for (String s : structures) {
            if (message.contains(s)) {
                hasStructure = true;
                break;
            }
        }
        if (!hasStructure) return false;

        // 检查是否包含动作词
        String[] words = {"找", "去", "想去", "带我去", "我要去", "到", "我要"};
        for (String w : words) {
            if (message.contains(w)) {
                return true;
            }
        }

        // ===== 如果消息以"哆啦B梦"开头且包含结构名，也视为请求 =====
        if (message.contains("哆啦B梦") || message.contains("哆啦") || message.contains("DuoLab")) {
            return true;
        }

        return false;
    }

    private void handleStructureRequest(Player player, String message) {
        String structureName = extractStructureName(message);
        if (structureName == null) return;

        int radius = extractRadius(message, 200);
        String dimension = extractDimension(message);
        entity.locateStructure(player, structureName, radius, dimension);
        talkCooldown = 40;
    }

    private void handleSetTarget(Player player, String message) {
        String[] parts = message.replace("设置门", "").replace("设定门", "").trim().split("\\s+");
        try {
            if (parts.length >= 3) {
                int x = Integer.parseInt(parts[parts.length - 3]);
                int y = Integer.parseInt(parts[parts.length - 2]);
                int z = Integer.parseInt(parts[parts.length - 1]);
                String dimension = "minecraft:overworld";
                if (parts.length >= 4) {
                    String dim = parts[0].replace(":", "").replace("minecraft", "");
                    if (!dim.isEmpty()) dimension = "minecraft:" + dim;
                }
                entity.setDoorTarget(player, dimension, x, y, z);
                talkCooldown = 40;
            }
        } catch (NumberFormatException ignored) {}
    }

    private void handleTpPlayer(Player player, String message) {
        Pattern p = Pattern.compile("传送到\\s*(\\w+)");
        var m = p.matcher(message);
        if (m.find()) {
            entity.tpToPlayer(player, m.group(1));
            talkCooldown = 40;
        }
    }

    private void handleCloseDoor(Player player) {
        entity.closeDoor(player);
        talkCooldown = 40;
    }

    private void handleDoorInfo(Player player) {
        entity.getDoorInfo(player);
        talkCooldown = 40;
    }

    private boolean hasGivenItemBefore(ItemStack stack) {  // ← 参数改成 ItemStack
        String id = stack.getItem().getDescriptionId();
        return givenTracker.getOrDefault(id, 0) >= 1;
    }

    private void recordItemGiven(ItemStack stack) {  // ← 参数改成 ItemStack
        String id = stack.getItem().getDescriptionId();
        givenTracker.put(id, givenTracker.getOrDefault(id, 0) + 1);
    }

    private String extractStructureName(String msg) {
        String[] map = {
                "村庄", "village", "要塞", "stronghold", "神殿", "temple",
                "堡垒", "bastion", "城市", "city", "地牢", "dungeon",
                "遗迹", "ruins", "塔", "pillager_outpost", "矿井", "mineshaft"
        };
        for (int i = 0; i < map.length; i += 2) {
            if (msg.contains(map[i])) return map[i+1];
        }
        return null;
    }

    private int extractRadius(String msg, int def) {
        var p = Pattern.compile("(\\d+)\\s*格");
        var m = p.matcher(msg);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) {}
        }
        return def;
    }

    private String extractDimension(String msg) {
        if (msg.contains("地狱")) return "minecraft:the_nether";
        if (msg.contains("末地")) return "minecraft:the_end";
        if (msg.contains("主世界") || msg.contains("现实")) return "minecraft:overworld";
        return null;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("HasGivenDoor", hasGivenDoor);
        // 保存 givenTracker...
    }

    public void load(CompoundTag tag) {
        hasGivenDoor = tag.getBoolean("HasGivenDoor");
        // 加载 givenTracker...
    }
}