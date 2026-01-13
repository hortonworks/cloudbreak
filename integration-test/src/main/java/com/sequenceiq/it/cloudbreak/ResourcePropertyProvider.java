package com.sequenceiq.it.cloudbreak;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;

@Component
public class ResourcePropertyProvider {

    private static final int MAX_LENGTH = 19;

    private static final int ENVIRONMENT_NAME_MAX_LENGTH = 28;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public String getName() {
        return trim(prefix() + '-' + uuid(), MAX_LENGTH);
    }

    public String getName(CloudPlatform cloudPlatform) {
        return trim(prefix(cloudPlatform) + '-' + uuid(), MAX_LENGTH);
    }

    public String getName(int maxLength) {
        return trim(prefix() + '-' + uuid(), maxLength);
    }

    public String getName(int maxLength, CloudPlatform cloudPlatform) {
        return trim(prefix(cloudPlatform) + '-' + uuid(), maxLength);
    }

    public String getEnvironmentName() {
        return trim(prefix() + '-' + uuid(), ENVIRONMENT_NAME_MAX_LENGTH);
    }

    public String getEnvironmentName(String additionalPrefix) {
        return trim(prefix() + '-' + additionalPrefix + '-' + uuid(), ENVIRONMENT_NAME_MAX_LENGTH);
    }

    public String getEnvironmentName(CloudPlatform cloudPlatform) {
        return trim(prefix(cloudPlatform) + '-' + uuid(), ENVIRONMENT_NAME_MAX_LENGTH);
    }

    public String getInvalidName() {
        return trim(prefix() + "-?!;" + uuid(), MAX_LENGTH);
    }

    public String getDescription(String typeName) {
        return "Test " + typeName + ". Created at: " + dateTime();
    }

    public String prefix() {
        return commonCloudProperties.getCloudProvider().toLowerCase(Locale.ROOT) + "-test";
    }

    public String prefix(CloudPlatform cloudPlatform) {
        String cloudPlatformPrefix = commonCloudProperties.getCloudProvider().toLowerCase(Locale.ROOT);
        if (cloudPlatform != null) {
            cloudPlatformPrefix = cloudPlatform.name().toLowerCase(Locale.ROOT);
        }
        return cloudPlatformPrefix + "-test";
    }

    private String dateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.format(dateTimeFormatter);
    }

    private String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private String trim(String name, int maxLength) {
        return (name.length() > maxLength) ? name.substring(0, maxLength) : name;
    }
}
