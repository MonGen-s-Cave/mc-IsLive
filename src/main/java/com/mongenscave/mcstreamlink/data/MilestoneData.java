package com.mongenscave.mcstreamlink.data;

import com.mongenscave.mcstreamlink.identifiers.MilestoneType;
import com.mongenscave.mcstreamlink.identifiers.PlatformType;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
@AllArgsConstructor
public class MilestoneData {
    private UUID id;
    private UUID playerUuid;
    private PlatformType platform;
    private MilestoneType type;
    private int value;
    private String commandId;
    private boolean triggered;

    public MilestoneData(@NotNull UUID playerUuid, @NotNull PlatformType platform, @NotNull MilestoneType type, int value, @NotNull String commandId) {
        this.id = UUID.randomUUID();
        this.playerUuid = playerUuid;
        this.platform = platform;
        this.type = type;
        this.value = value;
        this.commandId = commandId;
        this.triggered = false;
    }
}
