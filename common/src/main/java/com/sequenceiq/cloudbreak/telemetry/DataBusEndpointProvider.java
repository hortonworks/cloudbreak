package com.sequenceiq.cloudbreak.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataBusEndpointProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBusEndpointProvider.class);

    private static final String CNAME_ENDPOINT = "https://dbusapi.us-west-1.altus.cloudera.com";

    public String getDataBusEndpoint(String endpoint, boolean useCname) {
        String databusUrl = endpoint;
        if (useCname) {
            LOGGER.debug("Forcing to use '{}' for databus endpoint.", CNAME_ENDPOINT);
            databusUrl = CNAME_ENDPOINT;
        }
        return databusUrl;
    }
}
