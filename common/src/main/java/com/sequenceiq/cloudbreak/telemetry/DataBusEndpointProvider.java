package com.sequenceiq.cloudbreak.telemetry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPattern;
import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPatternsConfig;

@Component
public class DataBusEndpointProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBusEndpointProvider.class);

    private static final String DATABUS_S3_HTTP_ENDPOINT = "https://%s.s3.amazonaws.com";

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final DataBusS3EndpointPatternsConfig dataBusS3EndpointPatternsConfig;

    private final String databusCnameEndpoint;

    public DataBusEndpointProvider(AltusDatabusConfiguration altusDatabusConfiguration,
            DataBusS3EndpointPatternsConfig dataBusS3EndpointPatternsConfig, @Value("${altus.databus.cname}") String databusCnameEndpoint) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
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

    public String getDatabusS3Endpoint(String endpoint, boolean forceToUsePatterns) {
        if (StringUtils.isNotBlank(altusDatabusConfiguration.getAltusDatabusS3BucketName()) && !forceToUsePatterns) {
            return String.format(DATABUS_S3_HTTP_ENDPOINT, altusDatabusConfiguration.getAltusDatabusS3BucketName());
        } else {
            return getDatabusS3EndpointFromPatterns(endpoint);
        }
    }

    public String getDatabusS3Endpoint(String endpoint) {
        return getDatabusS3Endpoint(endpoint, false);
    }

    private String getDatabusS3EndpointFromPatterns(String endpoint) {
        String dbusS3Endpoint = null;
        for (DataBusS3EndpointPattern s3EndpointPatternObj : dataBusS3EndpointPatternsConfig.getPatterns()) {
            if (StringUtils.isNotBlank(endpoint) && endpoint.contains(s3EndpointPatternObj.getPattern())) {
                return s3EndpointPatternObj.getEndpoint();
            }
        }
        return dbusS3Endpoint;
    }
}
