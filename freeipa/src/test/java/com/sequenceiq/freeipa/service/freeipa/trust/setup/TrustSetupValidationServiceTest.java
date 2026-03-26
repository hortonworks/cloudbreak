package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.COMMENT;
import static com.sequenceiq.freeipa.service.freeipa.trust.setup.TrustSetupValidationService.DOCS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeValidationResponse;

@ExtendWith(MockitoExtension.class)
class TrustSetupValidationServiceTest {

    private static final long STACK_ID = 4L;

    private static final String KERBERIZED_FAILED = "failed";

    @Mock
    StackService stackService;

    @Mock
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private TrustSetupValidationService underTest;

    private MockedStatic<OrchestratorExceptionAnalyzer> orchestratorExceptionAnalyzer;

    @BeforeEach
    void setupTest() {
        orchestratorExceptionAnalyzer = mockStatic(OrchestratorExceptionAnalyzer.class);
        lenient().when(messagesService.getMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messagesService.getMessageWithArgs(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messagesService.getMessageIfExists(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
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
        assertEquals("trust.validation.notfound", results.getErrors().get(0).message());
    }

    @Test
    void testValidateWhenNoValidationError() {
        setup();
        TaskResults validationResult = underTest.validateTrustSetup(STACK_ID);
        assertEquals(0L, validationResult.getErrors().size());
    }

    @Test
    void testValidateWhenMissingLoadBalancer() throws Exception {
        setup();
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
        TaskResults validationResult = underTest.validateTrustSetup(STACK_ID);
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("trust.validation.loadbalancer.failure", validationResult.getErrors().get(0).message());
        Map<String, String> additionalParams = validationResult.getErrors().get(0).additionalParams();
        assertEquals("trust.validation.loadbalancer.comment", additionalParams.get(COMMENT));
    }

    @Test
    void testValidateWhenMissingPackage() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(STACK_ID)).thenReturn(false);
        TaskResults validationResult = underTest.validateTrustSetup(STACK_ID);
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("trust.validation.packageavailability.failure",
                validationResult.getErrors().get(0).message());
        Map<String, String> additionalParams = validationResult.getErrors().get(0).additionalParams();
        assertEquals("trust.validation.packageavailability.comment", additionalParams.get(COMMENT));
        verify(environmentService).getEnvironmentType(null);
    }

    @Test
    void testValidateWhenDnsValidationFailure() throws Exception {
        setup();
        doThrow(new RuntimeException("DNS validation error")).doNothing().when(hostOrchestrator).runOrchestratorState(any());

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(1, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals("trust.validation.dns failed", taskResult.message());
        assertEquals("DNS validation error", taskResult.additionalParams().get(COMMENT));
        verify(environmentService).getEnvironmentType(null);
    }

    @Test
    void testValidateWhenDnsValidationFailureWithParams() throws Exception {
        setup();
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Dns validation error");
        doThrow(orchestratorException).doNothing().when(hostOrchestrator).runOrchestratorState(any());
        Map<String, String> params = Map.of("stdout", "out", "stderr", "err");
        when(OrchestratorExceptionAnalyzer.getHostSaltCommands(orchestratorException)).thenReturn(Set.of(
                new OrchestratorExceptionAnalyzer.HostSaltCommands("host", List.of(new OrchestratorExceptionAnalyzer.SaltCommand("command", params)))));

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(1, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals("trust.validation.dns failed", taskResult.message());
        assertTrue(taskResult.additionalParams().entrySet().containsAll(params.entrySet()));
        assertEquals("trust.validation.dns.comment", taskResult.additionalParams().get(COMMENT));
        assertEquals(DocumentationLinkProvider.hybridDnsArchitectureLink(), taskResult.additionalParams().get(DOCS));
    }

    @Test
    void testValidateWhenMultipleFailure() throws Exception {
        setup(false);
        when(packageAvailabilityChecker.isPackageAvailable(STACK_ID)).thenReturn(false);
        doThrow(new RuntimeException("DNS validation error")).doThrow(new RuntimeException("Reverse DNS validation error"))
                .when(hostOrchestrator).runOrchestratorState(any());
        setUpEnvironmentType(4L, EnvironmentType.HYBRID);

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(STACK_ID, result.getErrors().size());
        assertEquals("trust.validation.packageavailability.failure", result.getErrors().get(0).message());
        assertEquals("trust.validation.dns failed", result.getErrors().get(1).message());
        assertEquals("trust.validation.reversedns failed", result.getErrors().get(2).message());
        assertEquals(KERBERIZED_FAILED, result.getErrors().get(3).message());
        verify(environmentService).getEnvironmentType(anyString());
    }

    @Test
    void testValidateWhenInvalidCluster() {
        setup(false);
        setUpEnvironmentType(4L, EnvironmentType.HYBRID);

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(1L, result.getErrors().size());
        TaskResult taskResult = result.getErrors().get(0);
        assertEquals(KERBERIZED_FAILED, taskResult.message());
        assertEquals("trust.validation.KERBERIZED.comment", taskResult.additionalParams().get(COMMENT));
        assertEquals(DocumentationLinkProvider.hybridSecurityRequirements(), taskResult.additionalParams().get(DOCS));
    }

    @Test
    void testValidateWhenExceptionDuringDatalakeValidation() {
        setup(false);
        RuntimeException e = new RuntimeException("exception");
        when(remoteEnvironmentEndpoint.validateForDatalake(any())).thenThrow(e);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn("extractedMessage");
        setUpEnvironmentType(4L, EnvironmentType.HYBRID);

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(1L, result.getErrors().size());
        assertEquals("trust.validation.datalake.failure", result.getErrors().get(0).message());
        assertFalse(result.getErrors().get(0).additionalParams().containsKey(COMMENT));
    }

    @Test
    void testValidateWhenNoRemoteEnvironment() {
        setup();
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setKdcFqdn("fqdn");
        crossRealmTrust.setKdcIp("kdcip");
        crossRealmTrust.setDnsIp("dnsip");
        crossRealmTrust.setKdcType(KdcType.ACTIVE_DIRECTORY);
        setUpEnvironmentType(STACK_ID, EnvironmentType.HYBRID);
        when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.of(crossRealmTrust));

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertEquals(1L, result.getErrors().size());
        assertEquals("trust.validation.datalake.failure", result.getErrors().get(0).message());
        assertEquals("trust.validation.datalake.comment.missingcrn", result.getErrors().get(0).additionalParams().get(COMMENT));
        verify(environmentService).getEnvironmentType(anyString());
    }

    @Test
    void testValidateWhenClusterValidationPassedWithEmptyMessage() {
        setup();
        setUpEnvironmentType(4L, EnvironmentType.HYBRID);
        ValidateForDatalakeResponse validateForDatalakeResponse = new ValidateForDatalakeResponse();
        validateForDatalakeResponse.setValid(true);
        ValidateForDatalakeValidationResponse validation = new ValidateForDatalakeValidationResponse();
        validation.setPassed(true);
        validation.setValidationType("KERBERIZED");
        validation.setMessage("");
        validateForDatalakeResponse.setValidations(List.of(validation));
        when(remoteEnvironmentEndpoint.validateForDatalake(any())).thenReturn(validateForDatalakeResponse);

        TaskResults result = underTest.validateTrustSetup(STACK_ID);

        assertTrue(result.taskResults().stream().allMatch(taskResult -> StringUtils.isNotEmpty(taskResult.message())));
    }

    private void setUpEnvironmentType(long stackId, EnvironmentType environmentType) {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        String envCrn = "crn";
        when(stack.getEnvironmentCrn()).thenReturn(envCrn);
        when(environmentService.getEnvironmentType(envCrn)).thenReturn(environmentType);
    }

    private void setup() {
        setup(true);
    }

    private void setup(boolean validForDatalake) {
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setKdcFqdn("fqdn");
        crossRealmTrust.setKdcIp("ip");
        crossRealmTrust.setDnsIp("dnsip");
        crossRealmTrust.setRemoteEnvironmentCrn("remoteenvcrn");
        crossRealmTrust.setKdcType(KdcType.ACTIVE_DIRECTORY);
        when(packageAvailabilityChecker.isPackageAvailable(STACK_ID)).thenReturn(true);
        when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.of(crossRealmTrust));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(mock(Stack.class));
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(mock()));
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        ValidateForDatalakeResponse validateForDatalakeResponse = new ValidateForDatalakeResponse();
        validateForDatalakeResponse.setValid(validForDatalake);
        ValidateForDatalakeValidationResponse validation = new ValidateForDatalakeValidationResponse();
        validation.setPassed(validForDatalake);
        validation.setValidationType("KERBERIZED");
        validation.setMessage(validForDatalake ? "success" : KERBERIZED_FAILED);
        validateForDatalakeResponse.setValidations(List.of(validation));
        lenient().when(remoteEnvironmentEndpoint.validateForDatalake(any())).thenReturn(validateForDatalakeResponse);
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
