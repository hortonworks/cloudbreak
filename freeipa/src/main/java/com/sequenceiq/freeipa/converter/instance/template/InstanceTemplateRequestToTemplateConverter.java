package com.sequenceiq.freeipa.converter.instance.template;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@Component
public class InstanceTemplateRequestToTemplateConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTemplateRequestToTemplateConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    public Template convert(InstanceTemplateRequest source, CloudPlatform cloudPlatform) {
        Template template = new Template();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.USER_MANAGED);
        setVolumesProperty(source.getAttachedVolumes(), template, cloudPlatform);
        template.setInstanceType(Objects.requireNonNullElse(source.getInstanceType(), defaultInstanceTypeProvider.getForPlatform(cloudPlatform.name())));
        Optional.ofNullable(source.getAws())
                .map(AwsInstanceTemplateParameters::getSpot)
                .map(AwsInstanceTemplateSpotParameters::getPercentage)
                .ifPresent(spotPercentage -> template.setAttributes(new Json(Map.of("spotPercentage", spotPercentage))));
        return template;
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Failed to parse template parameters as JSON.", e);
                throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
            }
        };
    }

    private void setVolumesProperty(Set<VolumeRequest> attachedVolumes, Template template, CloudPlatform cloudPlatform) {
        if (!CollectionUtils.isEmpty(attachedVolumes)) {
            attachedVolumes.stream().findFirst().ifPresent(v -> {
                String volumeType = v.getType();
                template.setVolumeType(volumeType == null ? "HDD" : volumeType);
                Integer volumeCount = v.getCount();
                template.setVolumeCount(volumeCount == null ? Integer.valueOf(0) : volumeCount);
                Integer volumeSize = v.getSize();
                template.setVolumeSize(volumeSize == null ? Integer.valueOf(0) : volumeSize);
            });
        } else {
            template.setVolumeCount(0);
            template.setVolumeSize(0);
        }
        template.setRootVolumeSize(defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform.name()));
    }
}
