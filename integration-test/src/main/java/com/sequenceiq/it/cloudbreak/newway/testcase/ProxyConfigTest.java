package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static org.junit.Assert.assertNotNull;

import java.util.UnknownFormatConversionException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ProxyConfigTest extends AbstractIntegrationTest {

    private static final String INVALID_PROXY_NAME = "a-@#$%|:&*;";

    private static final String SHORT_PROXY_NAME = "abc";

    private static final String PROXY_DESCRIPTION = "Valid proxy config description";

    private static final String PROXY_USER = "user";

    private static final String PROXY_PASSWORD = "password";

    private static final String PROXY_HOST = "localhost";

    private static final Integer PROXY_PORT = 8080;

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    @Inject
    private RandomNameCreator randomNameCreator;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((MockedTestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateValidProxy(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateValidHttpsProxy(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTPS)
                .when(ProxyConfig.postV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithTooLongName(TestContext testContext) {
        String name = longStringGeneratorUtil.stringGenerator(101);
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100")
                                .withKey(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithShortName(TestContext testContext) {
        testContext
                .given(ProxyConfigEntity.class)
                .withName(SHORT_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(SHORT_PROXY_NAME))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100")
                                .withKey(SHORT_PROXY_NAME))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithInvalidName(TestContext testContext) {
        testContext
                .given(ProxyConfigEntity.class)
                .withName(INVALID_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(INVALID_PROXY_NAME))
                .expect(UnknownFormatConversionException.class,
                        expectedMessage("Conversion = '|'")
                                .withKey(INVALID_PROXY_NAME))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithoutName(TestContext testContext) {
        String key = "noname";
        testContext
                .given(ProxyConfigEntity.class)
                .withName("")
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyLongDesc(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        String longDescription = longStringGeneratorUtil.stringGenerator(1001);
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(longDescription)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTPS)
                .when(ProxyConfig.postV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the description cannot be longer than 1000 character")
                                .withKey(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithoutHost(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        String key = "nohost";
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost("")
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the server host has to be in range of 1 to 255")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithoutPort(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        String key = "noport";
        testContext
                .given(ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(null)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("Server port is required")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDeleteCreateAgain(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(name, ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(name))
                .when(ProxyConfig.deleteV4(), key(name))
                .when(ProxyConfig.postV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateProxyWithSameName(TestContext testContext) {

        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(name, ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))

                .given(name, ProxyConfigEntity.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(ProxyConfig.postV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("proxy already exists with name")
                                .withKey(name))
                .validate();
    }
}