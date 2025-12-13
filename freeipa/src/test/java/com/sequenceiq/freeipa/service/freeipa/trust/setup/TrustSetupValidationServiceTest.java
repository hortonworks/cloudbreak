package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.COMMENT;
import static com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustSetupValidationService.DOCS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
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
        orchestratorExceptionAnalyzer = mockStatic(OrchestratorExceptionAnalyzer.class);
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
        assertEquals("Package validation failed",
                validationResult.getErrors().get(0).message());
        Map<String, String> additionalParams = validationResult.getErrors().get(0).additionalParams();
        assertTrue(additionalParams.get(COMMENT).startsWith("Trust setup requires certain packages to be present on the image"));
    }

    @Test
    void testValidateWhenDnsValidationFailure() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        doThrow(new RuntimeException("DNS validation error")).doNothing().when(hostOrchestrator).runOrchestratorState(any());

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals("DNS validation failed", taskResult.message());
        assertEquals("DNS validation error", taskResult.additionalParams().get(COMMENT));
    }

    @Test
    void testValidateWhenDnsValidationFailureWithParams() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error");
        doThrow(orchestratorException).doNothing().when(hostOrchestrator).runOrchestratorState(any());
        Map<String, String> params = Map.of("stdout", "out", "stderr", "err");
        when(OrchestratorExceptionAnalyzer.getHostSaltCommands(orchestratorException)).thenReturn(Set.of(
                new OrchestratorExceptionAnalyzer.HostSaltCommands("host", List.of(new OrchestratorExceptionAnalyzer.SaltCommand("command", params)))));

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals(taskResult.message(), "DNS validation failed");
        assertTrue(taskResult.additionalParams().entrySet().containsAll(params.entrySet()));
        assertTrue(taskResult.additionalParams().get(COMMENT).startsWith("The fully qualified domain name"));
        assertEquals(DocumentationLinkProvider.hybridDnsArchitectureLink(), taskResult.additionalParams().get(DOCS));
    }

    @Test
    void testValidateWhenMultipleFailure() throws Exception {
        setup(false);
        doThrow(new RuntimeException("DNS validation error")).doThrow(new RuntimeException("Reverse DNS validation error"))
                .when(hostOrchestrator).runOrchestratorState(any());

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(4L, result.getErrors().size());
        assertEquals("Package validation failed", result.getErrors().get(0).message());
        assertEquals("DNS validation failed", result.getErrors().get(1).message());
        assertEquals("Reverse DNS validation failed", result.getErrors().get(2).message());
        assertEquals("Security validation failed", result.getErrors().get(3).message());
    }

    @Test
    void testValidateWhenNotKerberized() {
        setup(false);
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1L, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals("Security validation failed", taskResult.message());
        assertTrue(taskResult.additionalParams().get(COMMENT)
                .startsWith("The base cluster selected as a Hybrid Environment Data Lake must be secured by Kerberos"));
        assertEquals(DocumentationLinkProvider.hybridSecurityRequirements(), taskResult.additionalParams().get(DOCS));
    }

    @Test
    void testValidateWhenExceptionDuringKerberized() {
        setup(false);
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        when(remoteEnvironmentEndpoint.getByCrn(any())).thenThrow(new RuntimeException("exception"));

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1L, result.getErrors().size());
        assertEquals("Security validation failed", result.getErrors().get(0).message());
        assertEquals("An error occurred during the kerberization verification: exception",
                result.getErrors().get(0).additionalParams().get(COMMENT));
    }

    @Test
    void testValidateWhenNoRemoteEnvironment() {
        setup();
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setKdcFqdn("fqdn");
        crossRealmTrust.setKdcIp("kdcip");
        crossRealmTrust.setDnsIp("dnsip");
        crossRealmTrust.setKdcType(KdcType.ACTIVE_DIRECTORY);
        when(crossRealmTrustService.getByStackIdIfExists(4L)).thenReturn(Optional.of(crossRealmTrust));
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);

        TaskResults result = underTest.validateTrustSetup(4L);

        assertEquals(1L, result.getErrors().size());
        assertEquals("Security validation failed", result.getErrors().get(0).message());
        assertEquals("Remote environment CRN is missing.\nPlease contact Cloudera support.", result.getErrors().get(0).additionalParams().get(COMMENT));
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
        crossRealmTrust.setKdcType(KdcType.ACTIVE_DIRECTORY);
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
