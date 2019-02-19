package com.sequenceiq.it.cloudbreak.newway.testcase;

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

import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

public class EnvironmentDatalakeClusterTest extends AbstractIntegrationTest {

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private static final String BP_NAME_DL = "HDP 3.1 - Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String BP_NAME_WL = "Data Science: Apache Spark 2, Apache Zeppelin";

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDalalakeDelete(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb", "rangerdb");
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given("placement", PlacementSettingsEntity.class)
                .given(StackEntity.class).withPlacement("placement")
                .withEnvironment(EnvironmentEntity.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList,  testContext.get(LdapConfigTestDto.class).getName(), null, BP_NAME_DL))
                .when(Stack.postV4())
                .when(Stack.deleteV4(), withoutLogError())
                .await(AbstractIntegrationTest.STACK_DELETED)

                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::putDetachResources)
                .when(Environment::get)
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDalalakeDeleteFails(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb1", "rangerdb1");
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given("placement", PlacementSettingsEntity.class)
                .given(StackEntity.class).withPlacement("placement")
                .withEnvironment(EnvironmentEntity.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList,  testContext.get(LdapConfigTestDto.class).getName(), null, BP_NAME_DL))
                .when(Stack.postV4())
                .deleteGiven(LdapConfigTestDto.class, ldapConfigTestClient.delete(), key(FORBIDDEN_KEY))
                .deleteGiven(DatabaseEntity.class, DatabaseEntity::delete, key(FORBIDDEN_KEY))
                .deleteGiven(CredentialTestDto.class, Credential::delete, key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentEntity.class, Environment::delete, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testSameEnvironmentWithDifferentDatalakes(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb2", "rangerdb2");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)
                .validate();
        createDatalake(testContext, rdsList, "dl-cluster-same-env", BP_NAME_DL);
        createDatalake(testContext, rdsList, "dl-cluster-same-env2", BP_NAME_DL);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testDatalakeChangeCredentialFails(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb3", "rangerdb3");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)
                .validate();
        createDatalake(testContext, rdsList, "dl-changecred", BP_NAME_DL);
        testContext
                .given("newCred", CredentialTestDto.class).withDescription("Change credential")
                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredentialName(null)
                .withCredential("newCred")
                .when(Environment::changeCredential,  key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testDatalakeDetachFails(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb4", "rangerdb4");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)
                .validate();
        createDatalake(testContext, rdsList, "dl-cluster-detach", BP_NAME_DL);
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::putDetachResources, key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testSameEnvironmentInDatalakeAndWorkload(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb5", "rangerdb5");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given("placement", PlacementSettingsEntity.class)
                .given(StackEntity.class)
                .withName("dl-wl-same-env2")
                .withPlacement("placement")
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, getRdsAsList(testContext),
                        testContext.get(LdapConfigTestDto.class).getName(), null, BP_NAME_WL))
                .when(Stack.postV4())
                .validate();
                createDatalake(testContext, rdsList, "dl-wl-same-env", BP_NAME_DL);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDalalakeWithoutResourcesFails(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)

                .given("placement", PlacementSettingsEntity.class)
                .given(StackEntity.class).withPlacement("placement")
                .withEnvironment(EnvironmentEntity.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, null,  null, null, BP_NAME_DL))
                .when(Stack.postV4(), key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    private void createDatalake(TestContext testContext, Set<String> rdsList, String name, String bpName) {
        testContext.given("placement", PlacementSettingsEntity.class)
                .given(StackEntity.class)
                .withName(name)
                .withPlacement("placement")
                .withEnvironment(EnvironmentEntity.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList, testContext.get(LdapConfigTestDto.class).getName(), null, bpName))
                .when(Stack.postV4())
                .validate();
    }

    private Set<String> createDatalakeResources(TestContext testContext, String hiveDb, String rangerDb) {
        createDefaultLdapConfig(testContext);
        testContext
                .given(DatabaseEntity.class)
                .withName(hiveDb)
                .when(DatabaseEntity.post())
                .withName(rangerDb)
                .withType("RANGER")
                .when(DatabaseEntity.post());
        Set<String> rdsSet = new HashSet<>();
        rdsSet.add(hiveDb);
        rdsSet.add(rangerDb);
        return rdsSet;
    }

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapConfigTestDto.class).getName()));
    }

    private Set<String> getRdsAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(DatabaseEntity.class).getName()));
    }

    private ClusterEntity setResources(TestContext testContext, Set<String> rdsConfigs, String ldapName, String proxyName, String bpName) {

        ClusterEntity cluster = new ClusterEntity(testContext)
                .valid()
                .withRdsConfigNames(rdsConfigs)
                .withAmbari(new AmbariEntity(testContext).valid().withClusterDefinitionName(bpName));
        if (rdsConfigs != null) {
            cluster.withRdsConfigNames(rdsConfigs);
        }
        if (ldapName != null) {
            cluster.withLdapConfigName(ldapName);
        }
        if (proxyName != null) {
            cluster.withProxyConfigName(proxyName);
        }
        return cluster;
    }

    private Collection<InstanceGroupEntity> setInstanceGroup(TestContext testContext) {
        Collection<InstanceGroupEntity> instanceGroupEntity = new ArrayList<>();
        InstanceGroupEntity ig = InstanceGroupEntity.hostGroup(testContext, HostGroupType.MASTER);
        instanceGroupEntity.add(ig);
        return instanceGroupEntity;
    }
}