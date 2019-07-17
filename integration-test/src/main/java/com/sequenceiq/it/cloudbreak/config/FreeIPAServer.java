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
import com.sequenceiq.it.cloudbreak.FreeIPATest;

@Component
public class FreeIPAServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    @Value("${integrationtest.freeipa.server}")
    private String server;

    @Value("${freeipa.server.contextPath:/freeipa}")
    private String freeIpaRootContextPath;

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

        checkNonEmpty("integrationtest.freeipa.server", server);
        checkNonEmpty("freeipa.server.contextPath", freeIpaRootContextPath);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.privatekey", secretKey);

        testParameter.put(FreeIPATest.FREEIPA_SERVER_ROOT, server + freeIpaRootContextPath);
        testParameter.put(FreeIPATest.ACCESS_KEY, accessKey);
        testParameter.put(FreeIPATest.SECRET_KEY, secretKey);
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
