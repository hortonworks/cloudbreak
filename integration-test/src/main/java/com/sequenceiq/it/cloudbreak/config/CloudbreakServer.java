package com.sequenceiq.it.cloudbreak.config;

import static org.apache.commons.net.util.Base64.decodeBase64;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.TestParameter;

@Component
public class CloudbreakServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.dp.profile:}")
    private String profile;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() throws IOException {

        configureFromCliProfile();

        checkNonEmpty("integrationtest.cloudbreak.server", server);
        checkNonEmpty("server.contextPath", cbRootContextPath);
        checkNonEmpty("integrationtest.user.crn", userCrn);

        testParameter.put(CloudbreakTest.CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testParameter.put(CloudbreakTest.USER_CRN, userCrn);
    }

    private void enforceHttpForCbAddress(String serverRaw) {
        server = "http://" + getDomainFromUrl(serverRaw) + ":9091";
    }

    private void configureFromCliProfile() throws IOException {
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
                calculateServerAddressFromProfile(prof);
                calculateCrnsFromProfile(prof);
            }
        } else {
            LOGGER.info("Could not find cb profile file at location {}, falling back to application.yml", cbProfileLocation);
        }
    }

    private void calculateServerAddressFromProfile(Map<String, String> prof) {
        if (StringUtils.isEmpty(server)) {
            server = prof.get("server");
            enforceHttpForCbAddress(server);
        }
    }

    private String getDomainFromUrl(String url) {
        if ("localhost".equals(url)) {
            return url;
        }
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid configuration value in Profile for server " + url, e);
        }
    }

    private void calculateCrnsFromProfile(Map<String, String> prof) {
        if (StringUtils.isEmpty(userCrn)) {
            userCrn = new String(decodeBase64(prof.get("apikeyid")));
        }
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
