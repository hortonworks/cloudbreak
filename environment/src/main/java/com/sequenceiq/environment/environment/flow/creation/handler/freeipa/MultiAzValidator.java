package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

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

    @Value("${cb.multiaz.supported.platform:AWS}")
    private Set<String> supportedMultiAzPlatforms;

    @PostConstruct
    public void initSupportedPlatforms() {
        if (supportedMultiAzPlatforms.isEmpty()) {
            supportedMultiAzPlatforms = Set.of(
                    CloudPlatform.AWS.name());
        }
    }

    public boolean suportedMultiAzForEnvironment(String platform) {
        return supportedMultiAzPlatforms.contains(platform);
    }
}
