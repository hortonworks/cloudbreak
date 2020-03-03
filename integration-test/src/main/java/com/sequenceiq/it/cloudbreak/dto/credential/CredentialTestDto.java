package com.sequenceiq.it.cloudbreak.dto.credential;

import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class CredentialTestDto extends DeletableEnvironmentTestDto<CredentialRequest, CredentialResponse, CredentialTestDto, CredentialResponse> {

    public static final String CREDENTIAL = "CREDENTIAL";

    @Inject
    private CredentialTestClient credentialTestClient;

    public CredentialTestDto(TestContext testContext) {
        super(new CredentialRequest(), testContext);
    }

    public CredentialTestDto() {
        super(CREDENTIAL);
        setRequest(new CredentialRequest());
    }

    public CredentialTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()));
        withDescription(getResourcePropertyProvider().getDescription("credential"));
        return getCloudProvider().credential(this);
    }

    public CredentialTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public CredentialTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public CredentialTestDto withCloudPlatform(String cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public CredentialTestDto withAwsParameters(AwsCredentialParameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public CredentialTestDto withGcpParameters(GcpCredentialParameters gcpParameters) {
        getRequest().setGcp(gcpParameters);
        return this;
    }

    public CredentialTestDto withAzureParameters(AzureCredentialRequestParameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public CredentialTestDto withOpenstackParameters(OpenstackParameters openstackParameters) {
        getRequest().setOpenstack(openstackParameters);
        return this;
    }

    public CredentialTestDto withYarnParameters(YarnParameters yarnParameters) {
        getRequest().setYarn(yarnParameters);
        return this;
    }

    public CredentialTestDto withMockParameters(MockParameters mockParameters) {
        getRequest().setMock(mockParameters);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("CLEAN UP :: Deleting credential with name: [{}]", getName());
        when(credentialTestClient.delete()).withName(getName());
    }

    @Override
    public Collection<CredentialResponse> getAll(EnvironmentClient client) {
        return client.getEnvironmentClient().credentialV1Endpoint().list().getResponses();
    }

    @Override
    public boolean deletable(CredentialResponse entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, CredentialResponse entity, EnvironmentClient client) {
        try {
            client.getEnvironmentClient().credentialV1Endpoint().deleteByName(entity.getName());
            LOGGER.info("DELETE :: Credential with name: [{}] has been deleted successfully.", entity.getName());
        } catch (Exception e) {
            LOGGER.warn("DELETE :: Something went wrong during [{}] credential delete: {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 1000;
    }

    @Override
    protected String name(CredentialResponse entity) {
        return entity.getName();
    }
}
