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
        String zone = getZoneNameByProvider(entity.getProvider(), entity.getRegion());
        CbUser cbUser = userDetailsService.getDetails(entity.getOwner(), UserFilterField.USERID);

        CloudbreakUsageJson json = new CloudbreakUsageJson();
        json.setOwner(entity.getOwner());
        json.setAccount(entity.getAccount());
        json.setProvider(entity.getProvider());
        json.setRegion(zone);
        json.setInstanceHours(entity.getInstanceHours());
        json.setDay(day);
        json.setStackId(entity.getStackId());
        json.setStackName(entity.getStackName());
        json.setUsername(cbUser.getUsername());
        json.setCosts(entity.getCosts());
        json.setInstanceType(entity.getInstanceType());
        json.setHostGroup(entity.getHostGroup());
        return json;
    }

    @Override
    public CloudbreakUsage convert(CloudbreakUsageJson json) {
        CloudbreakUsage entity = new CloudbreakUsage();
        entity.setOwner(json.getOwner());
        entity.setAccount(json.getAccount());
        entity.setProvider(json.getProvider());
        entity.setRegion(json.getRegion());
        entity.setInstanceHours(json.getInstanceHours());
        entity.setStackId(json.getStackId());
        entity.setStackName(json.getStackName());
        entity.setInstanceType(json.getInstanceType());
        entity.setHostGroup(json.getHostGroup());
        return entity;
    }

    private String getZoneNameByProvider(String cloud, String zoneFromUsage) {
        String zone = "";
        if (zoneFromUsage != null && CloudPlatform.AWS.name().equals(cloud)) {
            Regions transformedZone = Regions.valueOf(zoneFromUsage);
            zone = transformedZone.name();
        } else if (zoneFromUsage != null && CloudPlatform.GCC.name().equals(cloud)) {
            GccZone transformedZone = GccZone.valueOf(zoneFromUsage);
            zone = transformedZone.name();
        } else if (zoneFromUsage != null && CloudPlatform.AZURE.name().equals(cloud)) {
            AzureLocation transformedZone = AzureLocation.valueOf(zoneFromUsage);
            zone = transformedZone.name();
        }
        return zone;
    }
}
