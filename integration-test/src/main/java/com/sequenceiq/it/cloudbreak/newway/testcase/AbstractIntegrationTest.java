package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.action.BlueprintGetListAction;
import com.sequenceiq.it.cloudbreak.newway.action.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.ImageCatalogCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.proxy.ProxyConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.RdsConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.actor.Actor;
import com.sequenceiq.it.cloudbreak.newway.context.PurgeGarbageService;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = {IntegrationTestConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {

    public static final Map<String, Status> STACK_DELETED = Map.of("status", Status.DELETE_COMPLETED);

    protected static final Map<String, Status> STACK_AVAILABLE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.AVAILABLE);

    protected static final Map<String, Status> STACK_FAILED = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    protected static final Map<String, Status> STACK_STOPPED = Map.of("status", Status.STOPPED, "clusterStatus", Status.STOPPED);

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Value("${integrationtest.cleanup.cleanupBeforeStart:false}")
    private boolean cleanupBeforeStart;

    @Inject
    private RandomNameCreator nameGenerator;

    @BeforeSuite
    public void beforeSuite(ITestContext testngContext) {
        MDC.put("testlabel", "init of " + getClass().getSimpleName());
    }

    @BeforeMethod
    public void beforeMethod(Method method) {
        MDC.put("testlabel", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
    }

    @BeforeClass
    public void createSharedObjects() {
        String testClassName = getClass().getSimpleName();
        MDC.put("testlabel", testClassName);
        if (cleanupBeforeStart) {
            applicationContext.getBean(PurgeGarbageService.class).purge();
        }
    }

    @AfterMethod
    public void afterMethod() {
        MDC.put("testlabel", null);
    }

    @AfterClass(alwaysRun = true)
    public void cleanSharedObjects() {

    }

    @AfterSuite
    public void afterSuite() {

    }

    @DataProvider
    public Object[][] testContext() {
        return new Object[][]{{applicationContext.getBean(TestContext.class)}};
    }

    public RandomNameCreator getNameGenerator() {
        return nameGenerator;
    }

    protected void createDefaultEnvironment(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post);
    }

    protected void createDefaultCredential(TestContext testContext) {
        testContext.given(CredentialEntity.class)
                .when(new CredentialCreateAction());
    }

    protected void createDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogEntity.class)
                .when(new ImageCatalogCreateIfNotExistsAction())
                .when(ImageCatalogEntity::putSetDefaultByName);
    }

    protected Set<String> createDefaultProxyConfig(TestContext testContext) {
        testContext
                .given(ProxyConfigEntity.class)
                .when(new ProxyConfigCreateIfNotExistsAction());
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        return validProxy;
    }

    protected Set<String> createDefaultLdapConfig(TestContext testContext) {
        testContext
                .given(LdapConfigEntity.class)
                .when(new LdapConfigCreateIfNotExistsAction());
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        return validLdap;
    }

    protected Set<String> createDefaultRdsConfig(TestContext testContext) {
        testContext
                .given(RdsConfigEntity.class)
                .when(new RdsConfigCreateIfNotExistsAction());
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        return validRds;
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    protected void createSecondUser(TestContext testContext) {
        testContext.as(Actor::secondUser);
    }

    protected void initializeDefaultBlueprints(TestContext testContext) {
        testContext
                .init(BlueprintEntity.class)
                .when(new BlueprintGetListAction());
    }

    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }
}
