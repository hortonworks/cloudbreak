package com.sequenceiq.cloudbreak.telemetry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPattern;
import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPatternsConfig;

@Component
public class DataBusEndpointProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBusEndpointProvider.class);

    private final DataBusS3EndpointPatternsConfig dataBusS3EndpointPatternsConfig;

    private final String databusCnameEndpoint;

    public DataBusEndpointProvider(DataBusS3EndpointPatternsConfig dataBusS3EndpointPatternsConfig,
            @Value("${altus.databus.cname}") String databusCnameEndpoint) {
        this.dataBusS3EndpointPatternsConfig = dataBusS3EndpointPatternsConfig;
        this.databusCnameEndpoint = databusCnameEndpoint;
    }

    public String getDataBusEndpoint(String endpoint, boolean useCname) {
        String databusUrl = endpoint;
        if (useCname) {
            LOGGER.debug("Forcing to use '{}' for databus endpoint.", databusCnameEndpoint);
            databusUrl = databusCnameEndpoint;
        }
        return databusUrl;
    }

    public String getDatabusS3Endpoint(String endpoint) {
        String dbusS3Endpoint = null;
        for (DataBusS3EndpointPattern s3EndpointPatternObj : dataBusS3EndpointPatternsConfig.getPatterns()) {
            if (StringUtils.isNotBlank(endpoint) && endpoint.contains(s3EndpointPatternObj.getPattern())) {
                return s3EndpointPatternObj.getEndpoint();
            }
        }
        return dbusS3Endpoint;
    }
}
