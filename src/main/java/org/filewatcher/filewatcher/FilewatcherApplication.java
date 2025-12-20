package org.filewatcher.filewatcher;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.filewatcher.filewatcher.conf.AppProperties;
import org.filewatcher.filewatcher.conf.WatcherType;
import org.filewatcher.filewatcher.service.ApacheCommonsWatcherListener;
import org.filewatcher.filewatcher.service.DirsToWatch;
import org.filewatcher.filewatcher.service.SpringWatcherFileChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FilewatcherApplication {
	private static final Logger log = LoggerFactory.getLogger(FilewatcherApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(FilewatcherApplication.class, args);
		WatcherType watcherType = ctx.getBean(AppProperties.class).watcherType();
		log.info("Configured watcher type: {}", watcherType);
	}

	@Bean(destroyMethod = "shutdownNow")
	ExecutorService fileEventPollExecutor(@Value("${poll.thread.count:1}") int pollThreadCount) {
		return Executors.newFixedThreadPool(pollThreadCount);
	}

	@Bean(destroyMethod = "stop")
	@ConditionalOnProperty(value = "fw.watcher-type", havingValue = "spring")
	FileSystemWatcher springFileSystemWatcher(AppProperties properties, DirsToWatch dirsToWatch, SpringWatcherFileChangeListener listener) {
		var fileSystemWatcher = new FileSystemWatcher(
				properties.springWatcher().daemon(),
				properties.springWatcher().pollInterval(),
				properties.springWatcher().quietPeriod()
		);
		fileSystemWatcher.addSourceDirectories(dirsToWatch.getAbsolutePaths().stream().map(Path::toFile).toList());
		fileSystemWatcher.addListener(listener);
		fileSystemWatcher.start();
		return fileSystemWatcher;
	}

	@Bean(destroyMethod = "stop")
	@ConditionalOnProperty(value = "fw.watcher-type", havingValue = "apache_commons")
	FileAlterationMonitor apacheCommonsWatcher(AppProperties properties, DirsToWatch dirsToWatch,
											   ApacheCommonsWatcherListener listener) throws Exception {
		FileAlterationMonitor monitor = new FileAlterationMonitor(properties.apacheCommonsWatcher().interval().toMillis());
		for (Path absolutePath : dirsToWatch.getAbsolutePaths()) {
			FileAlterationObserver observer = new FileAlterationObserver(absolutePath.toFile());
			observer.addListener(listener);
			monitor.addObserver(observer);
		}
		monitor.start();
		return monitor;
	}
}
