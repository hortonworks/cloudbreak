package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static org.junit.Assert.assertNotNull;

import java.util.UnknownFormatConversionException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
    private ProxyTestClient proxyTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid http proxy request",
            when = "calling create proxy",
            then = "getting back a list which contains the proxy object")
    public void testCreateValidProxy(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid https proxy request",
            when = "calling create proxy",
            then = "getting back a list which contains the proxy object")
    public void testCreateValidHttpsProxy(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTPS)
                .when(proxyTestClient.createV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with too long name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithTooLongName(TestContext testContext) {
        String name = getLongNameGenerator().stringGenerator(101);
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100")
                                .withKey(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with too short name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithShortName(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(ProxyTestDto.class)
                .withName(SHORT_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100").withKey(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with specific characters in the name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithInvalidName(TestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .withName(INVALID_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(INVALID_PROXY_NAME))
                .expect(UnknownFormatConversionException.class,
                        expectedMessage("Conversion = '|'")
                                .withKey(INVALID_PROXY_NAME))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with empty name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutName(TestContext testContext) {
        String key = "noname";
        testContext
                .given(ProxyTestDto.class)
                .withName("")
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the name has to be in range of 4 to 100")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with too long description",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyLongDesc(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String longDescription = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(longDescription)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTPS)
                .when(proxyTestClient.createV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the description cannot be longer than 1000 character")
                                .withKey(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request without host",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutHost(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String key = "nohost";
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost("")
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the server host has to be in range of 1 to 255")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request without port",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutPort(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String key = "noport";
        testContext
                .given(ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(null)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(key))
                .expect(BadRequestException.class,
                        expectedMessage("Server port is required")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid proxy request",
            when = "calling create proxy then delete that and create again",
            then = "getting back list with proxy which contains the proxy object")
    public void testCreateDeleteCreateAgain(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(name, ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(name))
                .when(proxyTestClient.deleteV4(), key(name))
                .when(proxyTestClient.createV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid proxy request",
            when = "calling create proxy then create again",
            then = "getting a BadRequestException")
    public void testCreateProxyWithSameName(TestContext testContext) {

        String name = resourcePropertyProvider().getName();
        testContext
                .given(name, ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))

                .given(name, ProxyTestDto.class)
                .withName(name)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .when(proxyTestClient.createV4(), key(name))
                .expect(BadRequestException.class,
                        expectedMessage("proxy already exists with name")
                                .withKey(name))
                .validate();
    }
}