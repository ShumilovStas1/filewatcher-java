package org.filewatcher.filewatcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class FilewatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilewatcherApplication.class, args);
	}

	@Bean(destroyMethod = "shutdownNow")
	ExecutorService fileEventPollExecutor(@Value("${poll.thread.count:1}") int pollThreadCount) {
		return Executors.newFixedThreadPool(pollThreadCount);
	}
}
