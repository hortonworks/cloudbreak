package com.sequenceiq.freeipa.converter.instance.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.GcpInstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
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

    @Inject
    private EntitlementService entitlementService;

    public Template convert(InstanceTemplateRequest source, CloudPlatform cloudPlatform, String accountId, String diskEncryptionSetId, String gcpKmsEncryptionKey) {
        Template template = new Template();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.USER_MANAGED);
        setVolumesProperty(source.getAttachedVolumes(), template, cloudPlatform);
        template.setInstanceType(Objects.requireNonNullElse(source.getInstanceType(), defaultInstanceTypeProvider.getForPlatform(cloudPlatform.name())));
        Map<String, Object> attributes = new HashMap<>();
        if (cloudPlatform == CloudPlatform.AWS) {
            // FIXME Enable EBS encryption with appropriate KMS key
            attributes.put(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Boolean.TRUE);
            attributes.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name());
        }
        Optional.ofNullable(source.getAws())
                .map(AwsInstanceTemplateParameters::getSpot)
                .ifPresent(spotParameters -> {
                            attributes.put(AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, spotParameters.getPercentage());
                            if (Objects.nonNull(spotParameters.getMaxPrice())) {
                                attributes.put(AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, spotParameters.getMaxPrice());
                            }
                        }
                );
        if (diskEncryptionSetId != null && cloudPlatform == CloudPlatform.AZURE) {
            attributes.put(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, diskEncryptionSetId);
            attributes.put(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, true);
        }

        if (gcpKmsEncryptionKey != null && cloudPlatform == CloudPlatform.GCP) {
            attributes.put(GcpInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, gcpKmsEncryptionKey);
            attributes.put(GcpInstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM);
            attributes.put(GcpInstanceTemplate.KEY_ENCRYPTION_METHOD, KeyEncryptionMethod.KMS);
        }

        template.setAttributes(new Json(attributes));
        return template;
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
