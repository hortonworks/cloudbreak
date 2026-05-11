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
class FreeIpaDefaultSecurityRuleRequestProviderTest {

    private static final String CIDR = "172.27.0.0/16";

    @InjectMocks
    private FreeIpaDefaultSecurityRuleRequestProvider underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testPublicCloudEnvironmentTypeSecurityGroupRequest() {
        List<SecurityRuleRequest> result = underTest.createSecurityRuleRequests(CIDR);

        assertThat(result)
                .hasSize(1);
        assertThat(result.getFirst())
                .returns(NetworkProtocol.TCP.name(), SecurityRuleBase::getProtocol)
                .returns(FreeIpaDefaultSecurityRuleRequestProvider.TCP_PORTS, SecurityRuleBase::getPorts)
                .returns(CIDR, SecurityRuleBase::getSubnet)
                .returns(false, SecurityRuleBase::isModifiable);
    }

}
