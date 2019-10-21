package com.sequenceiq.it.cloudbreak.config;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.EnvironmentTest;

@Component
public class EnvironmentServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    private static final int DEFAULT_ENVIRONMENT_PORT = 8088;

    @Value("${integrationtest.environment.server}")
    private String server;

    @Value("${environment.url:localhost:" + DEFAULT_ENVIRONMENT_PORT + "}")
    private String environmentUrl;

    @Value("${environment.server.contextPath:/environmentservice}")
    private String environmentRootContextPath;

    @Value("${integrationtest.user.accesskey:}")
    private String accessKey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretKey;

    @Inject
    private TestParameter testParameter;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    @PostConstruct
    private void init() throws IOException {
        configureFromCliProfile();

        checkNonEmpty("integrationtest.environment.server", server);
        checkNonEmpty("environment.server.contextPath", environmentRootContextPath);
        checkNonEmpty("environment.url", environmentUrl);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.privatekey", secretKey);

        testParameter.put(EnvironmentTest.ENVIRONMENT_SERVER_ROOT, server + environmentRootContextPath);
        testParameter.put(EnvironmentTest.ENVIRONMENT_INTERNAL_SERVER_ROOT, "http://" + environmentUrl + environmentRootContextPath);
        testParameter.put(EnvironmentTest.ACCESS_KEY, accessKey);
        testParameter.put(EnvironmentTest.SECRET_KEY, secretKey);
    }

    private void configureFromCliProfile() throws IOException {
        Map<String, String> profiles = cliProfileReaderService.read();
        if (profiles == null) {
            LOGGER.warn("localhost in ~/.dp/config or "
                    + "integrationtest.dp.profile in application.yml or "
                    + "-Dintegrationtest.dp.profile should be added with exited profile");
            return;
        }

        server = serverUtil.calculateServerAddressFromProfile(server, profiles);
        accessKey = serverUtil.calculateApiKeyFromProfile(accessKey, profiles);
        secretKey = serverUtil.calculatePrivateKeyFromProfile(secretKey, profiles);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
