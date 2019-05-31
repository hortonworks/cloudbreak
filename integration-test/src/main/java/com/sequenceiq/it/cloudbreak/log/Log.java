package com.sequenceiq.it.cloudbreak.log;

import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.testng.Reporter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Log {
    private Log() {
    }

    public static void logJSON(Logger logger, String message, Object jsonObject) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, jsonObject);

        String msg = message + writer;
        if (logger != null) {
            logger.info(msg);
        }
        Reporter.log(msg);
    }

    public static void logJSON(String message, Object jsonObject) throws IOException {
        logJSON(null, message, jsonObject);
    }

    public static void log(String message, Object... args) {
        log(null, message, args);
    }

    public static void log(Logger logger, String message, Object... args) {
        String format = String.format(message, args);
        log(format);
        if (logger != null) {
            logger.info(format);
        }
    }

    public static void log(String message) {
        Reporter.log(message);
    }
}
