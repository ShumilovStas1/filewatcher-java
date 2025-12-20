package org.filewatcher.filewatcher.conf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "fw")
@Validated
public record AppProperties(
                            @NotNull
                            WatcherType watcherType,
                            @NotEmpty
                            List<Path> watchDirs,
                            @Valid
                            SpringFileWatcherProperties springWatcher,
                            @Valid
                            NioFileWatcherProperties nioWatcher,
                            @Valid
                            ApacheCommonsFileWatcherProperties apacheCommonsWatcher) {

    public record SpringFileWatcherProperties(boolean daemon,
                                              Duration pollInterval,
                                              Duration quietPeriod) {

    }

    public record NioFileWatcherProperties(@Min(1) int pollThreadCount) {

    }

    public record ApacheCommonsFileWatcherProperties(Duration interval) {

    }
}


