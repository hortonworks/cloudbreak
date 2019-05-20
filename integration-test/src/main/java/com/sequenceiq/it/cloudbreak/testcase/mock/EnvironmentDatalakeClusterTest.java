package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

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

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and then delete",
            when = "create cluster and if available then delete",
            then = "the cluster should work")
    public void testCreateDalalakeDelete(TestContext testContext) {
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .withKerberosConfigs(getKerberosAsList(testContext))
                .when(environmentTestClient.createV4())
                .given(ClusterTestDto.class).valid()
                .withRdsConfigNames(rdsList)
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .withKerberos(testContext.get(KerberosTestDto.class).getName())
                .given("placement", PlacementSettingsTestDto.class)

                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))

                .when(stackTestClient.createV4())
                .when(stackTestClient.deleteV4(), RunningParameter.withoutLogError())
                .await(AbstractIntegrationTest.STACK_DELETED)

                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .withKerberosConfigs(getKerberosAsList(testContext))

                .when(environmentTestClient.detachV4())
                .when(environmentTestClient.getV4())
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
                .then(EnvironmentClusterTest::checkEnvHasNoKerberos)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and then delete resources",
            when = "create cluster and then delete resources",
            then = "the resource deletion does not work because the resources attached to the cluster")
    public void testCreateDalalakeDeleteFails(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .given(ClusterTestDto.class).valid()
                .withRdsConfigNames(rdsList)
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .given("placement", PlacementSettingsTestDto.class)
                .given(StackTestDto.class).withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
                .when(stackTestClient.createV4())
                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV4(), RunningParameter.key(forbiddenKey))
                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), RunningParameter.key(forbiddenKey))
                .deleteGiven(CredentialTestDto.class, credentialTestClient.deleteV4(), RunningParameter.key(forbiddenKey))
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.deleteV4(), RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create two datalake cluster in one environment",
            when = "create cluster called twice",
            then = "the cluster creation should work in both case")
    public void testSameEnvironmentWithDifferentDatalakes(TestContext testContext) {
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList);
        createDatalake(testContext, rdsList);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster in one environment and change credential in environment",
            when = "call create cluster and change credential",
            then = "the credential change will not work")
    public void testDatalakeChangeCredentialFails(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList);
        testContext
                .given("newCred", CredentialTestDto.class).withDescription("Change credential")
                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withCredentialName(null)
                .withCredential("newCred")
                .when(environmentTestClient.changeCredential(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster in one environment and detach resources",
            when = "call create cluster and detach resources",
            then = "will not work because the datalake still up and running")
    public void testDatalakeDetachFails(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
                .validate();
        createDatalake(testContext, rdsList);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.detachV4(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and workload",
            when = "call create cluster with datalake and wiht workload config",
            then = "will work fine")
    public void testSameEnvironmentInDatalakeAndWorkload(TestContext testContext) {
        String dlName = resourcePropertyProvider().getName();
        String hivedb = resourcePropertyProvider().getName();
        String rangerdb = resourcePropertyProvider().getName();
        Set<String> rdsList = createDatalakeResources(testContext, hivedb, rangerdb);
        testContext.given(EnvironmentTestDto.class)
                .withRdsConfigs(rdsList)
                .withLdapConfigs(getLdapAsList(testContext))
                .when(environmentTestClient.createV4())
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
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .given(StackTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .withPlacement("placement")
                .withEnvironment(EnvironmentTestDto.class)
                .withInstanceGroupsEntity(setInstanceGroup(testContext))
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

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapTestDto.class).getName()));
    }

    private Set<String> getKerberosAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(KerberosTestDto.class).getName()));
    }

    private Collection<InstanceGroupTestDto> setInstanceGroup(TestContext testContext) {
        Collection<InstanceGroupTestDto> instanceGroupTestDto = new ArrayList<>();
        InstanceGroupTestDto ig = InstanceGroupTestDto.hostGroup(testContext, HostGroupType.MASTER);
        instanceGroupTestDto.add(ig);
        return instanceGroupTestDto;
    }
}