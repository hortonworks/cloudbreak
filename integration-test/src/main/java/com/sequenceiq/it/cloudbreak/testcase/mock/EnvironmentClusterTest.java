package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.EnvironmentSettingsV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the resource delete endpoints and environment delete endpoints are called",
            then = "non of the operations should succeed")
    public void testCreateWlClusterDeleteFails(MockedTestContext testContext) {
        createEnvWithResources(testContext);
        createDefaultRdsConfig(testContext);
        createDefaultLdapConfig(testContext);

        testContext.given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseTestDto.class).getName(),
                                testContext.get(LdapTestDto.class).getName()
                        )
                )
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "calling create cluster with a different cluster name",
            then = "the second cluster should be created")
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        String newStack = resourcePropertyProvider().getName();
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(newStack, StackTestDto.class)
                .when(stackTestClient.createV4(), RunningParameter.key(newStack))
                .await(STACK_AVAILABLE, RunningParameter.key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment and a database which in not attached to the environment",
            when = "a cluster is created in the environment and with the non-attached database",
            then = "the cluster create should succeed")
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseTestDto.class).getName(),
                        null))
                .when(stackTestClient.createV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "create cluster request is sent with missing environment settings",
            then = "a BadRequestException should be returned")
    public void testClusterWithEmptyEnvironmentRequest(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given("invalidEnvironmentSettingsRequest", EnvironmentSettingsV4TestDto.class)
                .withName(null)
                .withCredentialName(null)
                .given(StackTestDto.class)
                .withEnvironmentSettings("invalidEnvironmentSettingsRequest")
                .when(stackTestClient.createV4(), RunningParameter.key("badRequest"))
                .expect(BadRequestException.class, RunningParameter.key("badRequest")
                        .withExpectedMessage(".*CredentialName or EnvironmentName is mandatory"))
                .validate();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4());
    }

    private ClusterTestDto setResources(TestContext testContext, String rdsName, String ldapName) {
        ClusterTestDto cluster = testContext.given(ClusterTestDto.class)
                .valid();
        if (rdsName != null) {
            Set<String> rdsSet = new LinkedHashSet<>();
            rdsSet.add(rdsName);
            cluster.withRdsConfigNames(rdsSet);
        }
        if (ldapName != null) {
            cluster.withLdapConfigName(ldapName);
        }
        return cluster;
    }
}