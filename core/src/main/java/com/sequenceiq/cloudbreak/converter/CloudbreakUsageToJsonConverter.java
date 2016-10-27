package com.sequenceiq.cloudbreak.converter;

import java.text.SimpleDateFormat;
import java.time.Duration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.UsageStatus;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.UsageTimeService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class CloudbreakUsageToJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakUsage, CloudbreakUsageJson> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUsageToJsonConverter.class);

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private UsageTimeService usageTimeService;

    @Override
    public CloudbreakUsageJson convert(CloudbreakUsage entity) {
        CloudbreakUsageJson json = new CloudbreakUsageJson();
        String day = DATE_FORMAT.format(entity.getDay());
        String cbUser;
        try {
            cbUser = userDetailsService.getDetails(entity.getOwner(), UserFilterField.USERID).getUsername();
        } catch (Exception ex) {
            LOGGER.warn(String.format("Expected user was not found with '%s' id. Maybe it was deleted by the admin user.", entity.getOwner()));
            cbUser = entity.getOwner();
        }
        json.setOwner(entity.getOwner());
        json.setAccount(entity.getAccount());
        json.setProvider(entity.getProvider());
        json.setRegion(entity.getRegion());
        json.setAvailabilityZone(entity.getAvailabilityZone());
        json.setInstanceHours(getInstanceHours(entity));
        json.setDuration(getDuration(entity));
        json.setDay(day);
        json.setStackId(entity.getStackId());
        json.setStackName(entity.getStackName());
        json.setUsername(cbUser);
        json.setCosts(entity.getCosts());
        json.setInstanceType(entity.getInstanceType());
        json.setInstanceGroup(entity.getInstanceGroup());
        json.setBlueprintId(entity.getBlueprintId());
        json.setBlueprintName(entity.getBlueprintName());
        return json;
    }

    private Long getInstanceHours(CloudbreakUsage usage) {
        if (usage.getStatus() == UsageStatus.OPEN) {
            Duration newDuration = usageTimeService.calculateNewDuration(usage);
            return usageTimeService.convertToInstanceHours(newDuration);
        }
        return usage.getInstanceHours();
    }

    private String getDuration(CloudbreakUsage usage) {
        if (usage.getStatus() == UsageStatus.OPEN) {
            return usageTimeService.calculateNewDuration(usage).toString();
        }
        return usage.getDuration();
    }

}
