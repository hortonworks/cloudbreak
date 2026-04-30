package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class EncryptionProfileTestDto
        extends DeletableEnvironmentTestDto<EncryptionProfileRequest, EncryptionProfileResponse, EncryptionProfileTestDto, EncryptionProfileResponse> {

    private static final String ENCRYPTION_PROFILE_RESOURCE_NAME = "encryptionProfileNameOrCrn";

    protected EncryptionProfileTestDto(TestContext testContext) {
        super(new EncryptionProfileRequest(), testContext);
    }

    @Override
    protected String name(EncryptionProfileResponse entity) {
        return entity.getName();
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Collection<EncryptionProfileResponse> getAll(EnvironmentClient client) {
        return client.getDefaultClient(getTestContext()).encryptionProfileEndpoint().list().getResponses();
    }

    @Override
    public void delete(TestContext testContext, EncryptionProfileResponse entity, EnvironmentClient client) {
        try {
            client.getDefaultClient(getTestContext()).encryptionProfileEndpoint().deleteByName(entity.getName());
            LOGGER.info("Encryption Profile {} has been deleted successfully.", entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Fail to delete encryption profile: {}  Error: {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public void deleteForCleanup() {
        getClientForCleanup().getDefaultClient(getTestContext()).encryptionProfileEndpoint().deleteByCrn(getCrn());
    }

    @Override
    public String getResourceNameType() {
        return ENCRYPTION_PROFILE_RESOURCE_NAME;
    }

    @Override
    public EncryptionProfileTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()));
        withDescription(getResourcePropertyProvider().getDescription("encryptionProfile"));
        return getCloudProvider().encryptionProfile(this);
    }

    public EncryptionProfileTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public EncryptionProfileTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public EncryptionProfileTestDto withTlsVersions(Set<TlsVersion> tlsVersions) {
        getRequest().setTlsVersions(tlsVersions);
        return this;
    }

    public EncryptionProfileTestDto withCipherSuites(List<String> cipherSuites) {
        getRequest().setCipherSuites(cipherSuites);
        return this;
    }
}
