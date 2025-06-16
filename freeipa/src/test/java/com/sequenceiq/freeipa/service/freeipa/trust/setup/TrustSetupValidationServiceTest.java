package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;

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

    @InjectMocks
    private TrustSetupValidationService underTest;

    @Test
    void testValidateReturnsErrorWhenNoCrossRealmInfoProvided() {
        Long stackId = 1L;
        when(crossRealmTrustService.getByStackIdIfExists(stackId)).thenReturn(Optional.empty());

        ValidationResult result = underTest.validateTrustSetup(stackId);

        assertTrue(result.hasError());
        assertEquals("No cross realm information is provided", result.getErrors().get(0));
    }

    @Test
    void testValidateWhenNoValidationError() {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        ValidationResult validationResult = underTest.validateTrustSetup(4L);
        assertEquals(0L, validationResult.getErrors().size());
    }

    @Test
    void testValidateWhenMissingPackage() throws Exception {
        setup();
        ValidationResult validationResult = underTest.validateTrustSetup(4L);
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("ipa-server-trust-ad package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.",
                validationResult.getErrors().get(0));
    }

    @Test
    void testValidateWhenDnsValidationFailure() throws Exception {
        setup();
        when(packageAvailabilityChecker.isPackageAvailable(4L)).thenReturn(true);
        doThrow(new RuntimeException("DNS validation error")).when(hostOrchestrator).runOrchestratorState(any());

        ValidationResult result = underTest.validateTrustSetup(4L);

        assertEquals(1, result.getErrors().size());
        assertEquals("DNS validation error", result.getErrors().get(0));
    }

    @Test
    void testValidateWhenMultipleFailure() throws Exception {
        setup();
        doThrow(new RuntimeException("DNS validation error")).when(hostOrchestrator).runOrchestratorState(any());

        ValidationResult result = underTest.validateTrustSetup(4L);

        assertEquals(2L, result.getErrors().size());
        assertEquals("DNS validation error", result.getErrors().get(0));
        assertEquals("ipa-server-trust-ad package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.",
                result.getErrors().get(1));
    }

    private void setup() {
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setFqdn("fqdn");
        crossRealmTrust.setIp("ip");
        when(crossRealmTrustService.getByStackIdIfExists(4L)).thenReturn(Optional.of(crossRealmTrust));
        when(stackService.getByIdWithListsInTransaction(4L)).thenReturn(mock(Stack.class));
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
    }
}
