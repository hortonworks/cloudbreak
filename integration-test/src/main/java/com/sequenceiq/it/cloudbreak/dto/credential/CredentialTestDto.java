package com.sequenceiq.it.cloudbreak.dto.credential;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.cumulus.CumulusYarnParameters;
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
        withName(getResourceProperyProvider().getName());
        withDescription(getResourceProperyProvider().getDescription("credential"));
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

    public CredentialTestDto withCumulusParameters(CumulusYarnParameters cumulusParameters) {
        getRequest().setCumulus(cumulusParameters);
        return this;
    }

    public CredentialTestDto withMockParameters(MockParameters mockParameters) {
        getRequest().setMock(mockParameters);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(credentialTestClient.delete());
    }

    @Override
    public Collection<CredentialResponse> getAll(EnvironmentClient client) {
        CredentialEndpoint credentialEndpoint = client.getEnvironmentClient().credentialV1Endpoint();
        return new ArrayList<>(credentialEndpoint.list().getResponses());
    }

    @Override
    public void delete(TestContext testContext, CredentialResponse entity, EnvironmentClient client) {
        CredentialEndpoint credentialEndpoint = client.getEnvironmentClient().credentialV1Endpoint();
        credentialEndpoint.deleteByName(entity.getName());
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
