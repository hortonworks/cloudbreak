package com.sequenceiq.cloudbreak.rotation.context.provider;

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

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class SaltPasswordRotationContextProviderTest {

    private static final String CRN = "crn";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Mock
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @InjectMocks
    private SaltPasswordRotationContextProvider underTest;

    @Mock
    private StackDto stack;

    @BeforeEach
    void setUp() {
        lenient().when(stackDtoService.getByCrn(CRN)).thenReturn(stack);
    }

    @Test
    void rotationJobContextResourceCrn() {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(CRN);

        assertThat(result.get(CUSTOM_JOB).getResourceCrn()).isEqualTo(CRN);
    }

    @Test
    void rotationJobContextPreValidateJob() throws CloudbreakOrchestratorException {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(CRN);

        CustomJobRotationContext rotationContext = (CustomJobRotationContext) result.get(CUSTOM_JOB);
        rotationContext.getPreValidateJob().orElseThrow().run();

        verify(stackDtoService).getByCrn(CRN);
        verify(rotateSaltPasswordValidator).validateRotateSaltPassword(stack);
    }

    @Test
    void rotationJobContextRotationJob() throws CloudbreakOrchestratorException {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(CRN);

        CustomJobRotationContext rotationContext = (CustomJobRotationContext) result.get(CUSTOM_JOB);
        rotationContext.getRotationJob().orElseThrow().run();

        verify(stackDtoService).getByCrn(CRN);
        verify(rotateSaltPasswordService).rotateSaltPassword(stack);
    }

    @Test
    void secretType() {
        assertThat(underTest.getSecret()).isEqualTo(CloudbreakSecretType.SALT_PASSWORD);
    }

}
