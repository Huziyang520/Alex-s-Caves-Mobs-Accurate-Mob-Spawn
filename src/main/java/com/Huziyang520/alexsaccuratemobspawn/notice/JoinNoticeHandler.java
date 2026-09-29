package com.Huziyang520.alexsaccuratemobspawn.notice;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import com.Huziyang520.alexsaccuratemobspawn.config.MobRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Chat notice shown to a player when they join a world.
 *
 * <p>Sent every time a player joins, and can be turned off with {@code showChatNotice} in
 * {@code common.toml}. Uses a translatable component with a fallback so that a client without this
 * mod still sees readable text instead of a raw translation key.</p>
 */
@Mod.EventBusSubscriber(modid = alexsaccuratemobspawn.MOD_ID)
public final class JoinNoticeHandler {

    /** Translation key; see assets/alexsaccuratemobspawn/lang/. */
    public static final String KEY_NOTICE = "message.alexsaccuratemobspawn.config_moved";

    /**
     * Used when the receiving client has no translation for the key (mod not installed on that
     * client, or an unsupported language). Must stay in sync with lang/zh_cn.json.
     */
    private static final String FALLBACK =
            "Alex Accurate Mob Spawn：本模组的配置文件位于 config\\alexsaccuratemobspawn 文件夹内，"
                    + "你可以仿照其中文件的格式自行编写配置文件，控制任意生物的生成概率与生成倍率。"
                    + "若你先前安装过本模组，原 config\\alexscavessaccuratemobspawn-common.toml 的内容已自动转移到 "
                    + "config\\alexsaccuratemobspawn\\alexscavessaccuratemobspawnmultiplier.toml，旧文件已删除。"
                    + "该提示可在配置文件中关闭。";

    private JoinNoticeHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!MobRules.showChatNotice()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(KEY_NOTICE, FALLBACK)
                .withStyle(ChatFormatting.YELLOW));
        alexsaccuratemobspawn.LOGGER.info("Sent config notice to {}", player.getGameProfile().getName());
    }
}
