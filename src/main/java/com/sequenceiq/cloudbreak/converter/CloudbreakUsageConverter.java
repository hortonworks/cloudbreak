package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@Component
public class CloudbreakUsageConverter extends AbstractConverter<CloudbreakUsageJson, CloudbreakUsage> {
    @Override
    public CloudbreakUsageJson convert(CloudbreakUsage entity) {
        CloudbreakUsageJson json = new CloudbreakUsageJson();
        json.setUserName(entity.getOwner());
        json.setAccountName(entity.getAccount());
        json.setBlueprintName(entity.getBlueprintName());
        json.setBlueprintId(entity.getBlueprintId());
        json.setCloud(entity.getCloud());
        json.setZone(entity.getZone());
        json.setRunningHours(entity.getRunningHours());
        json.setMachineType(entity.getMachineType());
        json.setDay(entity.getDay().toString());
        return json;
    }

    @Override
    public CloudbreakUsage convert(CloudbreakUsageJson json) {
        CloudbreakUsage entity = new CloudbreakUsage();
        entity.setOwner(json.getUserName());
        entity.setAccount(json.getAccountName());
        entity.setBlueprintName(json.getBlueprintName());
        entity.setBlueprintId(json.getBlueprintId());
        entity.setCloud(json.getCloud());
        entity.setZone(json.getZone());
        entity.setRunningHours(json.getRunningHours());
        entity.setMachineType(json.getMachineType());
        return entity;

    }
}
