package com.sequenceiq.cloudbreak.platform;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
public class PlatformConfig {

    @Value("${cdp.platforms.supportedPlatforms}")
    private Set<CloudPlatform> cdpSupportedPlatforms;

    @Value("${cdp.platforms.experimentalPlatforms}")
    private Set<CloudPlatform> cdpExperimentalPlatforms;

    @Value("${cdp.platforms.supportedFeature.externalDatabase}")
    private Set<CloudPlatform> dbServiceSupportedPlatforms;

    @Value("${cdp.platforms.supportedFeature.stopDatabase}")
    private Set<CloudPlatform> dbServicePauseSupportedPlatforms;

    @Value("${cdp.platforms.supportedFeature.sslEnforcement}")
    private Set<CloudPlatform> dbServiceSslEnforcementSupportedPlatforms;

    private Set<CloudPlatform> allAvailablePlatforms;

    @PostConstruct
    public void init() {
        allAvailablePlatforms = new HashSet<>();
        allAvailablePlatforms.addAll(cdpSupportedPlatforms);
        allAvailablePlatforms.addAll(cdpExperimentalPlatforms);
    }

    /**
     * Contains platforms that are GA and should be available for all customers. Use this for customer facing elements like UI visibility.
     **/
    public Set<CloudPlatform> getCdpSupportedPlatforms() {
        return cdpSupportedPlatforms;
    }

    /**
     * Contains platforms that should only be enabled for internal testing. ycloud,mock.
     **/
    public Set<CloudPlatform> getCdpExperimentalPlatforms() {
        return cdpExperimentalPlatforms;
    }

    /**
     * Contains All possible platforms. Use this internally.
     **/
    public Set<CloudPlatform> getAllPossiblePlatforms() {
        return allAvailablePlatforms;
    }

    public Set<CloudPlatform> getSupportedExternalDatabasePlatforms() {
        return dbServiceSupportedPlatforms;
    }

    public Set<CloudPlatform> getExperimentalExternalDatabasePlatforms() {
        return cdpExperimentalPlatforms;
    }

    public Set<CloudPlatform> getDatabasePauseSupportedPlatforms() {
        return dbServicePauseSupportedPlatforms;
    }

    public Set<CloudPlatform> getDatabaseSslEnforcementSupportedPlatforms() {
        return dbServiceSslEnforcementSupportedPlatforms;
    }

}
