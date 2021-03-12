package com.sequenceiq.cloudbreak.telemetry;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataBusEndpointProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBusEndpointProvider.class);

    private static final String CNAME_ENDPOINT = "https://dbusapi.us-west-1.altus.cloudera.com";

    private static final Map<String, String> DBUS_API_S3_ENDPOINT_PATTERN_PAIRS =
            Map.of("dbusapi.us-west-1", "https://cloudera-dbus-prod.s3.amazonaws.com",
                    "dbusapi.sigma-dev", "https://cloudera-dbus-dev.s3.amazonaws.com",
                    "dbusapi.sigma-stage", "https://cloudera-dbus-stage.s3.amazonaws.com");

    public String getDataBusEndpoint(String endpoint, boolean useCname) {
        String databusUrl = endpoint;
        if (useCname) {
            LOGGER.debug("Forcing to use '{}' for databus endpoint.", CNAME_ENDPOINT);
            databusUrl = CNAME_ENDPOINT;
        }
        return databusUrl;
    }

    public String getDatabusS3Endpoint(String endpoint) {
        String dbusS3Endpoint = null;
        for (Map.Entry<String, String> entry : DBUS_API_S3_ENDPOINT_PATTERN_PAIRS.entrySet()) {
            if (StringUtils.isNotBlank(endpoint) && endpoint.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return dbusS3Endpoint;
    }
}
