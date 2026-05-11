package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaHybridSecurityRuleRequestProviderTest {

    private static final String CIDR = "172.27.0.0/16";

    @InjectMocks
    private FreeIpaHybridSecurityRuleRequestProvider underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testHybridEnvironmentTypeSecurityGroupRequest() {
        List<SecurityRuleRequest> result = underTest.createSecurityRuleRequests(CIDR);

        assertThat(result)
                .hasSize(2);
        assertThat(result.get(0))
                .returns(NetworkProtocol.TCP.name(), SecurityRuleBase::getProtocol)
                .returns(FreeIpaHybridSecurityRuleRequestProvider.TCP_PORTS, SecurityRuleBase::getPorts)
                .returns(CIDR, SecurityRuleBase::getSubnet)
                .returns(false, SecurityRuleBase::isModifiable);
        assertThat(result.get(1))
                .returns(NetworkProtocol.UDP.name(), SecurityRuleBase::getProtocol)
                .returns(FreeIpaHybridSecurityRuleRequestProvider.UDP_PORTS, SecurityRuleBase::getPorts)
                .returns(CIDR, SecurityRuleBase::getSubnet)
                .returns(false, SecurityRuleBase::isModifiable);
    }

}
