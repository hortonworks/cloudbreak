package com.sequenceiq.cloudbreak.cloud.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ConfigurationProperties("cb.hdp")
@Component
public class DefaultHDPInfo extends HDPInfo {


}
