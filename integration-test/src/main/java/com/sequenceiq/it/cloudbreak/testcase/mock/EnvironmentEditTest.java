package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class EnvironmentEditTest extends AbstractMockTest {

    private static final String PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
            + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
            + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
            + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
            + "KR495VFmuOepLYz5I8Dn sequence-eu";

    private static final String NEW_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
            + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
            + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
            + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
            + "KR495VFmuOepLrttyyt sequence-eu";

    private static final String INVALID_PUBLIC_KEY = "invalid-ssh-rsa "
            + "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
            + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
            + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
            + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
            + "KR495VFmuOepLYz5I8Dn sequence-eu";

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
    public void authenticationEditWhenSetExistingKeyAndDeleteManagedSuccessfully(MockedTestContext testContext) {
        String randomPublicKeyId = UUID.randomUUID().toString();
        testContext
                .given(HttpMock.class)
                .mockSpi().getPublicKey().get()
                .pathVariable("publicKeyId", randomPublicKeyId)
                .thenReturn(Map.of("publicKeyId", randomPublicKeyId, "publicKey", "asd"))

                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId(randomPublicKeyId)
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication())
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    String publicKeyId = t.getResponse().getAuthentication().getPublicKeyId();
                    String publicKey = t.getResponse().getAuthentication().getPublicKey();
                    if (!randomPublicKeyId.equals(publicKeyId)) {
                        throw new TestFailException("The auth public key id was not changed, but it should be changed");
                    }
                    if (publicKey != null) {
                        throw new TestFailException("The auth public key should be null");
                    }
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with existed ssh key",
            when = "change existing ssh key to managed one",
            then = "delete managed ssh key but not create new one")
    public void authenticationEditWhenSetManagedKeyAndNotDeleteExisted(MockedTestContext testContext) {
        String randomPublicKeyId = UUID.randomUUID().toString();
        testContext
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId(randomPublicKeyId)
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKey(PUBLIC_KEY)
                .withPublicKeyId(null)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication())
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    String publicKeyId = t.getResponse().getAuthentication().getPublicKeyId();
                    String publicKey = t.getResponse().getAuthentication().getPublicKey();
                    if (randomPublicKeyId.equals(publicKeyId)) {
                        throw new TestFailException("The auth public key id was not changed, but it should be changed");
                    }
                    if (publicKey == null) {
                        throw new TestFailException("The auth public key should not be null");
                    }
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "change authentication",
            then = "get validation errors")
    public void authenticationEditValidationErrors(MockedTestContext testContext) {
        String value = UUID.randomUUID().toString();
        String errorPattern = String.format(".*'%s'.*\\s.*The uploaded SSH Public Key is invalid.*"
                + "\\s.*ecdsa-sha2-nistp384.*\\s.*either publicKey or publicKeyId.*", value);
        testContext
                .given(HttpMock.class)
                .mockSpi().getPublicKey().get()
                .pathVariable("publicKeyId", value)
                .thenReturn(null)

                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId(value)
                .withPublicKey(INVALID_PUBLIC_KEY)
                .given(EnvironmentTestDto.class)
                .whenException(environmentTestClient.changeAuthentication(), BadRequestException.class, expectedMessage(errorPattern))
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKeyId(null)
                .withPublicKey(null)
                .given(EnvironmentTestDto.class)
                .whenException(environmentTestClient.changeAuthentication(), BadRequestException.class,
                        expectedMessage("You should define either the publicKey or the publicKeyId."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with existed ssh key",
            when = "change ssh key",
            then = "update to new ssh key")
    public void authenticationEditWhenSetPublicKeyAndNotManaged(MockedTestContext testContext) {
        testContext
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKey(PUBLIC_KEY)
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentAuthenticationTestDto.class)
                .withPublicKey(PUBLIC_KEY)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.changeAuthentication())
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    String publicKey = t.getResponse().getAuthentication().getPublicKey();
                    if (NEW_PUBLIC_KEY.equals(publicKey)) {
                        throw new TestFailException("The auth public key must be equals");
                    }
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "change authentication",
            then = "get validation errors")
    public void securityAccessEditValidationErrors(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentSecurityAccessTestDto.class)
                .withCidr("151.151.0.0/16")
                .given(EnvironmentTestDto.class)
                .withSecurityAccess()
                .whenException(environmentTestClient.changeSecurityAccess(), BadRequestException.class,
                        expectedMessage(
                                "1. Please add the default or knox security groups, we cannot edit with empty value.\n" +
                                "2. The CIDR can be replaced with the default and knox security groups, please add to the request\n" +
                                "3. The CIDR could not be updated in the environment"))

                .given(EnvironmentSecurityAccessTestDto.class)
                .withCidr("10.blahblah")
                .given(EnvironmentTestDto.class)
                .withSecurityAccess()
                .whenException(environmentTestClient.changeSecurityAccess(), BadRequestException.class,
                        expectedMessage(
                                "The format of the CIDR is not accepted."))

                .given(EnvironmentSecurityAccessTestDto.class)
                .withCidr("10.")
                .given(EnvironmentTestDto.class)
                .withSecurityAccess()
                .whenException(environmentTestClient.changeSecurityAccess(), BadRequestException.class,
                        expectedMessage(
                                "^More than one validation errors happened: \\n" +
                                "(The format of the CIDR is not accepted.\\nThe list of CIDRs must consist of characters between 5 and 4000|" +
                                "The list of CIDRs must consist of characters between 5 and 4000\\nThe format of the CIDR is not accepted\\.)+$"))

                .validate();
    }
}
