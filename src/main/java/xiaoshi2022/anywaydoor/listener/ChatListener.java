package xiaoshi2022.anywaydoor.listener;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;

public class ChatListener {

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayer player = sender;
            Level level = player.level();
            String chatMessage = message.signedContent();

            // ===== 删除主人检查，所有哆啦B梦都会响应聊天 =====
            level.getEntitiesOfClass(DuolabEntity.class,
                            player.getBoundingBox().inflate(16.0D))
                    .forEach(duolab -> {
                        // 不再检查主人，直接处理聊天
                        duolab.chat.onPlayerChat(player, chatMessage);
                    });
        });
    }
}