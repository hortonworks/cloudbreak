package com.sequenceiq.it.cloudbreak.testcase.mock.environment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentDatalakeClusterTest extends AbstractIntegrationTest {

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

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

    // TODO: Update to SDX endpoint
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "Create datalake cluster and then delete",
            when = "create cluster and if available then delete",
            then = "the cluster should work")
    public void testCreateDatalakeDelete(TestContext testContext) {
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(environmentTestClient.create())
                .given(ClusterTestDto.class).valid()
                .withRdsConfigNames(rdsList)
                .given("placement", PlacementSettingsTestDto.class)

                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))

                .when(stackTestClient.createV4())
                .when(stackTestClient.deleteV4(), RunningParameter.withoutLogError())
                .await(AbstractIntegrationTest.STACK_DELETED)

                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())

                .when(environmentTestClient.describe())
                .validate();
    }

    // TODO: Update to new SDX endpoint
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "Create datalake cluster and then delete resources",
            when = "create cluster and then delete resources",
            then = "the resource deletion does not work because the resources attached to the cluster")
    public void testCreateDatalakeDeleteFails(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(environmentTestClient.create())
                .given(ClusterTestDto.class).valid()
                .withRdsConfigNames(rdsList)
                .given("placement", PlacementSettingsTestDto.class)
                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))
                .when(stackTestClient.createV4())
                .given(LdapTestDto.class)
                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV1(), RunningParameter.key(forbiddenKey))
                .given(DatabaseTestDto.class)
                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), RunningParameter.key(forbiddenKey))
                .given(EnvironmentTestDto.class)
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.delete(), RunningParameter.key(forbiddenKey))
                .validate();
    }

    // TODO: Update to SDX endpoints
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "Create two datalake cluster in one environment",
            when = "create cluster called twice",
            then = "the cluster creation should work in both case")
    public void testSameEnvironmentWithDifferentDatalakes(TestContext testContext) {
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .validate();
        createDatalake(testContext, rdsList);
        createDatalake(testContext, rdsList);
    }

    // TODO: Update to SDX endpoints
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "Create datalake cluster and workload",
            when = "call create cluster with datalake and with workload config",
            then = "will work fine")
    public void testSameEnvironmentInDatalakeAndWorkload(TestContext testContext) {
        String dlName = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .given("placement", PlacementSettingsTestDto.class)
                .given(StackTestDto.class)
                .withName(dlName)
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList);
    }

    private void createDatalake(TestContext testContext, Set<String> rdsList) {
        testContext.given("placement", PlacementSettingsTestDto.class)
                .given(ClusterTestDto.class).valid()
                .withRdsConfigNames(rdsList)
                .given(StackTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))
                .when(stackTestClient.createV4())
                .validate();
    }

    private Set<String> createDatalakeResources(TestContext testContext, String hiveDb, String rangerDb) {
        createDefaultLdapConfig(testContext);
        createDefaultKerberosConfig(testContext);
        testContext
                .given(DatabaseTestDto.class)
                .withName(hiveDb)
                .when(databaseTestClient.createV4())
                .withName(rangerDb)
                .withType("RANGER")
                .when(databaseTestClient.createV4());
        Set<String> rdsSet = new HashSet<>();
        rdsSet.add(hiveDb);
        rdsSet.add(rangerDb);
        return rdsSet;
    }
}