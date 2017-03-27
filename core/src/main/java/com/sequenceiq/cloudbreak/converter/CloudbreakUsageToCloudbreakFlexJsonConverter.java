package com.sequenceiq.cloudbreak.converter;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class CloudbreakUsageToCloudbreakFlexJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakUsage, CloudbreakFlexUsageJson> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUsageToCloudbreakFlexJsonConverter.class);

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    @Inject
    private UserDetailsService userDetailsService;

    @Override
    public CloudbreakFlexUsageJson convert(CloudbreakUsage source) {
        CloudbreakFlexUsageJson json = new CloudbreakFlexUsageJson();
        json.setOwner(source.getOwner());
        json.setAccount(source.getAccount());
        json.setBlueprintName(source.getBlueprintName());
        json.setStackName(source.getStackName());
        json.setParentUuid(source.getParentUuid());
        json.setStackUuid(source.getStackUuid());
        json.setRegion(source.getRegion());
        json.setInstanceNum(source.getInstanceNum());
        json.setPeak(source.getPeak());
        json.setFlexId(source.getFlexId());
        json.setSmartSenseId(source.getSmartSenseId());
        json.setDay(getDayAsString(source.getDay()));
        json.setUsername(getUserEmail(source));
        return json;
    }

    private String getUserEmail(CloudbreakUsage source) {
        String cbUser;
        try {
            cbUser = userDetailsService.getDetails(source.getOwner(), UserFilterField.USERID).getUsername();
        } catch (Exception ex) {
            LOGGER.warn(String.format("Expected user was not found with '%s' id. Maybe it was deleted by the admin user.", source.getOwner()));
            cbUser = source.getOwner();
        }
        return cbUser;
    }

    private String getDayAsString(Date day) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return sdf.format(day);
    }
}
