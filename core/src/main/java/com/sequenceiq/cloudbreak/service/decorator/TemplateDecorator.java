package com.sequenceiq.cloudbreak.service.decorator;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.common.api.type.CdpResourceType;

@Component
public class TemplateDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateDecorator.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private LocationService locationService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CloudParameterCache cloudParameterCache;

    public Template decorate(Credential credential, Template template, String region, String availabilityZone, String variant, CdpResourceType cdpResourceType,
            boolean gatewayType) {
        setRootVolumeSize(template, gatewayType);
        setRootVolumeType(template, credential.cloudPlatform());
        boolean needToFetchVolumeCountAndSize = template.getVolumeTemplates().stream()
                .anyMatch(it -> it.getVolumeCount() == null || it.getVolumeSize() == null);
        if (needToFetchVolumeCountAndSize) {
            LOGGER.info("We need to fetch the VM types. Perhaps the client does not provide the volume count or size");
            CloudVmTypes vmTypesV2 = cloudParameterService.getVmTypesV2(
                    extendedCloudCredentialConverter.convert(credential),
                    region,
                    variant,
                    cdpResourceType,
                    new HashMap<>());
            String locationString = locationService.location(region, availabilityZone);
            PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
            for (VolumeTemplate volumeTemplate : template.getVolumeTemplates()) {
                VolumeParameterConfig config;
                try {
                    config = resolveVolumeParameterConfig(template, volumeTemplate, platformDisks, vmTypesV2, locationString);
                } catch (NoSuchElementException ignored) {
                    LOGGER.debug("No VolumeParameterConfig found, which might be normal for platforms like YARN");
                    config = VolumeParameterConfig.EMPTY;
                }
                if (config.volumeParameterType() != null) {
                    if (volumeTemplate.getVolumeCount() == null) {
                        volumeTemplate.setVolumeCount(config.maximumNumber());
                    }
                    if (volumeTemplate.getVolumeSize() == null) {
                        volumeTemplate.setVolumeSize(config.maximumSize());
                    }
                }
            }
        }
        return template;
    }

    private VolumeParameterConfig resolveVolumeParameterConfig(Template template, VolumeTemplate volumeTemplate,
            PlatformDisks platformDisks, CloudVmTypes vmTypesV2, String locationString) {
        Platform platform = Platform.platform(template.getCloudPlatform());
        Map<String, VolumeParameterType> map = platformDisks.getDiskMappings().get(platform);
        VolumeParameterType volumeParameterType = map.get(volumeTemplate.getVolumeType());

        if (volumeParameterType == null) {
            throw new CloudbreakServiceException("Cannot find the volume type: " + volumeTemplate.getVolumeType()
                    + ". Supported types: " + String.join(", ", map.keySet()));
        }
        VmType vmType = vmTypesV2.getCloudVmResponses()
                .getOrDefault(locationString, Collections.emptySet())
                .stream()
                .filter(curr -> curr.value().equals(template.getInstanceType())).findFirst().get();
        return vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
    }

    private void setRootVolumeSize(Template template, boolean gatewayType) {
        if (template.getRootVolumeSize() == null) {
            LOGGER.debug("No root volume size was set in the request. Getting default value for platform '{}'", template.getCloudPlatform());
            template.setRootVolumeSize(defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(template.getCloudPlatform(), gatewayType));
        }
    }

    private void setRootVolumeType(Template template, String cloudPlatform) {
        if (isEmpty(template.getRootVolumeType())) {
            LOGGER.debug("Setting root volume type with default value for platform '{}'", template.getCloudPlatform());
            template.setRootVolumeType(cloudParameterCache.getDefaultVolumeType(cloudPlatform));
        }
    }
}
