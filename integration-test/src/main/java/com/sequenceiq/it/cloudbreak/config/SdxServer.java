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
import com.sequenceiq.it.cloudbreak.SdxTest;

@Component
public class SdxServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    private static final int SDX_PORT = 8086;

    @Value("${integrationtest.sdx.server}")
    private String server;

    @Value("${sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Inject
    private TestParameter testParameter;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    @PostConstruct
    private void init() throws IOException {
        configureFromCliProfile();

        checkNonEmpty("integrationtest.sdx.server", server);
        checkNonEmpty("sdx.server.contextPath", sdxRootContextPath);
        checkNonEmpty("integrationtest.user.crn", userCrn);

        testParameter.put(SdxTest.SDX_SERVER_ROOT, server + sdxRootContextPath);
        testParameter.put(SdxTest.USER_CRN, userCrn);
    }

    private void configureFromCliProfile() throws IOException {
        Map<String, String> profiles = cliProfileReaderService.read();
        if (profiles == null) {
            LOGGER.warn("localhost in ~/.dp/config or "
                    + "integrationtest.dp.profile in application.yml or "
                    + "-Dintegrationtest.dp.profile should be added with exited profile");
            return;
        }

        server = serverUtil.calculateServerAddressFromProfile(server, profiles, SDX_PORT);
        userCrn = serverUtil.calculateCrnsFromProfile(userCrn, profiles);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
