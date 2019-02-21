package com.sequenceiq.it.cloudbreak.newway;


import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters.CLOUD_PROVIDER;

import javax.inject.Inject;

import org.kohsuke.randname.RandomNameGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;

@Component
public class RandomNameCreator {

    @Inject
    private RandomNameGenerator generator;

    @Value("${" + CLOUD_PROVIDER + ":MOCK}")
    private CloudPlatform cloudPlatform;

    public String getRandomNameForResource() {
        return cloudPlatform.name().toLowerCase() + '-' + generator.next().replaceAll("_", "-");
    }

    public String getInvalidRandomNameForResource() {
        return cloudPlatform.name().toLowerCase() + "-?!;" + generator.next().replaceAll("_", "-");
    }
}
