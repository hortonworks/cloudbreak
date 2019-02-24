package com.sequenceiq.it.cloudbreak.newway.entity.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class PlatformNetworksTestDto extends AbstractCloudbreakEntity<Object, PlatformNetworksV4Response, PlatformNetworksTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformNetworksTestDto.class);

    private String credentialName;

    private String region;

    private String platformVariant;

    private String availabilityZone;

    protected PlatformNetworksTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public PlatformNetworksTestDto valid() {
        return withPlatformVariant("mock")
                .withRegion("mockRegion")
                .withAvailabilityZone("mockAZ")
                .withCredentialName("mock-credential");
    }

    public String getCredentialName() {
        return credentialName;
    }

    public PlatformNetworksTestDto withCredentialName(String credentialName) {
        this.credentialName = credentialName;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public PlatformNetworksTestDto withRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public PlatformNetworksTestDto withPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
        return this;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public PlatformNetworksTestDto withAvailabilityZone(String availabilityZone) {
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
