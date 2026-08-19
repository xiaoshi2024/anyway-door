package xiaoshi2022.anywaydoor.entity.handler;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;
import xiaoshi2022.anywaydoor.regsiter.ModItems;

import java.util.List;

public class PocketHandler {
    private final DuolabEntity entity;
    private final NonNullList<ItemStack> pocketItems = NonNullList.create();
    private static final int MAX_SIZE = 27;
    private static final int STORE_DELAY = 100; // 5秒 (20 ticks/秒)

    // ===== 待存储物品 =====
    private ItemStack pendingItem = ItemStack.EMPTY;
    private int storeTimer = -1;

    // ===== 是否拥有"无限门"权限 =====
    private boolean hasInfiniteDoor = true;

    public PocketHandler(DuolabEntity entity) {
        this.entity = entity;
        if (!entity.level().isClientSide) {
            addDoorToPocket();
        }
    }

    private void addDoorToPocket() {
        ItemStack door = new ItemStack(ModItems.DIMENSIONAL_DOOR_ITEM);
        addItem(door);
        hasInfiniteDoor = true;
    }

    /**
     * 尝试存储物品到口袋（带5秒延迟）
     * @param stack 要存储的物品
     * @param player 给予物品的玩家（用于消息反馈）
     * @return 是否开始存储流程
     */
    public boolean tryStoreItem(ItemStack stack, Player player) {
        if (stack.isEmpty()) return false;
        if (isFull()) {
            player.displayClientMessage(
                    Component.translatable("entity.anyway-door.duolab.pocket_full"), true);
            return false;
        }

        // 设置待存储物品
        this.pendingItem = stack.copy();
        this.storeTimer = STORE_DELAY;

        // 让哆啦B梦手里拿着物品
        entity.setMainHandItem(stack);

        // 播放打开口袋动画
        entity.playOpenBag();

        player.displayClientMessage(
                Component.translatable("entity.anyway-door.duolab.gift_received",
                        stack.getDisplayName().getString()), true);

        return true;
    }

    /**
     * 每 tick 调用，处理存储计时
     */
    public void tick() {
        if (storeTimer > 0) {
            storeTimer--;
            if (storeTimer == 0 && !pendingItem.isEmpty()) {
                // 5秒到了，存入口袋
                if (addItem(pendingItem)) {
                    entity.setMainHandItem(ItemStack.EMPTY);
                    entity.level().players().forEach(p -> {
                        if (p.distanceTo(entity) < 20) {
                            p.displayClientMessage(
                                    Component.translatable("entity.anyway-door.duolab.stored",
                                            pendingItem.getDisplayName().getString()), true);
                        }
                    });
                } else {
                    // 口袋满了，掉出来
                    entity.spawnAtLocation(pendingItem);
                    entity.setMainHandItem(ItemStack.EMPTY);
                    entity.level().players().forEach(p -> {
                        if (p.distanceTo(entity) < 20) {
                            p.displayClientMessage(
                                    Component.translatable("entity.anyway-door.duolab.pocket_full_drop"), true);
                        }
                    });
                }
                pendingItem = ItemStack.EMPTY;
            }
        }
    }

    /**
     * 检查是否有正在存储的物品
     */
    public boolean isStoring() {
        return storeTimer > 0 && !pendingItem.isEmpty();
    }

    /**
     * 获取存储进度 (0.0 ~ 1.0)
     */
    public float getStoreProgress() {
        if (storeTimer <= 0) return 0.0f;
        return 1.0f - (storeTimer / (float) STORE_DELAY);
    }

    public boolean addItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (pocketItems.size() >= MAX_SIZE) return false;

        for (ItemStack existing : pocketItems) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return true;
                }
            }
        }

        if (!stack.isEmpty()) {
            pocketItems.add(stack.copy());
            stack.setCount(0);
            return true;
        }
        return false;
    }

    public List<ItemStack> getItems() { return List.copyOf(pocketItems); }
    public void clear() { pocketItems.clear(); }
    public boolean isEmpty() { return pocketItems.isEmpty(); }
    public boolean isFull() { return pocketItems.size() >= MAX_SIZE; }
    public int size() { return pocketItems.size(); }

    public boolean hasDoor() {
        for (ItemStack stack : pocketItems) {
            if (stack.getItem() == ModItems.DIMENSIONAL_DOOR_ITEM ||
                    stack.getItem() == ModBlocks.DIMENSIONAL_DOOR.asItem()) {
                return true;
            }
        }
        return false;
    }

    public boolean consumeDoor() {
        for (int i = 0; i < pocketItems.size(); i++) {
            ItemStack stack = pocketItems.get(i);
            if (stack.getItem() == ModItems.DIMENSIONAL_DOOR_ITEM ||
                    stack.getItem() == ModBlocks.DIMENSIONAL_DOOR.asItem()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    pocketItems.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    public void giveDoorToPlayer(Player player) {
        ItemStack doorStack = new ItemStack(ModItems.DIMENSIONAL_DOOR_ITEM);
        player.getInventory().add(doorStack);
    }

    public void save(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : pocketItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(entity.registryAccess(), itemTag);
            listTag.add(itemTag);
        }
        tag.put("PocketItems", listTag);
        tag.putBoolean("HasInfiniteDoor", hasInfiniteDoor);

        // 保存待存储物品
        if (!pendingItem.isEmpty()) {
            CompoundTag pendingTag = new CompoundTag();
            pendingItem.save(entity.registryAccess(), pendingTag);
            tag.put("PendingItem", pendingTag);
            tag.putInt("StoreTimer", storeTimer);
        }
    }

    public void load(CompoundTag tag) {
        pocketItems.clear();
        if (tag.contains("PocketItems", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("PocketItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(entity.registryAccess(), listTag.getCompound(i));
                if (!stack.isEmpty()) pocketItems.add(stack);
            }
        }
        hasInfiniteDoor = tag.getBoolean("HasInfiniteDoor");

        // 加载待存储物品
        if (tag.contains("PendingItem")) {
            pendingItem = ItemStack.parseOptional(entity.registryAccess(), tag.getCompound("PendingItem"));
            storeTimer = tag.getInt("StoreTimer");
        } else {
            pendingItem = ItemStack.EMPTY;
            storeTimer = -1;
        }

        if (!hasDoor() && hasInfiniteDoor && !entity.level().isClientSide) {
            addDoorToPocket();
        }
    }
}