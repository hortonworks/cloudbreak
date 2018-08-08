package com.sequenceiq.periscope.aspects;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AmbariRequestLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRequestLogging.class);

    public <T> T logging(Supplier<T> callback, String requestName) {
        long start = System.currentTimeMillis();
        T o = callback.get();
        LOGGER.info("Ambari '{}' finished in {} ms", System.currentTimeMillis() - start, requestName);
        return o;
    }
}
