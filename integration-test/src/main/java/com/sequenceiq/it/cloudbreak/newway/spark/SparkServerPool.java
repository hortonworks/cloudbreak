package com.sequenceiq.it.cloudbreak.newway.spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkServerPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServerPool.class);

    private static final long TIMEOUT = 15 * 60 * 1000;

    private final Map<SparkServer, Boolean> servers = new HashMap<>();

    public SparkServer pop() {
        synchronized (servers) {
            if (servers.entrySet().stream().noneMatch(Entry::getValue)) {
                try {
                    LOGGER.info("Waiting for spark server");
                    servers.wait(TIMEOUT);
                    LOGGER.debug("Waking up to grab a spark server");
                } catch (InterruptedException ignored) {
                }
            }
            Optional<Entry<SparkServer, Boolean>> found = servers.entrySet().stream().filter(Entry::getValue).findFirst();
            Entry<SparkServer, Boolean> entry = found.orElseThrow();
            entry.setValue(Boolean.FALSE);
            return entry.getKey();
        }
    }

    public void put(SparkServer sparkServer) {
        synchronized (servers) {
            servers.put(sparkServer, Boolean.TRUE);
            servers.notify();
        }
    }

    @PreDestroy
    public void autoShutdown() {
        LOGGER.info("Invoking PreDestroy for Spark Pool bean");
        servers.keySet().forEach(SparkServer::stop);
    }
}
