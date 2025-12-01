package com.mongenscave.mcstreamlink.hooks.plugins;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.identifiers.PlatformType;
import com.mongenscave.mcstreamlink.identifiers.keys.ConfigKeys;
import com.mongenscave.mcstreamlink.managers.MediaDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class PlaceholderAPI {
    public static boolean isRegistered = false;
    private static MediaDataManager dataManager;

    public static void registerHook(@NotNull MediaDataManager manager) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            dataManager = manager;
            new PlaceholderIntegration().register();
            isRegistered = true;
        }
    }

    private static class PlaceholderIntegration extends PlaceholderExpansion {
        @Override
        public @NotNull String getIdentifier() {
            return "mcstreamlink";
        }

        @Override
        public @NotNull String getAuthor() {
            return "coma112";
        }

        @Override
        public @NotNull String getVersion() {
            return McStreamLink.getInstance().getDescription().getVersion();
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        @Override
        public boolean persist() {
            return true;
        }

        @NotNull
        @Contract(pure = true)
        @Override
        public String onPlaceholderRequest(@NotNull Player player, @NotNull String params) {
            if (dataManager == null) return "---";

            switch (params.toLowerCase()) {
                case "youtube" -> {
                    return dataManager.isLive(player.getUniqueId(), PlatformType.YOUTUBE) ? ConfigKeys.PLACEHOLDER_LIVE.getString() : ConfigKeys.PLACEHOLDER_OFFLINE.getString();
                }

                case "twitch" -> {
                    return dataManager.isLive(player.getUniqueId(), PlatformType.TWITCH) ? ConfigKeys.PLACEHOLDER_LIVE.getString() : ConfigKeys.PLACEHOLDER_OFFLINE.getString();
                }

                case "any" -> {
                    return dataManager.isLiveOnAnyPlatform(player.getUniqueId()) ? ConfigKeys.PLACEHOLDER_LIVE.getString() : ConfigKeys.PLACEHOLDER_OFFLINE.getString();
                }
            }

            return "";
        }
    }
}