package com.sequenceiq.freeipa.sync.crossrealmtrust;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;

@ExtendWith(MockitoExtension.class)
class CrossRealmTrustStatusSyncServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private TrustStatusValidationService trustStatusValidationService;

    @InjectMocks
    private CrossRealmTrustStatusSyncService underTest;

    private Stack stack;

    private CrossRealmTrust crossRealmTrust;

    @Mock
    private ValidationResult validationResult;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        crossRealmTrust = new CrossRealmTrust();
        when(trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust)).thenReturn(validationResult);
    }

    @Test
    void syncCrossRealmTrustStatusPassed() {
        when(validationResult.hasError()).thenReturn(false);

        underTest.syncCrossRealmTrustStatus(stack, crossRealmTrust);

        verify(crossRealmTrustService).updateTrustStateByStackId(STACK_ID, TrustStatus.TRUST_ACTIVE);
    }

    @Test
    void syncCrossRealmTrustStatusFailed() {
        when(validationResult.hasError()).thenReturn(true);

        underTest.syncCrossRealmTrustStatus(stack, crossRealmTrust);

        verify(crossRealmTrustService).updateTrustStateByStackId(STACK_ID, TrustStatus.TRUST_BROKEN);
    }

}
