package com.sequenceiq.it.cloudbreak.dto.credential;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class CredentialTestDto extends DeletableEnvironmentTestDto<CredentialRequest, CredentialResponse, CredentialTestDto, CredentialResponse> {

    public static final String CREDENTIAL = "CREDENTIAL";

    private static final String CREDENTIAL_RESOURCE_NAME = "credentialName";

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

    @Override
    public String getResourceNameType() {
        return CREDENTIAL_RESOURCE_NAME;
    }

    public EditCredentialRequest modifyRequest() {
        EditCredentialRequest editRequest = new EditCredentialRequest();
        CredentialRequest request = getRequest();

        editRequest.setName(request.getName());
        editRequest.setAzure(request.getAzure());
        editRequest.setAws(request.getAws());
        editRequest.setGcp(request.getGcp());
        editRequest.setMock(request.getMock());
        editRequest.setOpenstack(request.getOpenstack());
        editRequest.setYarn(request.getYarn());
        editRequest.setCloudPlatform(request.getName());
        editRequest.setDescription(request.getName());
        editRequest.setCloudPlatform(getCloudPlatform().name());
        return editRequest;
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
    public void cleanUp(TestContext context, MicroserviceClient client) {
        LOGGER.info("Cleaning up credential with name: {}", getName());
        if (getResponse() != null) {
            when(credentialTestClient.delete(), key("delete-credential-" + getName()).withSkipOnFail(false));
        } else {
            LOGGER.info("Credential: {} response is null!", getName());
        }
    }

    @Override
    public Collection<CredentialResponse> getAll(EnvironmentClient client) {
        return client.getDefaultClient().credentialV1Endpoint().list().getResponses();
    }

    @Override
    public boolean deletable(CredentialResponse entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, CredentialResponse entity, EnvironmentClient client) {
        try {
            client.getDefaultClient().credentialV1Endpoint().deleteByName(entity.getName());
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

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }
}
