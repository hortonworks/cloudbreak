package com.sequenceiq.cloudbreak.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("altus.databus.s3.endpoint")
public class DataBusS3EndpointPatternsConfig {

    private List<DataBusS3EndpointPattern> patterns;

    public List<DataBusS3EndpointPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<DataBusS3EndpointPattern> patterns) {
        this.patterns = patterns;
    }
}
