package com.sequenceiq.mock.legacy.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.Json;

public class HostNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostNameUtil.class);

    private static final String MOCKRESPONSE = "/mockresponse/";

    private HostNameUtil() {

    }

    public static String generateHostNameByIp(String address) {
        return "host-" + address.replace(".", "-") + ".example.com";
    }

    public static String responseFromJsonFile(String path) {
        try (InputStream inputStream = HostNameUtil.class.getResourceAsStream(MOCKRESPONSE + path)) {
            ObjectMapper mapper = new ObjectMapper();
            inputStream.mark(Integer.MAX_VALUE);
            Map map = mapper.readValue(inputStream, Map.class);
            return new Json(map).getValue();
        } catch (IOException e) {
            LOGGER.error("can't read file from path", e);
            return "";
        }
    }
}
