package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
public class DatalakeStackEncryptionKeysRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @InjectMocks
    private DatalakeStackEncryptionKeysRotationContextProvider underTest;

    @Test
    void testIsApplicable() throws JsonProcessingException {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setVariant("AWS_NATIVE_GOV");
        when(sdxCluster.getStackRequest()).thenReturn(JsonUtil.writeValueAsString(stackV4Request));
        assertTrue(underTest.isApplicable(sdxCluster));
    }

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        assertEquals(1, result.size());

        PollerRotationContext pollerRotationContext = (PollerRotationContext) result.get(CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, pollerRotationContext.getResourceCrn());
        assertEquals(CloudbreakSecretType.STACK_ENCRYPTION_KEYS, pollerRotationContext.getSecretType());
    }

    @Test
    void testGetSecret() {
        assertEquals(DatalakeSecretType.STACK_ENCRYPTION_KEYS, underTest.getSecret());
    }
}
