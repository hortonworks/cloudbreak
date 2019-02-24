package com.sequenceiq.it.cloudbreak.newway.entity.accessconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class PlatformAccessConfigsTestDto extends AbstractCloudbreakEntity<Object, PlatformAccessConfigsV4Response, PlatformAccessConfigsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAccessConfigsTestDto.class);

    private String credentialName;

    private String region;

    private String platformVariant;

    private String availabilityZone;

    protected PlatformAccessConfigsTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public PlatformAccessConfigsTestDto valid() {
        return withPlatformVariant("mock")
                .withRegion("mockRegion")
                .withAvailabilityZone("mockAZ")
                .withCredentialName("mock-credential");
    }

    public String getCredentialName() {
        return credentialName;
    }

    public PlatformAccessConfigsTestDto withCredentialName(String credentialName) {
        this.credentialName = credentialName;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public PlatformAccessConfigsTestDto withRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public PlatformAccessConfigsTestDto withPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
        return this;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public PlatformAccessConfigsTestDto withAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.debug("this entry point does not have any clean up operation");
    }

    @Override
    public int order() {
        return 500;
    }

}
