package com.mongenscave.mcstreamlink.identifiers.keys;

import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.config.Config;
import com.mongenscave.mcstreamlink.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),
    NO_PERMISSION("messages.no-permission"),

    INVALID_URL("messages.invalid-url"),
    NO_DATA("messages.no-data"),
    NOT_NEGATIVE("messages.not-negative"),
    UNKNOWN_MILESTONE_COMMAND("messages.unknown-milestone-command"),
    UNKNOWN_MILESTONE("messages.unknown-milestone"),

    SUCCESS_ADD("messages.success-add"),
    SUCCESS_REMOVE("messages.success-remove"),

    MILESTONE_ADDED("messages.milestone-add"),
    MILESTONE_REMOVED("messages.milestone-remove");

    private final String path;
    private static final Config config = McStreamLink.getInstance().getLanguage();

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(config.getString(path))
                .replace("%prefix%", MessageProcessor.process(config.getString("prefix")));
    }

    public List<String> getMessages() {
        return config.getStringList(path)
                .stream()
                .toList();
    }
}
