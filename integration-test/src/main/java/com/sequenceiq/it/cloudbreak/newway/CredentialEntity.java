package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider.CREDENTIAL_DEFAULT_DESCRIPTION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.MockCloudProvider.MOCK_CAPITAL;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.CredentialV3Action;

@Prototype
public class CredentialEntity extends AbstractCloudbreakEntity<CredentialV4Request, CredentialV4Response, CredentialEntity>
        implements Purgable<CredentialV4Response> {

    public static final String CREDENTIAL = "CREDENTIAL";

    public CredentialEntity(TestContext testContext) {
        super(new CredentialV4Request(), testContext);
    }

    public CredentialEntity() {
        super(CREDENTIAL);
        setRequest(new CredentialV4Request());
    }

    public CredentialEntity valid() {
        MockCredentialV4Parameters credentialParameters = new MockCredentialV4Parameters();
        credentialParameters.setMockEndpoint(getTestContext().getSparkServer().getEndpoint());
        return withName(getNameCreator().getRandomNameForMock())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                    .withMockParameters(credentialParameters)
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

    public CredentialEntity withAwsParameters(AwsCredentialV4Parameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public CredentialEntity withGcpParameters(GcpCredentialV4Parameters gcpParameters) {
        getRequest().setGcp(gcpParameters);
        return this;
    }

    public CredentialEntity withAzureParameters(AzureCredentialV4Parameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public CredentialEntity withOpenstackParameters(OpenstackCredentialV4Parameters openstackParameters) {
        getRequest().setOpenstack(openstackParameters);
        return this;
    }

    public CredentialEntity withYarnParameters(YarnCredentialV4Parameters yarnParameters) {
        getRequest().setYarn(yarnParameters);
        return this;
    }

    public CredentialEntity withCumulusParameters(CumulusYarnCredentialV4Parameters cumulusParameters) {
        getRequest().setCumulus(cumulusParameters);
        return this;
    }

    public CredentialEntity withMockParameters(MockCredentialV4Parameters mockParameters) {
        getRequest().setMock(mockParameters);
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        CredentialV3Action.deleteV2(context, this, cloudbreakClient);
    }

    @Override
    public Collection<CredentialV4Response> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().credentialV4Endpoint().list(client.getWorkspaceId()).getResponses();
    }

    @Override
    public boolean deletable(CredentialV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(CredentialV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().credentialV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 1000;
    }
}
