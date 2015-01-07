package com.sequenceiq.cloudbreak.converter;

import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class CloudbreakUsageConverter extends AbstractConverter<CloudbreakUsageJson, CloudbreakUsage> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public CloudbreakUsageJson convert(CloudbreakUsage entity) {
        String day = DATE_FORMAT.format(entity.getDay());
        String zone = getZoneNameByProvider(entity.getCloud(), entity.getZone());
        CbUser cbUser = userDetailsService.getDetails(entity.getOwner(), UserFilterField.USERID);

        CloudbreakUsageJson json = new CloudbreakUsageJson();
        json.setOwner(entity.getOwner());
        json.setAccount(entity.getAccount());
        json.setBlueprintName(entity.getBlueprintName());
        json.setBlueprintId(entity.getBlueprintId());
        json.setCloud(entity.getCloud());
        json.setZone(zone);
        json.setInstanceHours(entity.getInstanceHours());
        json.setMachineType(entity.getMachineType());
        json.setDay(day);
        json.setStackId(entity.getStackId());
        json.setStackStatus(entity.getStackStatus());
        json.setStackName(entity.getStackName());
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
        entity.setInstanceHours(json.getInstanceHours());
        entity.setMachineType(json.getMachineType());
        entity.setStackId(json.getStackId());
        entity.setStackStatus(json.getStackStatus());
        entity.setStackName(json.getStackName());
        return entity;
    }

    private String getZoneNameByProvider(String cloud, String zoneFromUsage) {
        String zone = null;
        if (zoneFromUsage != null && CloudPlatform.AWS.name().equals(cloud)) {
            Regions transformedZone = Regions.fromName(zoneFromUsage);
            zone = transformedZone.name();
        } else if (zoneFromUsage != null && CloudPlatform.GCC.name().equals(cloud)) {
            GccZone transformedZone = GccZone.fromName(zoneFromUsage);
            zone = transformedZone.name();
        } else if (zoneFromUsage != null && CloudPlatform.AZURE.name().equals(cloud)) {
            AzureLocation transformedZone = AzureLocation.fromName(zoneFromUsage);
            zone = transformedZone.name();
        }
        return zone;
    }
}
