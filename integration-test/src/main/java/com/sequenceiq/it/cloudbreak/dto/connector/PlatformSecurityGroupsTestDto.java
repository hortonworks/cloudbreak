package com.sequenceiq.it.cloudbreak.dto.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class PlatformSecurityGroupsTestDto extends AbstractCloudbreakTestDto<Object, PlatformSecurityGroupsV4Response, PlatformSecurityGroupsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSecurityGroupsTestDto.class);

    private String credentialName;

    private String region;

    private String platformVariant;

    private String availabilityZone;

    protected PlatformSecurityGroupsTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public PlatformSecurityGroupsTestDto valid() {
        return withPlatformVariant("mock")
                .withRegion("mockRegion")
                .withAvailabilityZone("mockAZ")
                .withCredentialName("mock-credential");
    }

    public String getCredentialName() {
        return credentialName;
    }

    public PlatformSecurityGroupsTestDto withCredentialName(String credentialName) {
        this.credentialName = credentialName;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public PlatformSecurityGroupsTestDto withRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public PlatformSecurityGroupsTestDto withPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
        return this;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public PlatformSecurityGroupsTestDto withAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
