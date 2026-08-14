package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_STARTED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.EventAssertionCommon;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.StructuredEventUtil;

public class EnvironmentEditTest extends AbstractMockTest {

    private static final String FREEIPA_IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

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

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EventAssertionCommon eventAssertionCommon;

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
            given = "there is a running cloudbreak environment with FreeIPA, a Data Lake and a Data Hub, " +
                    "and the mock cloud provider reports a new secondary CIDR",
            when = "edit is called with refreshNetwork=true and no network payload",
            then = "the EnvNetworkCidrsModification flow chain runs to completion, notification events are emitted "
                    + "and the environment's networkCidrs are refreshed from the mock")
    public void editWithRefreshNetworkTriggersNetworkCidrsModificationFlow(MockedTestContext testContext) {
        String initialCidr = "192.168.0.0/16";
        String secondaryCidr = "10.0.0.0/16";
        createDefaultImageCatalog(testContext);
        testContext
                .given(HttpMock.class)
                .mockSpi().getNetworkCidr().get().clearMockResponse()
                .mockSpi().getNetworkCidr().get().thenReturn(Map.of("cidr", initialCidr, "cidrs", List.of(initialCidr)), null, 200, 0, null)

                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl(), FREEIPA_IMAGE_CATALOG_ID)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()

                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .awaitForFlow()

                .given(DistroXTestDto.class)
                .withEnvironment()
                .when(distroXTestClient.create())
                .awaitForFlow()

                .given(HttpMock.class)
                .mockSpi().getNetworkCidr().get().clearMockResponse()
                .mockSpi().getNetworkCidr().get().thenReturn(Map.of("cidr", initialCidr, "cidrs", List.of(initialCidr, secondaryCidr)), null, 200, 0, null)

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.refreshNetwork())
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    if (t.getResponse().getNetwork() == null) {
                        throw new TestFailException("Environment network should not be null after refreshNetwork edit");
                    }
                    if (t.getResponse().getNetwork().getNetworkCidrs() == null || !t.getResponse().getNetwork().getNetworkCidrs().contains(secondaryCidr)) {
                        throw new TestFailException("Environment networkCidrs should contain the new CIDR '" + secondaryCidr
                                + "' reported by the mock cloud provider, but was " + t.getResponse().getNetwork().getNetworkCidrs());
                    }
                    eventAssertionCommon.checkNotificationEvents(
                            StructuredEventUtil.getAuditEvents(
                                    tc.getMicroserviceClient(EnvironmentClient.class).getDefaultClient(tc).structuredEventsV1Endpoint(),
                                    t.getResponse().getCrn()),
                            List.of(
                                    ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_STARTED,
                                    ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_STARTED,
                                    ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_STARTED,
                                    ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FINISHED
                            ));
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
                        expectedMessage("1. Please add the default or knox security groups, we cannot edit with empty value.\n" +
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
                        expectedMessage("^More than one validation errors happened: \\n" +
                                "(The format of the CIDR is not accepted.\\nThe list of CIDRs must consist of characters between 5 and 4000|" +
                                "The list of CIDRs must consist of characters between 5 and 4000\\nThe format of the CIDR is not accepted\\.)+$"))

                .validate();
    }
}
