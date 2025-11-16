package com.mongenscave.mcstreamlink.utils;

import com.mongenscave.mcstreamlink.McStreamLink;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("McStreamLink");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String main = "\u001B[38;2;189;62;75m";
        String reset = "\u001B[0m";
        String software = McStreamLink.getInstance().getServer().getName();
        String version = McStreamLink.getInstance().getServer().getVersion();

        info("");
        info("{}   _____ _                            _      _       _      {}", main, reset);
        info("{}  / ____| |                          | |    (_)     | |     {}", main, reset);
        info("{} | (___ | |_ _ __ ___  __ _ _ __ ___ | |     _ _ __ | | __  {}", main, reset);
        info("{}  \\\\___ \\\\| __| '__/ _ \\\\/ _` | '_ ` _ \\\\| |    | | '_ \\\\| |/ /  {}", main, reset);
        info("{}  ____) | |_| | |  __/ (_| | | | | | | |____| | | | |   <   {}", main, reset);
        info("{} |_____/ \\\\__|_|  \\\\___|\\\\__,_|_| |_| |_|______|_|_| |_|_|\\\\_\\\\  {}", main, reset);
        info("");
        info("{}  The plugin successfully started.{}", main, reset);
        info("{}  mc-StreamLink {} {}{}", main, software, version, reset);
        info("{}  Discord @ dc.mongenscave.com{}", main, reset);
        info("");
    }
}