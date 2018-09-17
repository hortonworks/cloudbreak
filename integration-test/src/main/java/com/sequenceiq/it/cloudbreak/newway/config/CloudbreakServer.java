package com.sequenceiq.it.cloudbreak.newway.config;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakTest.SECOND_PASSWORD;
import static com.sequenceiq.it.cloudbreak.newway.CloudbreakTest.SECOND_USER;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

@Component
public class CloudbreakServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakServer.class);

    private static final String WARNING_TEXT = "Following variables must be set whether as environment variables or (test) application.yaml: "
            + "INTEGRATIONTEST_CLOUDBREAK_SERVER INTEGRATIONTEST_UAA_SERVER INTEGRATIONTEST_UAA_USER INTEGRATIONTEST_UAA_PASSWORD";

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.uaa.server}")
    private String uaaServer;

    @Value("${integrationtest.uaa.user}")
    private String defaultUaaUser;

    @Value("${integrationtest.uaa.password}")
    private String defaultUaaPassword;

    @Value("${integrationtest.uaa.secondUser:}")
    private String secondUaaUser;

    @Value("${integrationtest.uaa.secondPassword:}")
    private String secondUaaPassword;

    @Value("${integrationtest.cb.profile:}")
    private String profile;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() throws IOException {

        String userHome = System.getProperty("user.home");
        Path cbProfileLocation = Paths.get(userHome, ".cb", "config");

        String server = this.server;
        String uaaServer = this.uaaServer;
        String defaultUaaUser = this.defaultUaaUser;
        String defaultUaaPassword = this.defaultUaaPassword;

        if (Files.exists(cbProfileLocation)) {
            byte[] encoded = Files.readAllBytes(Paths.get(userHome, ".cb", "config"));
            String profileString = new String(encoded, Charset.defaultCharset());

            Yaml yaml = new Yaml();
            Map<String, Object> profiles = yaml.load(profileString);

            String usedProfile = "localhost";

            if (!StringUtils.isEmpty(profile)) {
                usedProfile = profile;
            }

            Map<String, String> prof = (Map<String, String>) profiles.get(usedProfile);

            if (prof == null) {
                LOGGER.warn("localhost in ~/.cb/config or "
                        + "integrationtest.cb.profile in application.yml or "
                        + "-Dintegrationtest.cb.profile should be added with exited profile");
            } else {
                if (StringUtils.isEmpty(server)) {
                    server = prof.get("server");
                    if ("localhost".equals(usedProfile) && !StringUtils.isEmpty(server)) {
                        uaaServer = "http://" + server + ":8089";
                    } else {
                        uaaServer = "https://" + server;
                    }
                    server = "https://" + server;
                }
                if (StringUtils.isEmpty(defaultUaaPassword)) {
                    defaultUaaPassword = prof.get("password");
                }
                if (StringUtils.isEmpty(defaultUaaUser)) {
                    defaultUaaUser = prof.get("username");
                }
            }
        } else {
            LOGGER.info("Could not find cb profile file at location {}, falling back to application.yml", cbProfileLocation);
        }

        checkNonEmpty(server);
        checkNonEmpty(cbRootContextPath);
        checkNonEmpty(uaaServer);
        checkNonEmpty(defaultUaaUser);
        checkNonEmpty(defaultUaaPassword);

        testParameter.put(CloudbreakTest.CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testParameter.put(CloudbreakTest.IDENTITY_URL, uaaServer);
        testParameter.put(CloudbreakTest.USER, defaultUaaUser);
        testParameter.put(CloudbreakTest.PASSWORD, defaultUaaPassword);

        if (!StringUtils.isEmpty(secondUaaUser)) {
            if (StringUtils.isEmpty(secondUaaPassword)) {
                throw new IllegalStateException("Second user password must not be empty!");
            }
            testParameter.put(SECOND_USER, secondUaaUser);
            testParameter.put(SECOND_PASSWORD, secondUaaPassword);
        }
    }

    private void checkNonEmpty(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(WARNING_TEXT);
        }
    }
}
