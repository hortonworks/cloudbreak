package com.sequenceiq.it.cloudbreak.config.server;

import static com.sequenceiq.it.cloudbreak.CloudbreakTest.IMAGE_CATALOG_MOCK_SERVER_ROOT;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.util.TestParameter;

@Component
public class CloudbreakServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakServer.class);

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    private static final int DEFAULT_CLOUDBREAK_PORT = 9091;

    @Value("${cloudbreak.url:localhost:" + DEFAULT_CLOUDBREAK_PORT + "}")
    private String cloudbreakUrl;

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.user.accesskey:}")
    private String accessKey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretKey;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.user.name:}")
    private String userName;

    @Value("${integrationtest.dp.profile:}")
    private String profile;

    @Value("${mock.imagecatalog.server:localhost:10090}")
    private String mockImageCatalogAddr;

    @Inject
    private TestParameter testParameter;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    @PostConstruct
    private void init() throws IOException {

        configureFromCliProfile();

        checkNonEmpty("integrationtest.cloudbreak.server", server);
        checkNonEmpty("cloudbreak.url", cloudbreakUrl);
        checkNonEmpty("server.contextPath", cbRootContextPath);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.secretkey", secretKey);

        testParameter.put(CloudbreakTest.CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testParameter.put(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT, "http://" + cloudbreakUrl + cbRootContextPath);
        testParameter.put(CloudbreakTest.ACCESS_KEY, accessKey);
        testParameter.put(CloudbreakTest.SECRET_KEY, secretKey);
        testParameter.put(CloudbreakTest.USER_CRN, userCrn);
        testParameter.put(CloudbreakTest.USER_NAME, userName);

        testParameter.put(IMAGE_CATALOG_MOCK_SERVER_ROOT, mockImageCatalogAddr);
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
