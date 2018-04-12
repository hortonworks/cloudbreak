package com.sequenceiq.cloudbreak.service.decorator;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class TemplateDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateDecorator.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private LocationService locationService;

    public Template decorate(Credential credential, Template subject, String region, String availabilityZone, String variant) {
        PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
        CloudVmTypes vmTypesV2 = cloudParameterService.getVmTypesV2(credential, region, variant, new HashMap<>());
        String locationString = locationService.location(region, availabilityZone);
        VolumeParameterConfig config;
        try {
            Platform platform = Platform.platform(subject.cloudPlatform());
            VmType vmType = vmTypesV2.getCloudVmResponses()
                    .getOrDefault(locationString, Collections.emptySet())
                    .stream()
                    .filter(curr -> curr.value().equals(subject.getInstanceType())).findFirst().get();
            Map<String, VolumeParameterType> map = platformDisks.getDiskMappings().get(platform);
            VolumeParameterType volumeParameterType = map.get(subject.getVolumeType());

            config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
        } catch (NoSuchElementException ignored) {
            LOGGER.info("No VolumeParameterConfig found, which might be normal for platforms like OpenStack");
            config = VolumeParameterConfig.EMPTY;
        }

        if (config.volumeParameterType() != null) {
            if (subject.getVolumeCount() == null) {
                subject.setVolumeCount(config.maximumNumber());
            }
            if (subject.getVolumeSize() == null) {
                subject.setVolumeSize(config.maximumSize());
            }
        }

        return subject;
    }
}
