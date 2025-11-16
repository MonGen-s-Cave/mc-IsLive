package com.mongenscave.mcislive;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.mongenscave.mcislive.clients.TwitchApiClient;
import com.mongenscave.mcislive.clients.YoutubeApiClient;
import com.mongenscave.mcislive.config.Config;
import com.mongenscave.mcislive.hooks.plugins.PlaceholderAPI;
import com.mongenscave.mcislive.identifiers.keys.ConfigKeys;
import com.mongenscave.mcislive.managers.MediaDataManager;
import com.mongenscave.mcislive.managers.MilestoneManager;
import com.mongenscave.mcislive.service.LiveCheckService;
import com.mongenscave.mcislive.utils.NotificationUtils;
import com.mongenscave.mcislive.utils.LoggerUtils;
import com.mongenscave.mcislive.utils.RegisterUtils;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;

public final class McIsLive extends ZapperJavaPlugin {
    @Getter private static McIsLive instance;
    @Getter private TaskScheduler scheduler;
    @Getter private Config language;
    @Getter private MediaDataManager mediaDataManager;
    @Getter private YoutubeApiClient youtubeClient;
    @Getter private TwitchApiClient twitchClient;
    @Getter private LiveCheckService liveCheckService;
    @Getter private MilestoneManager milestoneManager;
    private Config config;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeComponents();

        mediaDataManager = new MediaDataManager(this);
        milestoneManager = new MilestoneManager(this);

        youtubeClient = new YoutubeApiClient(this, ConfigKeys.API_YOUTUBE_API_KEY.getString());
        twitchClient = new TwitchApiClient(this, ConfigKeys.API_TWITCH_CLIENT_ID.getString(), ConfigKeys.API_TWITCH_CLIENT_SECRET.getString());

        NotificationUtils notificationService = new NotificationUtils(this);
        liveCheckService = new LiveCheckService(this, mediaDataManager, youtubeClient, twitchClient, notificationService);

        liveCheckService.start();

        PlaceholderAPI.registerHook(mediaDataManager);
        RegisterUtils.registerCommands();

        LoggerUtils.printStartup();
    }

    @Override
    public void onDisable() {
        if (liveCheckService != null) liveCheckService.stop();
        if (scheduler != null) scheduler.cancelTasks();
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }
}