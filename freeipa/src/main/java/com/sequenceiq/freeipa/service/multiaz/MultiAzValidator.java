package com.sequenceiq.freeipa.service.multiaz;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class MultiAzValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzValidator.class);

    @Value("${cb.multiaz.supported.instancemetadata.platforms:AWS,GCP,AZURE,YARN}")
    private Set<String> supportedInstanceMetadataPlatforms;

    @PostConstruct
    public void initSupportedVariants() {
        if (supportedInstanceMetadataPlatforms.isEmpty()) {
            supportedInstanceMetadataPlatforms = Set.of(
                    CloudPlatform.AWS.name(),
                    CloudPlatform.GCP.name(),
                    CloudPlatform.AZURE.name(),
                    CloudPlatform.YARN.name());
        }
    }

    public boolean supportedForInstanceMetadataGeneration(String cloudPlatform) {
        return supportedInstanceMetadataPlatforms.contains(cloudPlatform);
    }
}
