package com.sequenceiq.freeipa.service.stack;

import org.junit.Assert;
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
        Assert.assertEquals("cluster-proxy.ccmv2.endpoint-unavailable", message);
    }

    @Test
    public void testMessageTransformWhenCcmV1() {
        String message = underTest.transformMessage("cluster-proxy.ccm.endpoint-unavailable", Tunnel.CCM);
        Assert.assertEquals("cluster-proxy.ccm.endpoint-unavailable", message);
    }
}