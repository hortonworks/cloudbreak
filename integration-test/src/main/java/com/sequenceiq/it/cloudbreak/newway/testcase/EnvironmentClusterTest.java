package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.v3.StackV3Action;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private static final String BP_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testCreateWlClusterDelete(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .when(StackV3Action::deleteV2, withoutLogError())
                .await(AbstractIntegrationTest.STACK_DELETED)

                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withRdsConfigs(getRdsAsList(testContext))
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(Environment::putDetachResources)
                .when(Environment::get)
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
                .then(EnvironmentClusterTest::checkEnvHasNoProxy)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testWlClusterNotAttachResourceDetachDeleteOk(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)

                .given(EnvironmentEntity.class)
                .when(Environment::putDetachResources)
                .given(RdsConfigEntity.class)
                .when(RdsConfig::delete)
                .given(LdapConfigEntity.class)
                .when(LdapConfig::delete)
                .given(ProxyConfigEntity.class)
                .when(ProxyConfig::delete)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateWlClusterDeleteFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)

                .deleteGiven(ProxyConfigEntity.class, ProxyConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(LdapConfigEntity.class, LdapConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(RdsConfigEntity.class, RdsConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(CredentialEntity.class, Credential::delete, key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentEntity.class, Environment::delete, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateWlClusterDetachFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(getRdsAsList(testContext))
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(Environment::putDetachResources, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .given(StackEntity.class)
                .withName("int-same-env")
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testSameEnvironmentAttachRdsWithDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());

        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .given(StackEntity.class)
                .withName("it-same-env-rds")
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testReuseRdsWithDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());

        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .when(Environment::putDetachResources)
                .withName("int-env-reuse")
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment("int-env-reuse")
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext.given(EnvironmentEntity.class)
                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment("not-existing-env")
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV2(), key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testWlClusterChangeCred(TestContext testContext) {
        Map<String, Object> parameters = new HashMap<>();
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given(StackEntity.class)
                .withRegion("Europe")
                .withAttachEnvironment(testContext.get(EnvironmentEntity.class).getName())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)

                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredential(createCredentialRequest("MOCK", "Change credential", "int-change-cred", parameters))
                .when(Environment::changeCredential)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(createDefaultRdsConfig(testContext))
                .withLdapConfigs(createDefaultLdapConfig(testContext))
                .withProxyConfigs(createDefaultProxyConfig(testContext))
                .when(Environment::post);
    }

    private Set<String> getProxyAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(ProxyConfigEntity.class).getName()));
    }

    private Set<String> getRdsAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(RdsConfigEntity.class).getName()));
    }

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapConfigEntity.class).getName()));
    }

    private ClusterEntity setResources(TestContext testContext, String rdsName, String ldapName, String proxyName) {
        Set<String> rdsSet = new LinkedHashSet<>();
        rdsSet.add(rdsName);
        ClusterEntity cluster = new ClusterEntity(testContext)
                .valid()
                .withRdsConfigNames(rdsSet)
                .withAmbariRequest(new AmbariEntity(testContext).valid().withBlueprintName(BP_NAME));
        if (rdsName != null) {
            cluster.withRdsConfigNames(rdsSet);
        }
        if (ldapName != null) {
            cluster.withLdapConfigName(ldapName);
        }
        if (rdsName != null) {
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

    private static EnvironmentEntity checkEnvHasNoRds(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<RDSConfigResponse> rdsConfigResponseSet = environment.getResponse().getRdsConfigs();
        if (!rdsConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached rds");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvHasNoLdap(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<LdapConfigResponse> ldapConfigResponseSet = environment.getResponse().getLdapConfigs();
        if (!ldapConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached ldap");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvHasNoProxy(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<ProxyConfigResponse> proxyConfigResponseSet = environment.getResponse().getProxyConfigs();
        if (!proxyConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached proxy");
        }
        return environment;
    }
}