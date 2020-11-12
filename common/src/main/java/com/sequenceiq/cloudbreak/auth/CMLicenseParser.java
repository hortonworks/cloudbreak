package com.sequenceiq.cloudbreak.auth;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class CMLicenseParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMLicenseParser.class);

    public Optional<JsonCMLicense> parseLicense(String license) {
        Optional<JsonCMLicense> result = Optional.empty();
        if (isNotEmpty(license)) {
            try {
                String json = '{' + substringBeforeLast(substringAfter(license, "{"), "}") + '}';
                JsonCMLicense jsonCMLicense = JsonUtil.readValue(json, JsonCMLicense.class);
                result = Optional.of(jsonCMLicense);
            } catch (IOException e) {
                LOGGER.warn("Cannot parse CM license", e);
            }
        }
        return result;
    }
}
