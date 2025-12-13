package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;

public class ProxyConfigTest extends AbstractMockTest {

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
    public void testCreateValidProxy(MockedTestContext testContext) {
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
                .when(proxyTestClient.create())
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
    public void testCreateValidHttpsProxy(MockedTestContext testContext) {
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
                .when(proxyTestClient.create())
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
    public void testCreateProxyWithTooLongName(MockedTestContext testContext) {
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
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The length of the name has to be in range of 4 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with too short name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithShortName(MockedTestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .withName(SHORT_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The length of the name has to be in range of 4 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with specific characters in the name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithInvalidName(MockedTestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .withName(INVALID_PROXY_NAME)
                .withDescription(PROXY_DESCRIPTION)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withServerUser(PROXY_USER)
                .withPassword(PROXY_PASSWORD)
                .withProtocol(HTTP)
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The name can only contain lowercase alphanumeric" +
                        " characters and hyphens and has start with an alphanumeric character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with empty name",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutName(MockedTestContext testContext) {
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
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The length of the name has to be in range of 4 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request with too long description",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyLongDesc(MockedTestContext testContext) {
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
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The length of the description cannot" +
                        " be longer than 1000 character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request without host",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutHost(MockedTestContext testContext) {
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
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("The length of the server host" +
                        " has to be in range of 1 to 255"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid proxy request without port",
            when = "calling create proxy",
            then = "getting back a BadRequestException")
    public void testCreateProxyWithoutPort(MockedTestContext testContext) {
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
                .whenException(proxyTestClient.create(), BadRequestException.class, expectedMessage("Server port is required"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid proxy request",
            when = "calling create proxy then delete that and create again",
            then = "getting back list with proxy which contains the proxy object")
    public void testCreateDeleteCreateAgain(MockedTestContext testContext) {
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
                .when(proxyTestClient.create(), key(name))
                .when(proxyTestClient.delete(), key(name))
                .when(proxyTestClient.create(), key(name))
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
    public void testCreateProxyWithSameName(MockedTestContext testContext) {

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
                .when(proxyTestClient.create(), key(name))
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
                .whenException(proxyTestClient.create(), BadRequestException.class)
                .validate();
    }
}
