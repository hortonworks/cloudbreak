package com.sequenceiq.freeipa.service.rotation.saltpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeipaSaltPasswordContextProviderTest {

    private static final String ACCOUNT_ID = "accid";

    private static final String ENV_CRN = String.format("crn:cdp:environments:us-west-1:%s:environment:da337ac7-82ef-4f6c-a13c-45aa6960282d", ACCOUNT_ID);

    @Mock
    private StackService stackService;

    @Mock
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @InjectMocks
    private FreeipaSaltPasswordContextProvider underTest;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        lenient().when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
    }

    @Test
    void rotationJobContextResourceCrn() {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(ENV_CRN);

        assertThat(result.get(CUSTOM_JOB).getResourceCrn()).isEqualTo(ENV_CRN);
    }

    @Test
    void rotationJobContextPreValidateJob() throws CloudbreakOrchestratorFailedException {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(ENV_CRN);

        CustomJobRotationContext rotationContext = (CustomJobRotationContext) result.get(CUSTOM_JOB);
        rotationContext.getPreValidateJob().orElseThrow().run();

        verify(stackService).getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID);
        verify(rotateSaltPasswordService).validateRotateSaltPassword(stack);
    }

    @Test
    void rotationJobContextRotationJob() {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(ENV_CRN);

        CustomJobRotationContext rotationContext = (CustomJobRotationContext) result.get(CUSTOM_JOB);
        rotationContext.getRotationJob().orElseThrow().run();

        verify(stackService).getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID);
        verify(rotateSaltPasswordService).rotateSaltPassword(stack);
    }

    @Test
    void secretType() {
        assertThat(underTest.getSecret()).isEqualTo(FreeIpaSecretType.SALT_PASSWORD);
    }

}
