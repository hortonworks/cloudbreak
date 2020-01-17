package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsCredentialTest extends AbstractE2ETest {

    @Inject
    private CredentialTestClient credentialTestClient;

    //@Value("${integrationtest.user.rolearn}")
    //private String validRoleArn;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a running cloudbreak",
            when = "an aws credential with invalid roleArn sent",
            then = "a http 400 should return")
    public void testCreateRoleBasedCredentialWithInvalidContent(TestContext testContext) {
        AwsCredentialParameters credentialParameters = new AwsCredentialParameters();
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn("somethingCompletelyInvalid");
        credentialParameters.setRoleBased(roleBasedParameters);
        testContext
                .given(CredentialTestDto.class)
                .withAwsParameters(credentialParameters)
                .withName(resourcePropertyProvider().getName())
                .when(credentialTestClient.create(), key("error"))
                .expect(BadRequestException.class, key("error"))
                .then(AwsCredentialTest::credentialShouldNotBeSaved)
                .validate();
    }

    private static CredentialTestDto credentialShouldNotBeSaved(TestContext tc, CredentialTestDto dto, EnvironmentClient client) {
        client.getEnvironmentClient()
                .credentialV1Endpoint()
                .list()
                .getResponses()
                .stream()
                .filter(response -> nameMatchesWithTheSentCredentialName(response, dto))
                .findAny()
                .ifPresent(credentialResponse -> {
            throw new TestFailException("Credential with name \"" + dto.getRequest().getName() + "\" shouldn't be created!");
        });
        return dto;
    }

    private static boolean nameMatchesWithTheSentCredentialName(CredentialResponse response, CredentialTestDto credentialTestDto) {
        return credentialTestDto.getRequest().getName().equals(response.getName());
    }

}