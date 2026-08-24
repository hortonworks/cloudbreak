package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
public class DatalakeLUKSVolumePassphraseRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private SdxService sdxService;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private DatalakeLUKSVolumePassphraseRotationContextProvider underTest;

    @Test
    void testIsApplicable() throws JsonProcessingException {
        assertTrue(underTest.isApplicable(mock()));
    }

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(2, result.size());

        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);
        assertEquals(RESOURCE_CRN, customJobRotationContext.getResourceCrn());
        assertTrue(customJobRotationContext.getPreValidateJob().isPresent());

        PollerRotationContext pollerRotationContext = (PollerRotationContext) result.get(CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, pollerRotationContext.getResourceCrn());
        assertEquals(CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE, pollerRotationContext.getSecretType());
    }

    static Stream<Arguments> testPrevalidateJobArguments() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true)
        );
    }

    @MethodSource("testPrevalidateJobArguments")
    @ParameterizedTest
    void testPrevalidateJob(boolean envCrnCanBeRetrieved, boolean secretEncryptionEnabled) {
        when(sdxService.getEnvironmentCrnByResourceCrn(RESOURCE_CRN)).thenReturn(envCrnCanBeRetrieved ? Optional.of(ENVIRONMENT_CRN) : Optional.empty());
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder().withEnableSecretEncryption(secretEncryptionEnabled).build();
        lenient().when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) result.get(CommonSecretRotationStep.CUSTOM_JOB);

        if (!envCrnCanBeRetrieved || !secretEncryptionEnabled) {
            assertThrows(SecretRotationException.class, () -> customJobRotationContext.getPreValidateJob().get().run());
        } else {
            assertDoesNotThrow(() -> customJobRotationContext.getPreValidateJob().get().run());
        }
    }

    @Test
    void testGetSecret() {
        assertEquals(DatalakeSecretType.LUKS_VOLUME_PASSPHRASE, underTest.getSecret());
    }
}
