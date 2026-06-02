package com.sequenceiq.environment.encryptionprofile.cache;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class DefaultEncryptionProfileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEncryptionProfileProvider.class);

    private static final String DEFAULT_PATH = "defaults/encryptionprofile/";

    private static final String LOCATION_PATTERN = "classpath:" + DEFAULT_PATH + "**/*.json";

    private static final String CRN_TEMPLATE = "crn:cdp:environments:{region}:cloudera:encryptionProfile:{name}";

    private static final String FEDRAMP_FOLDER = "gov";

    private static final String PUBLIC_CLOUD_FOLDER = "public";

    private final Map<String, EncryptionProfile> defaultEncryptionProfileByName = new HashMap<>();

    private final Map<String, EncryptionProfile> defaultEncryptionProfileByCrn = new HashMap<>();

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Value("${crn.region:}")
    private String region;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private CommonGovService commonGovService;

    @PostConstruct
    public void loadDefaultEncryptionProfiles() throws IOException {
        Set<EncryptionProfile> encryptionProfiles = getDefaultEncryptionProfileFiles()
                .stream()
                .map(this::encryptionProfile)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        defaultEncryptionProfileByName.putAll(
                encryptionProfiles.stream()
                        .collect(Collectors.toMap(EncryptionProfile::getName, profile -> profile))
        );

        defaultEncryptionProfileByCrn.putAll(
                encryptionProfiles.stream()
                        .collect(Collectors.toMap(EncryptionProfile::getResourceCrn, profile -> profile))
        );
        LOGGER.info("Default Encryption profiles loaded for the environment: {}", defaultEncryptionProfileByName.keySet());
    }

    private Set<String> getDefaultEncryptionProfileFiles() throws IOException {
        LOGGER.info("Loading default encryption profiles from {}", LOCATION_PATTERN);
        return Arrays.stream(resolver.getResources(LOCATION_PATTERN))
                .filter(this::filterForDeploymentType)
                .map(resource -> {
                    try {
                        return DEFAULT_PATH + resource.getURL().getPath().split(DEFAULT_PATH)[1];
                    } catch (IOException e) {
                        LOGGER.debug("Could not load encryption profile file: {}", resource, e);
                        throw new RuntimeException("Could not load encryption profile with name: " + resource, e);
                    }
                })
                .collect(Collectors.toSet());
    }

    private Optional<EncryptionProfile> encryptionProfile(String resource) {
        try {
            String encryptionProfileConfig = FileReaderUtils.readFileFromClasspath(resource);
            EncryptionProfile encryptionProfile = new Json(encryptionProfileConfig).get(EncryptionProfile.class);
            encryptionProfile.setResourceCrn(createResourceCrn(encryptionProfile.getName()));
            return Optional.of(encryptionProfile);
        } catch (IOException e) {
            LOGGER.debug("Could not load encryption profile file: {}", resource, e);
            throw new RuntimeException("Could not load encryption profile with name: " + resource, e);
        }
    }

    private boolean filterForDeploymentType(Resource resource) {
        try {
            String parentFolderName = resource.getURL().getPath().toLowerCase(Locale.ROOT);

            boolean publicCloudFile = parentFolderName.contains(PUBLIC_CLOUD_FOLDER);
            boolean publicCloudEnabled = !preferencesService.enabledPlatforms().isEmpty()
                    || preferencesService.enabledGovPlatforms().isEmpty();

            boolean govCloudFile = parentFolderName.contains(FEDRAMP_FOLDER);
            boolean govCloudEnabled = !preferencesService.enabledGovPlatforms().isEmpty();

            return (publicCloudEnabled && publicCloudFile) || (govCloudEnabled && govCloudFile);
        } catch (IOException e) {
            LOGGER.debug("Could not load encryption profile file: {}", resource, e);
            return false;
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
