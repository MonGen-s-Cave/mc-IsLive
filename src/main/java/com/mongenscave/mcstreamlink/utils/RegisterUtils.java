package com.mongenscave.mcstreamlink.utils;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.annotations.Media;
import com.mongenscave.mcstreamlink.annotations.Milestone;
import com.mongenscave.mcstreamlink.annotations.MilestoneCommand;
import com.mongenscave.mcstreamlink.commands.CommandLive;
import com.mongenscave.mcstreamlink.data.MilestoneData;
import com.mongenscave.mcstreamlink.exception.CommandExceptionHandler;
import com.mongenscave.mcstreamlink.identifiers.keys.ConfigKeys;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.orphan.Orphans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class RegisterUtils {
    private static final McStreamLink plugin = McStreamLink.getInstance();

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin)
                .exceptionHandler(new CommandExceptionHandler())
                .suggestionProviders(providers -> providers.addProviderForAnnotation(Media.class, media -> context -> {
                    List<String> playerNames = Collections.synchronizedList(new ArrayList<>());

                    try {
                        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                            if (player.getName() != null && !plugin.getMediaDataManager().getAllPlayerData(player.getUniqueId()).isEmpty()) playerNames.add(player.getName());
                        }
                    } catch (Exception exception) {
                        LoggerUtils.error(exception.getMessage());
                    }

                    return playerNames;
                }))
                .suggestionProviders(providers -> providers.addProviderForAnnotation(Milestone.class, milestone -> context -> {
                    List<String> milestoneIds = Collections.synchronizedList(new ArrayList<>());

                    try {
                        var sender = context.actor();

                        if (sender instanceof Player player) {
                            List<MilestoneData> milestones = plugin.getMilestoneManager().getPlayerMilestones(player.getUniqueId());

                            for (MilestoneData milestoneData : milestones) {
                                milestoneIds.add(milestoneData.getId().toString());
                            }
                        }
                    } catch (Exception exception) {
                        LoggerUtils.error(exception.getMessage());
                    }

                    return milestoneIds;
                }))
                .suggestionProviders(providers -> providers.addProviderForAnnotation(MilestoneCommand.class, milestoneCommand -> context -> {
                    List<String> commandIds = Collections.synchronizedList(new ArrayList<>());

                    try {
                        var section = plugin.getConfiguration().getSection("milestone-commands");
                        if (section != null) commandIds.addAll(section.getRoutesAsStrings(false));
                    } catch (Exception exception) {
                        LoggerUtils.error(exception.getMessage());
                    }

                    return commandIds;
                }))
                .build();

        lamp.register(Orphans.path(ConfigKeys.ALIASES.getList().toArray(String[]::new)).handler(new CommandLive(plugin.getMediaDataManager(), plugin.getMilestoneManager())));
    }
}