package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
public class DatalakeSssdIpaPasswordSecretRotationContextProviderTest {

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private DatalakeSssdIpaPasswordSecretRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getName()).thenReturn("name");
        when(sdxService.getByCrn(any())).thenReturn(sdxCluster);
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("crn");
        assertTrue(contexts.keySet().containsAll(Set.of(CLOUDBREAK_ROTATE_POLLING, FREEIPA_ROTATE_POLLING)));
        assertEquals("name",
                ((PollerRotationContext) contexts.get(CommonSecretRotationStep.FREEIPA_ROTATE_POLLING)).getAdditionalProperties().get("CLUSTER_NAME"));
    }
}
