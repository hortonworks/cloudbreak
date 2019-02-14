package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.v3.StackV3Action;

public class EnvironmentDatalakeClusterTest extends AbstractIntegrationTest {

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private static final String BP_NAME_DL = "HDP 3.1 - Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String BP_NAME_WL = "Data Science: Apache Spark 2, Apache Zeppelin";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testCreateDalalakeDelete(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb", "rangerdb");
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList,  testContext.get(LdapConfigEntity.class).getName(), null, BP_NAME_DL))
                .when(Stack.postV2())
                .when(StackV3Action::deleteV2, withoutLogError())
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

    @Test(dataProvider = "testContext")
    public void testCreateDalalakeDeleteFails(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb1", "rangerdb1");
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList,  testContext.get(LdapConfigEntity.class).getName(), null, BP_NAME_DL))
                .when(Stack.postV2())
                .deleteGiven(LdapConfigEntity.class, LdapConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(RdsConfigEntity.class, RdsConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(CredentialEntity.class, Credential::delete, key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentEntity.class, Environment::delete, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
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

    @Test(dataProvider = "testContext")
    public void testDatalakeChangeCredentialFails(TestContext testContext) {
        Map<String, Object> parameters = new HashMap<>();
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb3", "rangerdb3");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)
                .validate();
        createDatalake(testContext, rdsList, "dl-changecred", BP_NAME_DL);
        testContext
                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredentialName(null)
                .withCredential(createCredentialRequest("MOCK", "Change credential", "int-change-cred-cl", parameters))
                .when(Environment::changeCredential,  key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
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
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testSameEnvironmentInDatalakeAndWorkload(TestContext testContext) {
        Set<String> rdsList = createDatalakeResources(testContext, "hivedb5", "rangerdb5");
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(Environment::post)

                .given(StackEntity.class)
                .withName("dl-wl-same-env2")
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withCluster(setResources(testContext, getRdsAsList(testContext),
                        testContext.get(LdapConfigEntity.class).getName(), null, BP_NAME_WL))
                .when(Stack.postV2())
                .validate();
                createDatalake(testContext, rdsList, "dl-wl-same-env", BP_NAME_DL);
    }

    @Test(dataProvider = "testContext")
    public void testCreateDalalakeWithoutResourcesFails(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, null,  null, null, BP_NAME_DL))
                .when(Stack.postV2(), key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    private void createDatalake(TestContext testContext, Set<String> rdsList, String name, String bpName) {
        testContext.given(StackEntity.class)
                .withName(name)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .withCluster(setResources(testContext, rdsList, testContext.get(LdapConfigEntity.class).getName(), null, bpName))
                .when(Stack.postV2())
                .validate();
    }

    private Set<String> createDatalakeResources(TestContext testContext, String hiveDb, String rangerDb) {
        createDefaultLdapConfig(testContext);
        testContext
                .given(RdsConfigEntity.class)
                .withName(hiveDb)
                .when(RdsConfig::post)
                .withName(rangerDb)
                .withType("RANGER")
                .when(RdsConfig::post);
        Set<String> rdsSet = new HashSet<>();
        rdsSet.add(hiveDb);
        rdsSet.add(rangerDb);
        return rdsSet;
    }

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapConfigEntity.class).getName()));
    }

    private Set<String> getRdsAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(RdsConfigEntity.class).getName()));
    }

    private ClusterEntity setResources(TestContext testContext, Set<String> rdsConfigs, String ldapName, String proxyName, String bpName) {

        ClusterEntity cluster = new ClusterEntity(testContext)
                .valid()
                .withRdsConfigNames(rdsConfigs)
                .withAmbari(new AmbariEntity(testContext).valid().withBlueprintName(bpName));
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

    private CredentialRequest createCredentialRequest(String cloudPlatform, String description, String name, Map<String, Object> parameters) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCloudPlatform(cloudPlatform);
        credentialRequest.setDescription(description);
        credentialRequest.setName(name);
        credentialRequest.setParameters(parameters);
        return credentialRequest;
    }

    private Collection<InstanceGroupEntity> setInstanceGroup(TestContext testContext) {
        Collection<InstanceGroupEntity> instanceGroupEntity = new ArrayList<>();
        InstanceGroupEntity ig = InstanceGroupEntity.hostGroup(testContext, HostGroupType.MASTER);
        instanceGroupEntity.add(ig);
        return instanceGroupEntity;
    }
}