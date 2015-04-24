package com.sequenceiq.cloudbreak.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class MDCBuilderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MDCBuilderTest.class);

    private ThreadPoolTaskExecutor executor;

    @Before
    public void setUp() throws Exception {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setQueueCapacity(2);
        executor.setKeepAliveSeconds(0);
        executor.setThreadNamePrefix("resourceBuilderExecutor-");
        executor.initialize();
    }

    @Test
    public void testExec() throws Exception {
        LOGGER.debug(" Active Count: {}", executor.getThreadGroup());
        List<Future> futures = new ArrayList<>();
        MDC.put("owner", "me");
        for (int i = 0; i < 10; i++) {
            String name = "th-" + i;
            MDC.put("owner", name);
            final Map ctx = MDC.getCopyOfContextMap();
            futures.add(executor.submit(new Thread(name) {
                @Override public void run() {
                    MDC.setContextMap(ctx);
                    LOGGER.info(getName());
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error(getName());
                    }
                }
            }));
        }

        for (Future f : futures) {
            f.get();
        }
    }

    @Test
    public void testMyExec() throws Exception {

    }
}