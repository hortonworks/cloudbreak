package com.sequenceiq.it.cloudbreak.newway;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;

@Component
public class ResourcePropertyProvider {

    private static final int MAX_LENGTH = 40;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public String getName() {
        return trim(prefix() + '-' + uuid());
    }

    public String getInvalidName() {
        return trim(prefix() + "-?!;" + uuid());
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

    private String trim(String name) {
        return (name.length() > MAX_LENGTH) ? name.substring(0, MAX_LENGTH) : name;
    }
}
