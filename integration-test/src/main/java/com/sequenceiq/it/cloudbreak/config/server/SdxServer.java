package com.sequenceiq.it.cloudbreak.config.server;

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
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.SdxTest;

@Component
public class SdxServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    @Value("${integrationtest.sdx.server}")
    private String server;

    @Value("${sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

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

        checkNonEmpty("integrationtest.sdx.server", server);
        checkNonEmpty("sdx.server.contextPath", sdxRootContextPath);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.privatekey", secretKey);

        testParameter.put(SdxTest.SDX_SERVER_ROOT, server + sdxRootContextPath);
        testParameter.put(SdxTest.ACCESS_KEY, accessKey);
        testParameter.put(SdxTest.SECRET_KEY, secretKey);
        testParameter.put(CloudbreakTest.USER_CRN, userCrn);
        testParameter.put(CloudbreakTest.USER_NAME, userName);
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
