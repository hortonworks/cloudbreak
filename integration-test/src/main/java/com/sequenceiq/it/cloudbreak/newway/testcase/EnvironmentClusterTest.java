package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
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
import com.sequenceiq.it.cloudbreak.newway.entity.EnvironmentSettingsV4Entity;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final String BP_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testDetachFromEnvWithDeletedCluster(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .when(StackActionV4::delete, withoutLogError())
                .await(Status.DELETE_COMPLETED)

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
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(Status.AVAILABLE)

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
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)

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
        testContext.given(EnvironmentSettingsV4Entity.class)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        testContext.get(LdapConfigEntity.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
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
                .when(Environment::post)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .given(StackEntity.class)
                .withName("int-same-env")
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testSameEnvironmentAttachRdsWithDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());

        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class).given(EnvironmentSettingsV4Entity.class)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .given(StackEntity.class)
                .withName("it-same-env-rds")
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testReuseRdsWithDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());

        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .when(Environment::putDetachResources)
                .withName("int-env-reuse")
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext.given(EnvironmentEntity.class)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(RdsConfigEntity.class).getName(),
                        null, null))
                .when(Stack.postV4(), key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testWlClusterChangeCred(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(Status.AVAILABLE)

                .given(CredentialEntity.class)
                .withName("int-change-cred-cl")

                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredentialName(null)
                .withCredential(createCredentialRequest("MOCK", "Change credential", "int-change-cred-cl"))
                .when(Environment::changeCredential)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .validate();
        checkCredentialAttachedToCluster(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(createDefaultRdsConfig(testContext))
                .withLdapConfigs(createDefaultLdapConfig(testContext))
                .withProxyConfigs(createDefaultProxyConfig(testContext))
                .when(Environment::post);
    }

    private void checkCredentialAttachedToCluster(TestContext testContext) {
        testContext.given(StackEntity.class)
                .withName(testContext.get(StackEntity.class).getName())
                .when(Stack::getByName)
                .then(EnvironmentClusterTest::checkCredentialInStack)
                .validate();
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
                .withAmbari(new AmbariEntity(testContext).valid().withBlueprintName(BP_NAME));
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

    private CredentialV4Request createCredentialRequest(String cloudPlatform, String description, String name) {
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setCloudPlatform(cloudPlatform);
        credentialRequest.setDescription(description);
        credentialRequest.setName(name);
        credentialRequest.setMock(new MockCredentialV4Parameters());
        return credentialRequest;
    }

    private static StackEntity checkCredentialInStack(TestContext testContext, StackEntity stack, CloudbreakClient cloudbreakClient) {
        String credentialName = stack.getResponse().getEnvironment().getCredential().getName();
        if (!credentialName.equals(testContext.get(CredentialEntity.class).getName())) {
            throw new TestFailException("Credential is not attached to cluster");
        }
        return stack;
    }

    private static EnvironmentEntity checkEnvHasNoRds(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<DatabaseV4Response> rdsConfigResponseSet = environment.getResponse().getDatabases();
        if (!rdsConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached rds");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvHasNoLdap(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        if (!ldapV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached ldap");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvHasNoProxy(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<ProxyV4Response> proxyV4ResponseSet = environment.getResponse().getProxies();
        if (!proxyV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached proxy");
        }
        return environment;
    }
}