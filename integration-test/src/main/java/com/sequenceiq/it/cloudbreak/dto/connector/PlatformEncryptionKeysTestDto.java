package com.sequenceiq.it.cloudbreak.dto.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class PlatformEncryptionKeysTestDto extends AbstractCloudbreakTestDto<Object, PlatformEncryptionKeysV4Response, PlatformEncryptionKeysTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformEncryptionKeysTestDto.class);

    private String credentialName;

    private String region;

    private String platformVariant;

    private String availabilityZone;

    protected PlatformEncryptionKeysTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public PlatformEncryptionKeysTestDto valid() {
        return withPlatformVariant("mock")
                .withRegion("mockRegion")
                .withAvailabilityZone("mockAZ")
                .withCredentialName("mock-credential");
    }

    public String getCredentialName() {
        return credentialName;
    }

    public PlatformEncryptionKeysTestDto withCredentialName(String credentialName) {
        this.credentialName = credentialName;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public PlatformEncryptionKeysTestDto withRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public PlatformEncryptionKeysTestDto withPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
        return this;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public PlatformEncryptionKeysTestDto withAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
