package com.sequenceiq.it.cloudbreak.config.server;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.RedBeamsTest;
import com.sequenceiq.it.util.TestParameter;

@Component
public class RedbeamsServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable(s) must be set as environment variables or in (test) application.yaml: %s";

    @Value("${integrationtest.redbeams.server}")
    private String server;

    @Value("${redbeams.server.contextPath:/redbeams}")
    private String rootContextPath;

    @Value("${integrationtest.user.accesskey:}")
    private String accessKey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretKey;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.user.name:}")
    private String userName;

    @Inject
    private TestParameter testParameter;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    @PostConstruct
    private void init() throws IOException {
        configureFromCliProfile();

        checkNonEmpty("integrationtest.redbeams.server", server);
        checkNonEmpty("redbeams.server.contextPath", rootContextPath);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.privatekey", secretKey);

        testParameter.put(RedBeamsTest.REDBEAMS_SERVER_ROOT, server + rootContextPath);
        testParameter.put(RedBeamsTest.ACCESS_KEY, accessKey);
        testParameter.put(RedBeamsTest.SECRET_KEY, secretKey);
        testParameter.put(CloudbreakTest.USER_CRN, userCrn);
        testParameter.put(CloudbreakTest.USER_NAME, userName);
    }

    private void configureFromCliProfile() throws IOException {
        Map<String, String> profile = cliProfileReaderService.read();
        if (profile == null) {
            LOGGER.warn("Failed to find specified profile (default \"localhost\") in ~/.dp/config. Set the profile "
                    + "name using integrationtest.dp.profile Spring Boot configuration property (application.yml), or "
                    + "-Dintegrationtest.dp.profile system property.");
            return;
        }

        server = serverUtil.calculateServerAddressFromProfile(server, profile);
        accessKey = serverUtil.calculateApiKeyFromProfile(accessKey, profile);
        secretKey = serverUtil.calculatePrivateKeyFromProfile(secretKey, profile);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
