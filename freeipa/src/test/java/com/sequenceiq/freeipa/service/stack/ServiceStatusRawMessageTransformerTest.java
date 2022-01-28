package com.sequenceiq.freeipa.service.stack;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
public class ServiceStatusRawMessageTransformerTest {

    @InjectMocks
    private ServiceStatusRawMessageTransformer underTest;

    @Test
    public void testMessageTransformWhenCcmV2() {
        String message = underTest.transformMessage("cluster-proxy.ccm.endpoint-unavailable", Tunnel.CCMV2);
        Assertions.assertEquals("cluster-proxy.ccmv2.endpoint-unavailable", message);
    }

    @Test
    public void testNullMessageTransformWhenCcmV2() {
        String message = underTest.transformMessage(null, Tunnel.CCMV2);
        Assertions.assertEquals(null, message);
    }

    @Test
    public void testMessageTransformWhenCcmV1() {
        String message = underTest.transformMessage("cluster-proxy.ccm.endpoint-unavailable", Tunnel.CCM);
        Assertions.assertEquals("cluster-proxy.ccm.endpoint-unavailable", message);
    }
}