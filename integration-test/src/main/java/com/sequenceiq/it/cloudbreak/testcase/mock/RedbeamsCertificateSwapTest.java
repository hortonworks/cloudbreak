package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.RUNNING;
import static java.lang.Boolean.FALSE;
import static java.util.regex.Pattern.DOTALL;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCreateAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsCertificateSwapAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentStartAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentStopAction;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CertificateSwapTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class RedbeamsCertificateSwapTest extends AbstractMockTest {

    private static final Class<CertificateSwapTestDto> A_CERTIFICATE_SWAP_REQUEST = CertificateSwapTestDto.class;

    private static final Class<EnvironmentTestDto> AN_ENVIRONMENT = EnvironmentTestDto.class;

    private static final Class<FreeIpaTestDto> A_FREEIPA = FreeIpaTestDto.class;

    private static final Class<SdxInternalTestDto> A_DATALAKE = SdxInternalTestDto.class;

    private static final Class<DistroXTestDto> A_DATAHUB = DistroXTestDto.class;

    private static final RedbeamsCertificateSwapAction REDBEAMS_CERTIFICATE_SWAP_REQUESTED = new RedbeamsCertificateSwapAction();

    private static final EnvironmentCreateAction ENVIRONMENT_CREATE_REQUESTED = new EnvironmentCreateAction();

    private static final FreeIpaCreateAction FREEIPA_CREATE_REQUESTED = new FreeIpaCreateAction();

    private static final SdxCreateInternalAction DATALAKE_CREATE_REQUESTED = new SdxCreateInternalAction();

    private static final DistroXCreateAction DATAHUB_CREATE_REQUESTED = new DistroXCreateAction();

    private static final EnvironmentStopAction ENVIRONMENT_STOP_REQUESTED = new EnvironmentStopAction();

    private static final EnvironmentStartAction ENVIRONMENT_START_REQUESTED = new EnvironmentStartAction();

    private static final String REGEX_TWO_CERTS_IN_ROOT_CERTS_SLS = "root-certs\\.sls.*(-----BEGIN CERTIFICATE-----.*){2}";

    private static final String REGEX_ONE_CERT_IN_ROOT_CERTS_SLS = "root-certs\\.sls.*(-----BEGIN CERTIFICATE-----.*){1}";

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment datalake and redbeams with ssl configuration stopped",
            when = "start",
            then = "redbeams certificate is sent again to the datalake through salt pillar")
    public void testRedbeamsCertificateSwap(TestContext testContext) {
        givenRedbeamsServerWithOneCertificate(testContext);
        givenACreatedEnvironment(testContext);
        givenACreatedFreeipa(testContext);
        givenACreatedDatalake(testContext);
        givenACreatedDatahub(testContext);
        givenTheEnvironmentStopped(testContext);
        whenTheEnvironmentHasStarted(testContext);
        thenOnlyOneCertificateIsPresentInTheRootCertsPillar(testContext);
        resetMocksSavedCalls(testContext);
        givenRedbeamsServerWithTwoCertificate(testContext);
        givenTheEnvironmentStopped(testContext);
        whenTheEnvironmentHasStarted(testContext);
        thenTwoCertificatesArePresentInTheRootCertsPillar(testContext);
        testContext.given(EnvironmentTestDto.class).validate();
    }

    private void thenOnlyOneCertificateIsPresentInTheRootCertsPillar(TestContext testContext) {
        thenCertificateIsPresentInRootCertsSaltPillar(testContext, REGEX_ONE_CERT_IN_ROOT_CERTS_SLS);
    }

    private void thenTwoCertificatesArePresentInTheRootCertsPillar(TestContext testContext) {
        thenCertificateIsPresentInRootCertsSaltPillar(testContext, REGEX_TWO_CERTS_IN_ROOT_CERTS_SLS);
    }

    private void givenRedbeamsServerWithOneCertificate(TestContext testContext) {
        givenRedbeamsWithCerts(testContext, false);
    }

    private void givenRedbeamsServerWithTwoCertificate(TestContext testContext) {
        givenRedbeamsWithCerts(testContext, true);
    }

    private void givenRedbeamsWithCerts(TestContext testContext, Boolean secondCert) {
        testContext
                .given(A_CERTIFICATE_SWAP_REQUEST)
                .withFirstCertificate(true)
                .withSecondCertificate(secondCert)
                .when(REDBEAMS_CERTIFICATE_SWAP_REQUESTED);
    }

    private void givenACreatedEnvironment(TestContext testContext) {
        testContext.given(AN_ENVIRONMENT)
                .withNetwork()
                .withCreateFreeIpa(FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(ENVIRONMENT_CREATE_REQUESTED)
                .await(EnvironmentStatus.AVAILABLE);
    }

    private void givenACreatedFreeipa(TestContext testContext) {
        testContext.given(A_FREEIPA)
                .when(FREEIPA_CREATE_REQUESTED)
                .await(AVAILABLE);
    }

    private void givenACreatedDatalake(TestContext testContext) {
        testContext.given(A_DATALAKE)
                .withDatabase(datalakeDatabaseRequest())
                .when(DATALAKE_CREATE_REQUESTED)
                .await(RUNNING)
                .enableVerification();
    }

    private void givenACreatedDatahub(TestContext testContext) {
        testContext.given(A_DATAHUB)
                .when(DATAHUB_CREATE_REQUESTED)
                .await(STACK_AVAILABLE)
                .enableVerification();
    }

    private void givenTheEnvironmentStopped(TestContext testContext) {
        testContext.given(AN_ENVIRONMENT)
                .when(ENVIRONMENT_STOP_REQUESTED)
                .await(ENV_STOPPED);
    }

    private void whenTheEnvironmentHasStarted(TestContext testContext) {
        testContext.given(AN_ENVIRONMENT)
                .when(ENVIRONMENT_START_REQUESTED)
                .await(EnvironmentStatus.AVAILABLE);
    }

    private void thenCertificateIsPresentInRootCertsSaltPillar(TestContext testContext, String regex) {
        testContext.given(A_DATALAKE)
                .mockSalt().saltPillarDistribute().post().bodyCheck(checkRegexInBody(regex)).verify()
                .given(A_DATAHUB)
                .mockSalt().saltPillarDistribute().post().bodyCheck(checkRegexInBody(regex)).verify();
    }

    private void resetMocksSavedCalls(TestContext testContext) {
        testContext.given(A_DATALAKE)
                .disableVerification()
                .enableVerification()
                .given(A_DATAHUB)
                .disableVerification()
                .enableVerification();
    }

    private SdxDatabaseRequest datalakeDatabaseRequest() {
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        return dbRequest;
    }

    private Predicate<String> checkRegexInBody(String regex) {
        return (body) -> {
            Pattern pattern = Pattern.compile(regex, DOTALL);
            Matcher matcher = pattern.matcher(body);
            return matcher.find();
        };
    }
}
