package com.sequenceiq.it.cloudbreak.config;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.FreeIPATest;

@Component
public class FreeIPAServer {

    private static final String WARNING_TEXT_FORMAT = "Following variable must be set whether as environment variables or (test) application.yaml: %s";

    @Value("${integrationtest.freeipa.server}")
    private String server;

    @Value("${freeipa.server.contextPath:/freeipa}")
    private String freeIpaRootContextPath;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() throws IOException {
        checkNonEmpty("integrationtest.freeipa.server", server);
        checkNonEmpty("freeipa.server.contextPath", freeIpaRootContextPath);
        checkNonEmpty("integrationtest.user.crn", userCrn);

        testParameter.put(FreeIPATest.FREEIPA_SERVER_ROOT, server + freeIpaRootContextPath);
        testParameter.put(FreeIPATest.USER_CRN, userCrn);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(String.format(WARNING_TEXT_FORMAT, name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}
