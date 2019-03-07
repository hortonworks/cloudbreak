package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters.CLOUD_PROVIDER;

import javax.inject.Inject;

import org.kohsuke.randname.RandomNameGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;

@Component
public class RandomNameCreator {

    public static final String PREFIX = "autotest-";

    private static final int MAX_LENGTH = 40;

    @Inject
    private RandomNameGenerator generator;

    @Value("${" + CLOUD_PROVIDER + ":MOCK}")
    private CloudPlatform cloudPlatform;

    public String getRandomNameForResource() {
        return trim(PREFIX + cloudPlatform.name().toLowerCase() + '-' + generator.next().replaceAll("_", "-"));
    }

    public String getInvalidRandomNameForResource() {
        return trim(PREFIX + cloudPlatform.name().toLowerCase() + "-?!;" + generator.next().replaceAll("_", "-"));
    }

    private String trim(String name) {
        return (name.length() > MAX_LENGTH) ? name.substring(0, MAX_LENGTH) : name;
    }
}
