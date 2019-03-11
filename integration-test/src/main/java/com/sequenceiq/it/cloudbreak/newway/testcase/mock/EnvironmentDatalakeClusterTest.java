package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class EnvironmentDatalakeClusterTest extends AbstractIntegrationTest {

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private static final String BP_NAME_DL = "HDP 3.1 - Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String BP_NAME_WL = "Data Science: Apache Spark 2, Apache Zeppelin";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and then delete",
            when = "create cluster and if available then delete",
            then = "the cluster should work")
    public void testCreateDalalakeDelete(TestContext testContext) {
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .given(ClusterEntity.class).valid()
                .withRdsConfigNames(rdsList)
                .withClusterDefinitionName(BP_NAME_DL)
                .withAmbari(testContext.given(AmbariEntity.class))
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .given("placement", PlacementSettingsEntity.class)

                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))

                .when(stackTestClient.createV4())
                .when(stackTestClient.deleteV4(), withoutLogError())
                .await(AbstractIntegrationTest.STACK_DELETED)

                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))

                .when(environmentTestClient.detachV4())
                .when(environmentTestClient.getV4())
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and then delete resources",
            when = "create cluster and then delete resources",
            then = "the resource deletion does not work because the resources attached to the cluster")
    public void testCreateDalalakeDeleteFails(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .given(ClusterEntity.class).valid()
                .withRdsConfigNames(rdsList)
                .withClusterDefinitionName(BP_NAME_DL)
                .withAmbari(testContext.given(AmbariEntity.class))
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .given("placement", PlacementSettingsEntity.class)
                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .when(stackTestClient.createV4())
                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV4(), key(forbiddenKey))
                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), key(forbiddenKey))
                .deleteGiven(CredentialTestDto.class, credentialTestClient.deleteV4(), key(forbiddenKey))
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.deleteV4(), key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create two datalake cluster in one environment",
            when = "create cluster called twice",
            then = "the cluster creation should work in both case")
    public void testSameEnvironmentWithDifferentDatalakes(TestContext testContext) {
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList, BP_NAME_DL);
        createDatalake(testContext, rdsList, BP_NAME_DL);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster in one environment and change credential in environment",
            when = "call create cluster and change credential",
            then = "the credential change will not work")
    public void testDatalakeChangeCredentialFails(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList, BP_NAME_DL);
        testContext
                .given("newCred", CredentialTestDto.class).withDescription("Change credential")
                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withCredentialName(null)
                .withCredential("newCred")
                .when(environmentTestClient.changeCredential(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster in one environment and detach resources",
            when = "call create cluster and detach resources",
            then = "will not work because the datalake still up and running")
    public void testDatalakeDetachFails(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList, BP_NAME_DL);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.detachV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and workload",
            when = "call create cluster with datalake and wiht workload config",
            then = "will work fine")
    public void testSameEnvironmentInDatalakeAndWorkload(TestContext testContext) {
        String dlName = getNameGenerator().getRandomNameForResource();
        String hivedb = getNameGenerator().getRandomNameForResource();
        String rangerdb = getNameGenerator().getRandomNameForResource();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .given("placement", PlacementSettingsEntity.class)
                .given(StackTestDto.class)
                .withName(dlName)
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList, BP_NAME_DL);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster with invalid configurations",
            when = "call create cluster",
            then = "will drop ForbiddenException")
    public void testCreateDalalakeWithoutResourcesFails(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(environmentTestClient.createV4())
                .given(ClusterEntity.class).valid()
                .withClusterDefinitionName(BP_NAME_DL)
                .withAmbari(testContext.given(AmbariEntity.class))
                .given("placement", PlacementSettingsEntity.class)
                .given(StackTestDto.class)
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .when(stackTestClient.createV4(), key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    private void createDatalake(TestContext testContext, Set<String> rdsList, String bpName) {
        testContext.given("placement", PlacementSettingsEntity.class)
                .given(ClusterEntity.class).valid()
                .withRdsConfigNames(rdsList)
                .withClusterDefinitionName(BP_NAME_DL)
                .withAmbari(testContext.given(AmbariEntity.class))
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .given(StackTestDto.class)
                .withName(getNameGenerator().getRandomNameForResource())
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .when(stackTestClient.createV4())
                .validate();
    }

    private Set<String> initRdsList(TestContext testContext) {
        String hiveName = getNameGenerator().getRandomNameForResource();
        String rangerName = getNameGenerator().getRandomNameForResource();
        return createDatalakeResources(testContext, hiveName, rangerName);
    }

    private Set<String> createDatalakeResources(TestContext testContext, String hiveDb, String rangerDb) {
        createDefaultLdapConfig(testContext);
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

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapTestDto.class).getName()));
    }

    private Set<String> getRdsAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(DatabaseTestDto.class).getName()));
    }

    private Collection<InstanceGroupEntity> setInstanceGroup(TestContext testContext) {
        Collection<InstanceGroupEntity> instanceGroupEntity = new ArrayList<>();
        InstanceGroupEntity ig = InstanceGroupEntity.hostGroup(testContext, HostGroupType.MASTER);
        instanceGroupEntity.add(ig);
        return instanceGroupEntity;
    }
}