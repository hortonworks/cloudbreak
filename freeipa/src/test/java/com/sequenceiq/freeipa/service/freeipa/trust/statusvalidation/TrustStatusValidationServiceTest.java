package com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_DOMAIN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY_ON_ERROR;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;
import static com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService.TRUST_STATUS_VALIDATION_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;

@ExtendWith(MockitoExtension.class)
class TrustStatusValidationServiceTest {

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private TrustStatusValidationService underTest;

    private CrossRealmTrust crossRealmTrust;

    @Mock
    private Stack stack;

    private OrchestratorStateParams stateParams;

    @BeforeEach
    void setUp()  {
        crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setFqdn("ad.hybrid.cloudera.org");
        crossRealmTrust.setRealm("hybrid.cloudera.org");
        stateParams = new OrchestratorStateParams();
        lenient().when(saltStateParamsService.createStateParams(stack, TRUST_STATUS_VALIDATION_STATE, true, MAX_RETRY, MAX_RETRY_ON_ERROR))
                .thenReturn(stateParams);
    }

    @Test
    void validateTrustStatusSuccess() throws Exception {
        doNothing().when(hostOrchestrator).runOrchestratorState(stateParams);

        ValidationResult result = underTest.validateTrustStatus(stack, crossRealmTrust);

        assertThat(result.hasError()).isFalse();
        assertThat(((Map<String, Map<String, String>>) stateParams.getStateParams().get(FREEIPA)).get(TRUST_SETUP_PILLAR))
                .containsEntry(AD_DOMAIN, "ad.hybrid.cloudera.org")
                .containsEntry(REALM, "HYBRID.CLOUDERA.ORG");
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

    @Test
    void validateTrustStatusFailure() throws Exception {
        String message = "message";
        doThrow(new CloudbreakOrchestratorFailedException(message)).when(hostOrchestrator).runOrchestratorState(stateParams);

        ValidationResult result = underTest.validateTrustStatus(stack, crossRealmTrust);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getFormattedErrors()).isEqualTo(message);
    }

}
