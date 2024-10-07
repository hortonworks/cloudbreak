package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
class DatalakeSaltPasswordRotationContextProviderTest {

    private static final String CRN = "crn";

    @InjectMocks
    private DatalakeSaltPasswordRotationContextProvider underTest;

    @Test
    void rotationJobContext() {
        Map<SecretRotationStep, RotationContext> result = underTest.getContexts(CRN);

        PollerRotationContext rotationContext = (PollerRotationContext) result.get(CLOUDBREAK_ROTATE_POLLING);
        assertThat(rotationContext.getResourceCrn()).isEqualTo(CRN);
        assertThat(rotationContext.getSecretType()).isEqualTo(CloudbreakSecretType.SALT_PASSWORD);
    }

    @Test
    void secretType() {
        assertThat(underTest.getSecret()).isEqualTo(DatalakeSecretType.SALT_PASSWORD);
    }

}
