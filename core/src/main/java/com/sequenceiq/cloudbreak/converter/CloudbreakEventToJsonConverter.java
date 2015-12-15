package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.model.CloudbreakEventsJson;

@Component
public class CloudbreakEventToJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakEvent, CloudbreakEventsJson> {

    @Override
    public CloudbreakEventsJson convert(CloudbreakEvent entity) {
        CloudbreakEventsJson json = new CloudbreakEventsJson();
        json.setAccount(entity.getAccount());
        json.setBlueprintId(entity.getBlueprintId());
        json.setBlueprintName(entity.getBlueprintName());
        json.setCloud(entity.getCloud());
        json.setEventMessage(entity.getEventMessage());
        json.setEventType(entity.getEventType());
        json.setEventTimestamp(entity.getEventTimestamp().getTime());
        json.setRegion(entity.getRegion());
        json.setOwner(entity.getOwner());
        json.setStackId(entity.getStackId());
        json.setStackName(entity.getStackName());
        json.setStackStatus(entity.getStackStatus());
        json.setNodeCount(entity.getNodeCount());
        json.setInstanceGroup(entity.getInstanceGroup());
        json.setClusterStatus(entity.getClusterStatus());
        return json;
    }
}
