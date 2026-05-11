package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;

@Service
public class FreeIpaHybridSecurityRuleRequestProvider {

    protected static final List<String> TCP_PORTS =
            List.of("22", "53", "88", "135", "139", "389", "445", "464", "636", "3268-3269", "49152-65535");

    protected static final List<String> UDP_PORTS =
            List.of("53", "88", "123", "138", "389", "464");

    public List<SecurityRuleRequest> createSecurityRuleRequests(String cidr) {
        SecurityRuleRequest tcpSecurityRuleRequest = new SecurityRuleRequest();
        tcpSecurityRuleRequest.setModifiable(false);
        tcpSecurityRuleRequest.setPorts(TCP_PORTS);
        tcpSecurityRuleRequest.setProtocol(NetworkProtocol.TCP.name());
        tcpSecurityRuleRequest.setSubnet(cidr);

        SecurityRuleRequest udpSecurityRuleRequest = new SecurityRuleRequest();
        udpSecurityRuleRequest.setModifiable(false);
        udpSecurityRuleRequest.setPorts(UDP_PORTS);
        udpSecurityRuleRequest.setProtocol(NetworkProtocol.UDP.name());
        udpSecurityRuleRequest.setSubnet(cidr);

        return List.of(tcpSecurityRuleRequest, udpSecurityRuleRequest);
    }
}
