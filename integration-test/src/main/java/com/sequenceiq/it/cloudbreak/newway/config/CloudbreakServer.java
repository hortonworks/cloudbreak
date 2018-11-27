package com.sequenceiq.it.cloudbreak.newway.config;

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

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.caas.token:}")
    private String refreshToken;

    @Value("${integrationtest.caas.protocol:}")
    private String caasProtocol;

    @Value("${integrationtest.caas.address:}")
    private String caasAddress;

    @Value("${integrationtest.dp.profile:}")
    private String profile;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() throws IOException {

        String userHome = System.getProperty("user.home");
        Path cbProfileLocation = Paths.get(userHome, ".dp", "config");

        if (Files.exists(cbProfileLocation)) {
            byte[] encoded = Files.readAllBytes(Paths.get(userHome, ".dp", "config"));
            String profileString = new String(encoded, Charset.defaultCharset());

            Yaml yaml = new Yaml();
            Map<String, Object> profiles = yaml.load(profileString);

            String usedProfile = "localhost";

            if (!StringUtils.isEmpty(profile)) {
                usedProfile = profile;
            }

            Map<String, String> prof = (Map<String, String>) profiles.get(usedProfile);

            if (prof == null) {
                LOGGER.warn("localhost in ~/.dp/config or "
                        + "integrationtest.dp.profile in application.yml or "
                        + "-Dintegrationtest.dp.profile should be added with exited profile");
            } else {
                if (StringUtils.isEmpty(server)) {
                    if (prof.get("server").contains("http")) {
                        server = prof.get("server");
                    } else {
                        server = "https://" + prof.get("server");
                    }
                }
                if (StringUtils.isEmpty(refreshToken)) {
                    refreshToken = prof.get("refreshtoken");
                }
            }
        } else {
            LOGGER.info("Could not find cb profile file at location {}, falling back to application.yml", cbProfileLocation);
        }

        checkNonEmpty("integrationtest.cloudbreak.server", server);

        String[] cloudbreakServerSplit = server.split("://");
        if (StringUtils.isEmpty(caasProtocol) && cloudbreakServerSplit.length > 0) {
            caasProtocol = cloudbreakServerSplit[0];
        }
        if (StringUtils.isEmpty(caasAddress) && cloudbreakServerSplit.length > 1) {
            caasAddress = cloudbreakServerSplit[1];
        }

        checkNonEmpty("server.contextPath", cbRootContextPath);
        checkNonEmpty("integrationtest.caas.token", refreshToken);
        checkNonEmpty("integrationtest.caas.protocol", caasProtocol);
        checkNonEmpty("integrationtest.caas.address", caasAddress);

        testParameter.put(CloudbreakTest.CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testParameter.put(CloudbreakTest.CAAS_PROTOCOL, caasProtocol);
        testParameter.put(CloudbreakTest.CAAS_ADDRESS, caasAddress);
        testParameter.put(CloudbreakTest.REFRESH_TOKEN, refreshToken);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
