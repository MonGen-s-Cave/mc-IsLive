package com.mongenscave.mcstreamlink.managers;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.data.MilestoneData;
import com.mongenscave.mcstreamlink.identifiers.MilestoneType;
import com.mongenscave.mcstreamlink.identifiers.PlatformType;
import com.mongenscave.mcstreamlink.identifiers.keys.ConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {
    private final McStreamLink plugin;
    private final Map<UUID, Map<UUID, BossBar>> activeBossBars;

    public BossBarManager(@NotNull McStreamLink plugin) {
        this.plugin = plugin;
        this.activeBossBars = new ConcurrentHashMap<>();
    }

    public void updateBossBars(@NotNull UUID playerUuid, @NotNull PlatformType platform, int currentValue, boolean isFollower) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player == null || !player.isOnline()) return;

        MilestoneType type = isFollower ? MilestoneType.FOLLOWER : MilestoneType.VIEWER;

        boolean enabled = isFollower
                ? ConfigKeys.MILESTONE_PROGRESS_FOLLOWER_ENABLED.getBoolean()
                : ConfigKeys.MILESTONE_PROGRESS_VIEWER_ENABLED.getBoolean();

        if (!enabled) {
            removeAllBossBars(playerUuid);
            return;
        }

        List<MilestoneData> milestones = plugin.getMilestoneManager()
                .getPlayerMilestonesByPlatform(playerUuid, platform)
                .stream()
                .filter(m -> m.getType() == type)
                .filter(m -> !m.isTriggered())
                .sorted(Comparator.comparingInt(MilestoneData::getValue))
                .toList();

        if (milestones.isEmpty()) {
            removeAllBossBars(playerUuid);
            return;
        }

        MilestoneData nextMilestone = milestones.getFirst();

        int target = nextMilestone.getValue();
        float progress = Math.min(1.0f, (float) currentValue / target);
        int percent = (int) (progress * 100);

        updateOrCreateBossBar(player, nextMilestone, currentValue, target, percent, progress, type);
    }

    private void updateOrCreateBossBar(
            @NotNull Player player,
            @NotNull MilestoneData milestone,
            int current,
            int target,
            int percent,
            float progress,
            @NotNull MilestoneType type
    ) {
        UUID playerUuid = player.getUniqueId();
        UUID milestoneId = milestone.getId();

        BossBar bossBar = activeBossBars
                .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(milestoneId, k -> createBossBar(type));

        String titleTemplate = type == MilestoneType.FOLLOWER
                ? ConfigKeys.MILESTONE_PROGRESS_FOLLOWER_TITLE.getString()
                : ConfigKeys.MILESTONE_PROGRESS_VIEWER_TITLE.getString();

        String formattedTitle = titleTemplate
                .replace("{current}", String.valueOf(current))
                .replace("{target}", String.valueOf(target))
                .replace("{percent}", String.valueOf(percent))
                .replace("{remaining}", String.valueOf(target - current));

        bossBar.setTitle(formattedTitle);
        bossBar.setProgress(progress);

        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        Map<UUID, BossBar> playerBars = activeBossBars.get(playerUuid);

        if (playerBars != null) {
            playerBars.entrySet().removeIf(entry -> {
                if (!entry.getKey().equals(milestoneId)) {
                    entry.getValue().removePlayer(player);
                    entry.getValue().removeAll();
                    return true;
                }
                return false;
            });
        }
    }

    @NotNull
    private BossBar createBossBar(@NotNull MilestoneType type) {
        String colorStr = type == MilestoneType.FOLLOWER
                ? ConfigKeys.MILESTONE_PROGRESS_FOLLOWER_COLOR.getString()
                : ConfigKeys.MILESTONE_PROGRESS_VIEWER_COLOR.getString();

        String styleStr = type == MilestoneType.FOLLOWER
                ? ConfigKeys.MILESTONE_PROGRESS_FOLLOWER_STYLE.getString()
                : ConfigKeys.MILESTONE_PROGRESS_VIEWER_STYLE.getString();

        BarColor color = BarColor.valueOf(colorStr);
        BarStyle style = BarStyle.valueOf(styleStr);

        return Bukkit.createBossBar("", color, style);
    }

    public void removeAllBossBars(@NotNull UUID playerUuid) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        Map<UUID, BossBar> playerBars = activeBossBars.remove(playerUuid);

        if (playerBars != null) {
            playerBars.values().forEach(bossBar -> {
                if (player != null) bossBar.removePlayer(player);
                bossBar.removeAll();
            });
        }
    }

    public void removeBossBar(@NotNull UUID playerUuid, @NotNull UUID milestoneId) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        Map<UUID, BossBar> playerBars = activeBossBars.get(playerUuid);

        if (playerBars != null) {
            BossBar bossBar = playerBars.remove(milestoneId);
            if (bossBar != null) {
                if (player != null) bossBar.removePlayer(player);
                bossBar.removeAll();
            }
        }
    }

    public void onMilestoneComplete(@NotNull UUID playerUuid, @NotNull UUID milestoneId) {
        removeBossBar(playerUuid, milestoneId);
    }

    public void onPlayerQuit(@NotNull UUID playerUuid) {
        removeAllBossBars(playerUuid);
    }

    public void removeAll() {
        activeBossBars.forEach((uuid, bars) -> bars.values().forEach(BossBar::removeAll));
        activeBossBars.clear();
    }
}