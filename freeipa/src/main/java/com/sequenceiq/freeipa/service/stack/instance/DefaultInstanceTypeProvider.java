package com.sequenceiq.freeipa.service.stack.instance;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.service.CredentialService;

/**
 * This class reads the environment properties of default instance type configurations for each cloud provider platform.
 * In order to configure a instance type for a platform, eg. AWS, one must specify a property like this:
 * -Dfreeipa.platform.default.instanceType.AWS=m5.large
 * <p>
 * For Azure:
 * -Dfreeipa.platform.default.instanceType.AZURE=Standard_DS3_v2
 * <p>
 * etc.
 */
@Service
public class DefaultInstanceTypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInstanceTypeProvider.class);

    private static final String DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX = "freeipa.platform.default.instanceType.";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private Environment environment;

    public List<String> getForPlatform(String credentialCrn, Platform platform, Region region, Architecture architecture) {
        try {
            ExtendedCloudCredential cloudCredential = credentialToCloudCredentialConverter
                    .convert(credentialService.getCredentialByCredCrn(credentialCrn));
            CloudRegions cloudRegions = cloudPlatformConnectors.getDefault(platform)
                    .platformResources()
                    .regions(cloudCredential, region, Map.of(), false);
            architecture = architecture == null ? Architecture.X86_64 : architecture;
            Map<Region, List<String>> defaultFreeIPAInstances = getDefaultMap(cloudRegions, architecture);
            List<String> defaultFreeIPAInstance = defaultFreeIPAInstances.get(region);
            if (defaultFreeIPAInstances.isEmpty() || defaultFreeIPAInstance == null) {
                LOGGER.debug("No default instance type found for platform: {}. Falling back to default empty string. "
                                + "Set '{}' property if '{}' is a valid cloud provider.",
                        platform, DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX + platform, platform);
            }
            return defaultFreeIPAInstance == null ? List.of() : defaultFreeIPAInstance;
        } catch (Exception e) {
            throw new CloudbreakRuntimeException(e);
        }
    }

    private Map<Region, List<String>> getDefaultMap(CloudRegions cloudRegions, Architecture architecture) {
        return switch (architecture) {
            case ARM64 -> cloudRegions.getDefaultArmFreeIPAVmtypes();
            default -> cloudRegions.getDefaultX86FreeIPAVmtypes();
        };
    }
}
