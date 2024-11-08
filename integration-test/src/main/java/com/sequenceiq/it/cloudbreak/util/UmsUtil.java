package com.sequenceiq.it.cloudbreak.util;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class UmsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUtil.class);

    private UmsUtil() {
    }

    public static boolean isMockUms(String accessKey) {
        try {
            Crn.fromString(new String(Base64.getDecoder().decode(accessKey)));
            LOGGER.info("Test user is for a mocked ums");
            return true;
        } catch (Exception e) {
            LOGGER.info("Test user is ready to use against ums");
            return false;
        }
    }
}
