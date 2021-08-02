package com.sequenceiq.environment.experience;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class ResponseReaderUtility {

    private ResponseReaderUtility() {
    }

    public static void logInputResponseContentIfPossible(Logger logger, Object toRead, String msg) {
        ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            logger.info(msg, mapper.writeValueAsString(toRead));
        } catch (Exception e) {
            logger.warn("Unable to process object into a valid JSON due to: " + e.getMessage(), e);
        }
    }

}
