package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@Component
public class CloudbreakUsageRequestToCloudbreakUsageConverter extends AbstractConversionServiceAwareConverter<CloudbreakUsageJson, CloudbreakUsage> {
    @Override
    public CloudbreakUsage convert(CloudbreakUsageJson json) {
        CloudbreakUsage entity = new CloudbreakUsage();
        entity.setProvider(json.getProvider());
        entity.setRegion(json.getRegion());
        entity.setAvailabilityZone(json.getAvailabilityZone());
        entity.setInstanceHours(json.getInstanceHours());
        entity.setStackId(json.getStackId());
        entity.setStackName(json.getStackName());
        entity.setInstanceType(json.getInstanceType());
        entity.setInstanceGroup(json.getInstanceGroup());
        entity.setBlueprintId(json.getBlueprintId());
        entity.setBlueprintName(json.getBlueprintName());
        return entity;
    }
}
