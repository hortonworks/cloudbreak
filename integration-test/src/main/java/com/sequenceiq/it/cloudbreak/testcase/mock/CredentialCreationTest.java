package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CredentialCreationTest extends AbstractIntegrationTest {

    @Inject
    private CredentialTestClient credentialTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a running cloudbreak",
            when = "an aws credential with too short roleArn sent",
            then = "a http 400 should return")
    public void createClusterWithoutNetworkAndPlacementShouldBadRequestException(MockedTestContext testContext) {
        AwsCredentialParameters credentialParameters = new AwsCredentialParameters();
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("tooshort");
        credentialParameters.setRoleBased(roleBasedParameters);
        testContext
                .given(CredentialTestDto.class)
                .withAwsParameters(credentialParameters)
                .withName(resourcePropertyProvider().getName())
                .when(credentialTestClient.create(), key("error"))
                .expect(BadRequestException.class, key("error"))
                .validate();
    }

}
