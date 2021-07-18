package com.sequenceiq.environment.experience;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ResponseReaderUtility {

    private ResponseReaderUtility() {
    }

    public static void logInputResponseContentIfPossible(Logger logger, Object toRead, String msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            logger.info(msg, mapper.writeValueAsString(toRead));
        } catch (Exception e) {
            logger.warn("Unable to process object into a valid JSON due to: " + e.getMessage(), e);
        }
    }

}
