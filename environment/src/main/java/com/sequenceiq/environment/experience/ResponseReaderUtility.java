package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.common.json.JsonUtil.writeValueAsString;

import org.slf4j.Logger;

public final class ResponseReaderUtility {

    private ResponseReaderUtility() {
    }

    public static void logInputResponseContentIfPossible(Logger logger, Object toRead, String msg) {
        try {
            logger.info(msg, writeValueAsString(toRead));
        } catch (Exception e) {
            logger.warn("Unable to process object into a valid JSON due to: {}", e.getMessage(), e);
        }
    }

}
