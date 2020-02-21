package com.sequenceiq.it.cloudbreak;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;

@Component
public class ResourcePropertyProvider {

    private static final int MAX_LENGTH = 40;

    private static final int ENVIRONMENT_NAME_MAX_LENGTH = 28;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public String getName() {
        return trim(prefix() + '-' + uuid(), MAX_LENGTH);
    }

    public String getName(int maxLength) {
        return trim(prefix() + '-' + uuid(), maxLength);
    }

    public String getEnvironmentName() {
        return trim(prefix() + '-' + uuid(), ENVIRONMENT_NAME_MAX_LENGTH);
    }

    public String getInvalidName() {
        return trim(prefix() + "-?!;" + uuid(), MAX_LENGTH);
    }

    public String getDescription(String typeName) {
        return "Test " + typeName + ". Created at: " + dateTime();
    }

    public String prefix() {
        return commonCloudProperties.getCloudProvider().toLowerCase() + "-test";
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
