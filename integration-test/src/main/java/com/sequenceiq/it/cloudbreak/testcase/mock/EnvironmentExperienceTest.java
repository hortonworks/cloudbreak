package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure.ParameterCheck.HAS_THESE_PARAMETERS;
import static com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ExperienceEndpoints.LIFTIE_API_ROOT;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class EnvironmentExperienceTest extends AbstractMockTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env",
            when = "env delete goes",
            then = "liftie endpoint is called")
    public void testDeleteEnvironmentWithExperiences(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .when(environmentTestClient.create())
                .enableVerification(LIFTIE_API_ROOT)
                .enableVerification()
                .awaitForCreationFlow();
        String envName = testContext.given(EnvironmentTestDto.class).getName();
        String crn = testContext.given(EnvironmentTestDto.class).getCrn();
        String tenant = testContext.getActingUserCrn().getAccountId();
        testContext.given(EnvironmentTestDto.class)
                .mockExperience().mockCreateLiftieExperience(envName, this::proc)
                .mockExperience().mockCreateDwxExperience(crn, this::proc)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .mockExperience().dwxExperience().get().crnless().pathVariable("crn", crn).atLeast(1).verify()
                .mockExperience().listLiftieExperience().get().crnless()
                .parameters(Map.of("env", envName, "tenant", tenant), HAS_THESE_PARAMETERS).atLeast(1).verify()
                .validate();
    }

    private void proc(Response res) {
        res.readEntity(String.class);
    }
}
