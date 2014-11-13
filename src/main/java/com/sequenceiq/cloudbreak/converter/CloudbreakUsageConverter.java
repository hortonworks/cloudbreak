package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class CloudbreakUsageConverter extends AbstractConverter<CloudbreakUsageJson, CloudbreakUsage> {

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public CloudbreakUsageJson convert(CloudbreakUsage entity) {
        CloudbreakUsageJson json = new CloudbreakUsageJson();
        json.setOwner(entity.getOwner());
        json.setAccount(entity.getAccount());
        json.setBlueprintName(entity.getBlueprintName());
        json.setBlueprintId(entity.getBlueprintId());
        json.setCloud(entity.getCloud());
        json.setZone(entity.getZone());
        json.setRunningHours(entity.getRunningHours());
        json.setMachineType(entity.getMachineType());
        json.setDay(entity.getDay().toString());
        json.setStackId(entity.getStackId());
        json.setStackStatus(entity.getStackStatus());

        CbUser cbUser = userDetailsService.getDetails(entity.getOwner(), UserFilterField.USERID);
        json.setUsername(cbUser.getUsername());
        return json;
    }

    @Override
    public CloudbreakUsage convert(CloudbreakUsageJson json) {
        CloudbreakUsage entity = new CloudbreakUsage();
        entity.setOwner(json.getOwner());
        entity.setAccount(json.getAccount());
        entity.setBlueprintName(json.getBlueprintName());
        entity.setBlueprintId(json.getBlueprintId());
        entity.setCloud(json.getCloud());
        entity.setZone(json.getZone());
        entity.setRunningHours(json.getRunningHours());
        entity.setMachineType(json.getMachineType());
        entity.setStackId(json.getStackId());
        entity.setStackStatus(json.getStackStatus());
        return entity;

    }
}
