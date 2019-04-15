package com.sequenceiq.it.cloudbreak.newway.dto.credential;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4RequestParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.newway.v4.CredentialV4Action;

@Prototype
public class CredentialTestDto extends DeletableTestDto<CredentialV4Request, CredentialV4Response, CredentialTestDto, CredentialV4Response> {

    public static final String CREDENTIAL = "CREDENTIAL";

    public CredentialTestDto(TestContext testContext) {
        super(new CredentialV4Request(), testContext);
    }

    public CredentialTestDto() {
        super(CREDENTIAL);
        setRequest(new CredentialV4Request());
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

    public CredentialTestDto withAwsParameters(AwsCredentialV4Parameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public CredentialTestDto withGcpParameters(GcpCredentialV4Parameters gcpParameters) {
        getRequest().setGcp(gcpParameters);
        return this;
    }

    public CredentialTestDto withAzureParameters(AzureCredentialV4RequestParameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public CredentialTestDto withOpenstackParameters(OpenstackCredentialV4Parameters openstackParameters) {
        getRequest().setOpenstack(openstackParameters);
        return this;
    }

    public CredentialTestDto withYarnParameters(YarnCredentialV4Parameters yarnParameters) {
        getRequest().setYarn(yarnParameters);
        return this;
    }

    public CredentialTestDto withCumulusParameters(CumulusYarnCredentialV4Parameters cumulusParameters) {
        getRequest().setCumulus(cumulusParameters);
        return this;
    }

    public CredentialTestDto withMockParameters(MockCredentialV4Parameters mockParameters) {
        getRequest().setMock(mockParameters);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        CredentialV4Action.deleteV2(context, this, cloudbreakClient);
    }

    @Override
    public Collection<CredentialV4Response> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().credentialV4Endpoint().list(client.getWorkspaceId()).getResponses();
    }

    @Override
    protected String name(CredentialV4Response entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, CredentialV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().credentialV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 1000;
    }
}
