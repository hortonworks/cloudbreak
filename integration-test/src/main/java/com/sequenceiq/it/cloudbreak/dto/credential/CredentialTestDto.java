package com.sequenceiq.it.cloudbreak.dto.credential;

import java.util.Collection;

import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractEnvironmentTestDto;

@Prototype
public class CredentialTestDto extends AbstractEnvironmentTestDto<CredentialRequest, CredentialResponse, CredentialTestDto> {

    public static final String CREDENTIAL = "CREDENTIAL";

    private String resourceCrn;

    private CredentialResponses credentialResponses;

    public CredentialTestDto(TestContext testContext) {
        super(new CredentialRequest(), testContext);
    }

    public CredentialTestDto() {
        super(CREDENTIAL);
        setRequest(new CredentialRequest());
    }

    public CredentialTestDto valid() {
        withName(resourceProperyProvider().getName());
        withDescription(resourceProperyProvider().getDescription("credential"));
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

//    public CredentialTestDto withAwsParameters(AwsCredentialParameters awsParameters) {
//        getRequest().setAws(awsParameters);
//        return this;
//    }
//
//    public CredentialTestDto withGcpParameters(GcpCredentialParameters gcpParameters) {
//        getRequest().setGcp(gcpParameters);
//        return this;
//    }
//
//    public CredentialTestDto withAzureParameters(AzureCredentialRequestParameters azureParameters) {
//        getRequest().setAzure(azureParameters);
//        return this;
//    }
//
//    public CredentialTestDto withOpenstackParameters(OpenstackParameters openstackParameters) {
//        getRequest().setOpenstack(openstackParameters);
//        return this;
//    }
//
//    public CredentialTestDto withYarnParameters(YarnParameters yarnParameters) {
//        getRequest().setYarn(yarnParameters);
//        return this;
//    }
//
//    public CredentialTestDto withCumulusParameters(CumulusYarnParameters cumulusParameters) {
//        getRequest().setCumulus(cumulusParameters);
//        return this;
//    }

    public CredentialTestDto withMockParameters(MockParameters mockParameters) {
        getRequest().setMock(mockParameters);
        return this;
    }

//    @Override
//    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
//        LOGGER.info("Cleaning up resource with name: {}", getName());
//        CredentialV4Action.deleteV2(context, this, cloudbreakClient);
//    }
//
//    @Override
//    public Collection<CredentialResponse> getAll(CloudbreakClient client) {
//        return client.get().list(client.getWorkspaceId()).getResponses();
//    }
//
//    @Override
//    protected String name(CredentialV4Response entity) {
//        return entity.getName();
//    }
//
//    @Override
//    public void delete(TestContext testContext, CredentialV4Response entity, CloudbreakClient client) {
//        try {
//            client.getCloudbreakClient().credentialV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
//        } catch (ProxyMethodInvocationException e) {
//            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
//        }
//    }

    @Override
    public int order() {
        return 1000;
    }

    protected String name(CredentialResponse entity) {
        return null;
    }

    public Collection<CredentialResponse> getAll(CloudbreakClient client) {
        return null;
    }

    public void delete(TestContext testContext, CredentialResponse entity, CloudbreakClient client) {

    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public CredentialResponses getCredentialResponses() {
        return credentialResponses;
    }

    public void setCredentialResponses(CredentialResponses credentialResponses) {
        this.credentialResponses = credentialResponses;
    }
}
