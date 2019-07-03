package com.sequenceiq.periscope.aspects;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogging.class);

    public <T> T logging(Supplier<T> callback, String requestName) {
        long start = System.currentTimeMillis();
        T o = callback.get();
        LOGGER.debug("Ambari '{}' finished in {} ms", requestName, System.currentTimeMillis() - start);
        return o;
    }
}
