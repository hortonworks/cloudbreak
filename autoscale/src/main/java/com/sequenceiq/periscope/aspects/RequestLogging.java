package com.sequenceiq.periscope.aspects;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestLogging {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogging.class);

    public <T> T logResponseTime(Supplier<T> callback, String requestName) {
        long start = System.currentTimeMillis();
        T response;
        try {
            response = callback.get();
            LOGGER.info("Request '{}' finished in '{}' ms.", requestName, System.currentTimeMillis() - start);
        } catch (Exception ex) {
            LOGGER.error("Request '{}', Exception: ", requestName, ex);
            throw ex;
        }
        return response;
    }
}
