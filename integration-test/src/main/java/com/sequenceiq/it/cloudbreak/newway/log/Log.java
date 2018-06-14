package com.sequenceiq.it.cloudbreak.newway.log;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Reporter;

import java.io.IOException;
import java.io.StringWriter;

public class Log {
    private Log() {
    }

    public static void logJSON(String message, Object jsonObject) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, jsonObject);

        Reporter.log(message + writer);
    }

    public static void log(String message) {
        Reporter.log(message);
    }
}
