package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.CheckCount;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.SpiEndpoints;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentEditTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak managed ssh key",
            when = "change managed ssh key to existing one",
            then = "delete managed ssh key but not create new one")
    public void authenticationEditWhenSetExistingKeyAndDeleteManagedSuccessfully(TestContext testContext) {
        testContext
                .given(HttpMock.class).whenRequested(SpiEndpoints.RegisterPublicKey.class).post()
                .thenReturn((s, model, uriParameters) -> "")
                .whenRequested(SpiEndpoints.GetPublicKey.class).get()
                .thenReturn((s, model, uriParameters) -> "true")
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)

                .given(HttpMock.class).whenRequested(SpiEndpoints.UnregisterPublicKey.class).post().clearCalls()
                .whenRequested(SpiEndpoints.UnregisterPublicKey.class).post()
                .thenReturn((s, model, uriParameters) -> "")
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId("existing-public-key")
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication())
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    String publicKeyId = t.getResponse().getAuthentication().getPublicKeyId();
                    String publicKey = t.getResponse().getAuthentication().getPublicKey();
                    if (!"existing-public-key".equals(publicKeyId)) {
                        throw new TestFailException("The auth public key id was not changed, but it should be changed");
                    }
                    if (publicKey != null) {
                        throw new TestFailException("The auth public key should be null");
                    }
                    return t;
                })
                .given(HttpMock.class).whenRequested(SpiEndpoints.UnregisterPublicKey.class).post().verify(CheckCount.times(1))
                .given(HttpMock.class).whenRequested(SpiEndpoints.RegisterPublicKey.class).post().verify(CheckCount.times(0))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with existed ssh key",
            when = "change existing ssh key to managed one",
            then = "delete managed ssh key but not create new one")
    public void authenticationEditWhenSetManagedKeyAndNotDeleteExisted(TestContext testContext) {
        testContext
                .given(HttpMock.class)
                .whenRequested(SpiEndpoints.GetPublicKey.class).get()
                .thenReturn((s, model, uriParameters) -> "true")
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId("existing-public-key")
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)

                .given(HttpMock.class)
                .whenRequested(SpiEndpoints.RegisterPublicKey.class).post()
                .thenReturn((s, model, uriParameters) -> "")
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKey("some-ssh-key")
                .withPublicKeyId(null)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication())
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    String publicKeyId = t.getResponse().getAuthentication().getPublicKeyId();
                    String publicKey = t.getResponse().getAuthentication().getPublicKey();
                    if ("existing-public-key".equals(publicKeyId)) {
                        throw new TestFailException("The auth public key id was not changed, but it should be changed");
                    }
                    if (publicKey == null) {
                        throw new TestFailException("The auth public key should not be null");
                    }
                    return t;
                })
                .given(HttpMock.class).whenRequested(SpiEndpoints.UnregisterPublicKey.class).post().verify(CheckCount.times(0))
                .given(HttpMock.class).whenRequested(SpiEndpoints.RegisterPublicKey.class).post().verify(CheckCount.times(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "change authentication",
            then = "get validation errors")
    public void authenticationEditValidationErrors(TestContext testContext) {
        testContext
                .given(HttpMock.class).whenRequested(SpiEndpoints.GetPublicKey.class).get()
                .thenReturn((s, model, uriParameters) -> {
                    return "false";
                })
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)

                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId("non-exists-public-key")
                .withPublicKey("not-empty-public-key")
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication(), key("all-defined"))
                .expect(BadRequestException.class, key("all-defined")
                        .withExpectedMessage("1. The publicKeyId with name of 'non-exists-public-key' does not exists on the provider\n" +
                                "2. You should define either publicKey or publicKeyId only"))

                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId(null)
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication(), key("non-defined"))
                .expect(BadRequestException.class, key("non-defined")
                        .withExpectedMessage("1. You should define publicKey or publicKeyId"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "change authentication",
            then = "get validation errors")
    public void securityAccessEditValidationErrors(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)

                .given(EnvironmentSecurityAccessTestDto.class)
                .withCidr("cidr")
                .given(EnvironmentTestDto.class)
                .withSecurityAccess()
                .when(environmentTestClient.changeSecurityAccess(), key("cidr-defined"))
                .expect(BadRequestException.class, key("cidr-defined")
                        .withExpectedMessage("1. Please add the default or knox security groups, we cannot edit with empty value.\n" +
                                "2. The CIDR can be replaced with the default and knox security groups, please add to the request\n" +
                                "3. The CIDR could not be updated in the environment"))
                .validate();
    }
}
