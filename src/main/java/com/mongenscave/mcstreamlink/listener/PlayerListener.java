package com.mongenscave.mcstreamlink.listener;

import com.mongenscave.mcstreamlink.McStreamLink;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        McStreamLink.getInstance().getBossBarManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
