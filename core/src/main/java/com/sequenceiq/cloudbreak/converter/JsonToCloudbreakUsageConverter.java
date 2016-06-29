package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;

@Component
public class JsonToCloudbreakUsageConverter extends AbstractConversionServiceAwareConverter<CloudbreakUsageJson, CloudbreakUsage> {
    @Override
    public CloudbreakUsage convert(CloudbreakUsageJson json) {
        CloudbreakUsage entity = new CloudbreakUsage();
        entity.setOwner(json.getOwner());
        entity.setAccount(json.getAccount());
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
