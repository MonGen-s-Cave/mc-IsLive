package com.mongenscave.mcstreamlink.utils;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.identifiers.PlatformType;
import com.mongenscave.mcstreamlink.identifiers.keys.ConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class NotificationUtils {
    private final McStreamLink plugin;

    public NotificationUtils(@NotNull McStreamLink plugin) {
        this.plugin = plugin;
    }

    public void notifyLiveStart(@NotNull Player player, @NotNull PlatformType platform) {
        if (!ConfigKeys.NOTIFICATIONS_ENABLED.getBoolean()) return;

        if (ConfigKeys.NOTIFICATIONS_ACTIONBAR_ENABLED.getBoolean()) {
            String message = ConfigKeys.NOTIFICATIONS_ACTIONBAR_MESSAGE.getString();

            String formatted = message
                    .replace("{player}", player.getName())
                    .replace("{platform}", platform.getFormatted());

            plugin.getScheduler().runTask(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendActionBar(formatted);
                }
            });
        }

        if (ConfigKeys.NOTIFICATIONS_CHAT_ENABLED.getBoolean()) {
            String message = ConfigKeys.NOTIFICATIONS_CHAT_MESSAGE.getString();

            String formatted = message
                    .replace("{player}", player.getName())
                    .replace("{platform}", platform.getFormatted());

            plugin.getScheduler().runTask(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(formatted);
                }
            });
        }
    }
}
