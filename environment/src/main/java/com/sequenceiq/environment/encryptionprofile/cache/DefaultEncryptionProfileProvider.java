package com.sequenceiq.environment.encryptionprofile.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class DefaultEncryptionProfileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEncryptionProfileProvider.class);

    private static final String LOCATION_PATTERN = "classpath:defaultencryptionprofile/**/*.json";

    private static final String CRN_TEMPLATE = "crn:cdp:environments:{region}:cloudera:encryptionProfile:{name}";

    private final Map<String, EncryptionProfile> defaultEncryptionProfileByName = new HashMap<>();

    private final Map<String, EncryptionProfile> defaultEncryptionProfileByCrn = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${crn.region:}")
    private String region;

    @PostConstruct
    public void loadDefaultEncryptionProfiles() throws IOException {
        LOGGER.info("Loading default encryption profiles from {}", LOCATION_PATTERN);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(LOCATION_PATTERN);

        for (Resource resource : resources) {
            EncryptionProfile encryptionProfile = objectMapper.readValue(resource.getInputStream(), EncryptionProfile.class);
            encryptionProfile.setResourceCrn(createResourceCrn(encryptionProfile.getName()));
            defaultEncryptionProfileByName.put(encryptionProfile.getName(), encryptionProfile);
            defaultEncryptionProfileByCrn.put(encryptionProfile.getResourceCrn(), encryptionProfile);
            LOGGER.debug("Loaded default encryption profile: {}", encryptionProfile);
        }
    }

    private String createResourceCrn(String name) {
        return CRN_TEMPLATE.replace("{region}", region).replace("{name}", name);
    }

    public Map<String, EncryptionProfile> defaultEncryptionProfilesByName() {
        return defaultEncryptionProfileByName;
    }

    public Map<String, EncryptionProfile> defaultEncryptionProfilesByCrn() {
        return defaultEncryptionProfileByCrn;
    }
}
