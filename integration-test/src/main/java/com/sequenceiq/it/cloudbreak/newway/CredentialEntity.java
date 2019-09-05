package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider.CREDENTIAL_DEFAULT_DESCRIPTION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.MockCloudProvider.MOCK_CAPITAL;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.CredentialV3Action;

public class CredentialEntity extends AbstractCloudbreakEntity<CredentialRequest, CredentialResponse, CredentialEntity, CredentialResponse> {

    public static final String CREDENTIAL = "CREDENTIAL";

    public CredentialEntity(TestContext testContext) {
        super(new CredentialRequest(), testContext);
    }

    public CredentialEntity() {
        super(CREDENTIAL);
        setRequest(new CredentialRequest());
    }

    public CredentialEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withParameters(Map.of("mockEndpoint", getTestContext().getSparkServer().getEndpoint()))
                .withCloudPlatform(MOCK_CAPITAL);
    }

    public CredentialEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public CredentialEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public CredentialEntity withCloudPlatform(String cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public CredentialEntity withParameters(Map<String, Object> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        CredentialV3Action.deleteV2(context, this, cloudbreakClient);
    }

    @Override
    public Collection<CredentialResponse> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().credentialV3Endpoint().listByWorkspace(client.getWorkspaceId());
    }

    @Override
    public boolean deletable(CredentialResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(CredentialResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().credentialV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }
}
