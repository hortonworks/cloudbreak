package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class RandomNameCreator {

    private static final int MAX_LENGTH = 40;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public String getRandomNameForResource() {
        return trim(prefix() + '-' + dateTime() + '-' + uuid());
    }

    public String getInvalidRandomNameForResource() {
        return trim(prefix() + "-?!;" + '-' + dateTime() + '-' + uuid());
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
