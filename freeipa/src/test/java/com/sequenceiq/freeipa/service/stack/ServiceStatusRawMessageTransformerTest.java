package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("cluster-proxy.ccmv2.endpoint-unavailable", message);
    }

    @Test
    public void testNullMessageTransformWhenCcmV2() {
        String message = underTest.transformMessage(null, Tunnel.CCMV2);
        assertEquals(null, message);
    }

    @Test
    public void testMessageTransformWhenCcmV1() {
        String message = underTest.transformMessage("cluster-proxy.ccm.endpoint-unavailable", Tunnel.CCM);
        assertEquals("cluster-proxy.ccm.endpoint-unavailable", message);
    }
}