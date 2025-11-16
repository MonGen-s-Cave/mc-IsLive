package com.mongenscave.mcstreamlink.commands;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.annotations.Media;
import com.mongenscave.mcstreamlink.annotations.Milestone;
import com.mongenscave.mcstreamlink.annotations.MilestoneCommand;
import com.mongenscave.mcstreamlink.identifiers.MilestoneType;
import com.mongenscave.mcstreamlink.identifiers.PlatformType;
import com.mongenscave.mcstreamlink.identifiers.keys.MessageKeys;
import com.mongenscave.mcstreamlink.managers.MediaDataManager;
import com.mongenscave.mcstreamlink.managers.MilestoneManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.UUID;

public class CommandLive implements OrphanCommand {
    private static final McStreamLink plugin = McStreamLink.getInstance();
    private final MediaDataManager dataManager;
    private final MilestoneManager milestoneManager;

    public CommandLive(@NotNull MediaDataManager dataManager, @NotNull MilestoneManager milestoneManager) {
        this.dataManager = dataManager;
        this.milestoneManager = milestoneManager;
    }

    @Subcommand("reload")
    @CommandPermission("mcislive.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        dataManager.reload();
        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @Subcommand("media add")
    @CommandPermission("mcislive.addmedia")
    public void addMedia(@NotNull CommandSender sender, @NotNull Player target, @NotNull PlatformType platform, @NotNull String channelUrl) {
        if (!isValidUrl(channelUrl, platform)) {
            sender.sendMessage(MessageKeys.INVALID_URL.getMessage());
            return;
        }

        plugin.getScheduler().runTaskAsynchronously(() -> {
            dataManager.addOrUpdateMedia(target.getUniqueId(), platform, channelUrl);
            plugin.getScheduler().runTask(() -> sender.sendMessage(MessageKeys.SUCCESS_ADD.getMessage()));
        });
    }

    @Subcommand("media remove")
    @CommandPermission("mcislive.removemedia")
    public void removeMedia(@NotNull CommandSender sender, @NotNull @Media OfflinePlayer target, @NotNull PlatformType platform) {
        plugin.getScheduler().runTaskAsynchronously(() -> {
            boolean removed = dataManager.removeMedia(target.getUniqueId(), platform);

            plugin.getScheduler().runTask(() -> {
                if (removed) sender.sendMessage(MessageKeys.SUCCESS_REMOVE.getMessage());
                else sender.sendMessage(MessageKeys.NO_DATA.getMessage());
            });
        });
    }

    @Subcommand("milestone add")
    @CommandPermission("mcislive.milestone.add")
    public void milestoneAdd(@NotNull Player player, @NotNull PlatformType platform, @NotNull MilestoneType type, int value, @NotNull @MilestoneCommand String commandId) {
        if (value <= 0) {
            player.sendMessage(MessageKeys.NOT_NEGATIVE.getMessage());
            return;
        }

        if (plugin.getConfiguration().get("milestone-commands." + commandId) == null) {
            player.sendMessage(MessageKeys.UNKNOWN_MILESTONE_COMMAND.getMessage());
            return;
        }

        plugin.getScheduler().runTaskAsynchronously(() -> {
            milestoneManager.addMilestone(player.getUniqueId(), platform, type, value, commandId);
            plugin.getScheduler().runTask(() -> player.sendMessage(MessageKeys.SUCCESS_ADD.getMessage()));
        });
    }

    @Subcommand("milestone remove")
    @CommandPermission("mcislive.milestone.remove")
    public void milestoneRemove(@NotNull Player player, @NotNull @Milestone String milestoneIdStr) {
        UUID milestoneId;
        try {
            milestoneId = UUID.fromString(milestoneIdStr);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(MessageKeys.UNKNOWN_MILESTONE.getMessage());
            return;
        }

        plugin.getScheduler().runTaskAsynchronously(() -> {
            boolean removed = milestoneManager.removeMilestone(milestoneId);

            plugin.getScheduler().runTask(() -> {
                if (removed) player.sendMessage(MessageKeys.SUCCESS_REMOVE.getMessage());
                else player.sendMessage(MessageKeys.UNKNOWN_MILESTONE.getMessage());
            });
        });
    }

    private boolean isValidUrl(@NotNull String url, @NotNull PlatformType platform) {
        return switch (platform) {
            case YOUTUBE -> url.contains("youtube.com") || url.contains("youtu.be");
            case TWITCH -> url.contains("twitch.tv");
        };
    }
}