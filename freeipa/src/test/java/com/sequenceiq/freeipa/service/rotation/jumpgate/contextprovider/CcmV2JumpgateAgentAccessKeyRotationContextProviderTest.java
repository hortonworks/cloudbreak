package com.sequenceiq.freeipa.service.rotation.jumpgate.contextprovider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class CcmV2JumpgateAgentAccessKeyRotationContextProviderTest {

    @InjectMocks
    private CcmV2JumpgateAgentAccessKeyRotationContextProvider underTest;

    @Test
    void testIsApplicable() {
        Stack stack = new Stack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        assertTrue(underTest.isApplicable(stack));
    }
}
