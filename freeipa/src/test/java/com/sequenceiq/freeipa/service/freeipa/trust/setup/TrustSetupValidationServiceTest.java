package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

@ExtendWith(MockitoExtension.class)
class TrustSetupValidationServiceTest {
    @Mock
    StackService stackService;

    @Mock
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @InjectMocks
    private TrustSetupValidationService underTest;

    private MockedStatic<OrchestratorExceptionAnalyzer> orchestratorExceptionAnalyzer;

    @BeforeEach
    void setupTest() {
        orchestratorExceptionAnalyzer = Mockito.mockStatic(OrchestratorExceptionAnalyzer.class);
    }

    @AfterEach
    void tearDown() {
        orchestratorExceptionAnalyzer.close();
    }

    @Test
    void testValidateReturnsErrorWhenNoCrossRealmInfoProvided() {
        Long stackId = 1L;
        when(crossRealmTrustService.getByStackIdIfExists(stackId)).thenReturn(Optional.empty());

        TaskResults results = underTest.validateTrustSetup(stackId);

        assertTrue(results.hasErrors());
        assertEquals("No cross realm information is provided", results.getErrors().get(0).message());
    }

    @Test
    void testValidateWhenNoValidationError() {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        TaskResults validationResult = underTest.validateTrustSetup(4L);
        assertEquals(0L, validationResult.getErrors().size());
    }

    @Test
    void testValidateWhenMissingPackage() throws Exception {
        setup();
        TaskResults validationResult = underTest.validateTrustSetup(4L);
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("ipa-server-trust-ad package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.",
                validationResult.getErrors().get(0).message());
    }

    @Test
    void testValidateWhenDnsValidationFailure() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        doThrow(new RuntimeException("DNS validation error")).doNothing().when(hostOrchestrator).runOrchestratorState(any());

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1, result.getErrors().size());
        assertEquals("DNS validation error", result.getErrors().get(0).message());
    }

    @Test
    void testValidateWhenDnsValidationFailureWithParams() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error");
        doThrow(orchestratorException).doNothing().when(hostOrchestrator).runOrchestratorState(any());
        Map<String, String> params = Map.of("key1", "value1", "key2", "value2");
        when(OrchestratorExceptionAnalyzer.getNodeErrorParameters(orchestratorException)).thenReturn(params);

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1, result.getErrors().size());
        assertEquals(result.getErrors().get(0).message(), "Dns validation failed: Dns validation error");
        assertEquals(params, result.getErrors().get(0).additionalParams());
    }

    @Test
    void testValidateWhenMultipleFailure() throws Exception {
        setup(false);
        doThrow(new RuntimeException("DNS validation error")).doThrow(new RuntimeException("Reverse DNS validation error"))
                .when(hostOrchestrator).runOrchestratorState(any());

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(4L, result.getErrors().size());
        assertEquals("ipa-server-trust-ad package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.",
                result.getErrors().get(0).message());
        assertEquals("DNS validation error", result.getErrors().get(1).message());
        assertEquals("Reverse DNS validation error", result.getErrors().get(2).message());
        assertEquals("The on premises cluster is not kerberized", result.getErrors().get(3).message());
    }

    @Test
    void testValidateWhenNotKerberized() {
        setup(false);
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1L, result.getErrors().size());
        assertEquals("The on premises cluster is not kerberized", result.getErrors().get(0).message());
    }

    @Test
    void testValidateWhenNoRemoteEnvironment() {
        setup();
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setKdcFqdn("fqdn");
        crossRealmTrust.setKdcIp("ip");
        when(crossRealmTrustService.getByStackIdIfExists(4L)).thenReturn(Optional.of(crossRealmTrust));
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1L, result.getWarnings().size());
        assertEquals("Remote environment crn is missing", result.getWarnings().get(0).message());
    }

    private void setup() {
        setup(true);
    }

    private void setup(boolean kerberized) {
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setKdcFqdn("fqdn");
        crossRealmTrust.setKdcIp("ip");
        crossRealmTrust.setDnsIp("dnsip");
        crossRealmTrust.setRemoteEnvironmentCrn("remoteenvcrn");
        when(crossRealmTrustService.getByStackIdIfExists(4L)).thenReturn(Optional.of(crossRealmTrust));
        when(stackService.getByIdWithListsInTransaction(4L)).thenReturn(mock(Stack.class));
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        lenient().when(remoteEnvironmentEndpoint.getByCrn(any())).thenReturn(createEnvironmentResponse(kerberized));
    }

    private DescribeEnvironmentResponse createEnvironmentResponse(boolean kerberized) {
        KerberosInfo kerberosInfo = new KerberosInfo();
        kerberosInfo.setKerberized(kerberized);
        PrivateDatalakeDetails privateDatalakeDetails = new PrivateDatalakeDetails();
        privateDatalakeDetails.setKerberosInfo(kerberosInfo);
        PvcEnvironmentDetails pvcEnvironmentDetails = new PvcEnvironmentDetails();
        pvcEnvironmentDetails.setPrivateDatalakeDetails(privateDatalakeDetails);
        Environment environment = new Environment();
        environment.setPvcEnvironmentDetails(pvcEnvironmentDetails);
        DescribeEnvironmentResponse describeEnvironmentResponse = new DescribeEnvironmentResponse();
        describeEnvironmentResponse.setEnvironment(environment);
        return describeEnvironmentResponse;
    }
}
