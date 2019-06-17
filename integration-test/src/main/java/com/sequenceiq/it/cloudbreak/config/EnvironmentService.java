package com.sequenceiq.it.cloudbreak.config;

import static com.sequenceiq.it.cloudbreak.EnvironmentServiceClient.ENVIRONMENTSERVICE_SERVER_ROOT;

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

@Component
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    private static final int ENVIRONMENT_PORT = 8088;

    @Value("${integrationtest.environment.server}")
    private String server;

    @Value("${freeipa.server.contextPath:/environmentservice}")
    private String environmentRootContextPath;

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

        testParameter.put(ENVIRONMENTSERVICE_SERVER_ROOT, server + environmentRootContextPath);
    }

    private void configureFromCliProfile() throws IOException {
        Map<String, String> profiles = cliProfileReaderService.read();
        if (profiles == null) {
            LOGGER.warn("localhost in ~/.dp/config or "
                    + "integrationtest.environment.server application.yml or "
                    + "-Dintegrationtest.environment.server should be added with exited profile");
            return;
        }

        server = serverUtil.calculateServerAddressFromProfile(server, profiles, ENVIRONMENT_PORT);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
