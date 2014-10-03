package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentValidator implements InitializingBean {

    private static final String URL_PATTERN = "^(?:(?:https?):\\/\\/)(?:\\S+(?::\\S*)?@)?"
            + "(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{"
            + "1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:"
            + "\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|"
            + "2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?"
            + ":[a-z\\x{00a1}\\-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}\\-\\x{ffff}0-9]+)(?:\\.(?"
            + ":[a-z\\x{00a1}\\-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}\\-\\x{ffff}0-9]+)*(?:\\.("
            + "?:[a-z\\x{00a1}\\-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?$";

    @Value("${cb.host.addr}")
    private String hostAddress;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!hostAddress.matches(URL_PATTERN)) {
            throw new IllegalArgumentException(
                    "Host address is not a valid URL, it should include the scheme, the address and optionally a port. "
                            + "It shouldn't be an internal address either." + hostAddress);
        }
    }

}
