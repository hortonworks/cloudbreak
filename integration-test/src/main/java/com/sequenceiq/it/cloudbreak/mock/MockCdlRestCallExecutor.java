package com.sequenceiq.it.cloudbreak.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockCdlRestCallExecutor extends AbstractRestCallExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCdlRestCallExecutor.class);

    @Value("${integrationtest.mock.cdl.rest.host:localhost}")
    private String mockCdlRestHost;

    @Value("${integrationtest.mock.cdl.rest.port:10080}")
    private int mockCdlRestPort;

    @Override
    protected String getUrl() {
        return String.format("http://%s:%d", mockCdlRestHost, mockCdlRestPort);
    }
}
