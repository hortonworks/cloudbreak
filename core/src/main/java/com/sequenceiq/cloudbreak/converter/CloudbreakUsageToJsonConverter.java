package com.sequenceiq.cloudbreak.converter;

import java.text.SimpleDateFormat;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class CloudbreakUsageToJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakUsage, CloudbreakUsageJson> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    private UserDetailsService userDetailsService;

    @Override
    public CloudbreakUsageJson convert(CloudbreakUsage entity) {
        String day = DATE_FORMAT.format(entity.getDay());
        CbUser cbUser = userDetailsService.getDetails(entity.getOwner(), UserFilterField.USERID);

        CloudbreakUsageJson json = new CloudbreakUsageJson();
        json.setOwner(entity.getOwner());
        json.setAccount(entity.getAccount());
        json.setProvider(entity.getProvider());
        json.setRegion(entity.getRegion());
        json.setAvailabilityZone(entity.getAvailabilityZone());
        json.setInstanceHours(entity.getInstanceHours());
        json.setDay(day);
        json.setStackId(entity.getStackId());
        json.setStackName(entity.getStackName());
        json.setUsername(cbUser.getUsername());
        json.setCosts(entity.getCosts());
        json.setInstanceType(entity.getInstanceType());
        json.setInstanceGroup(entity.getInstanceGroup());
        return json;
    }

}
