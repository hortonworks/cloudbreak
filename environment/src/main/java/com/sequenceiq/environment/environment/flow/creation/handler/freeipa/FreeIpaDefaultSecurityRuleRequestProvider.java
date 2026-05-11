package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;

@Service
public class FreeIpaDefaultSecurityRuleRequestProvider {

    protected static final List<String> TCP_PORTS = java.util.List.of("22");

    public List<SecurityRuleRequest> createSecurityRuleRequests(String cidr) {
        SecurityRuleRequest tcpSecurityRuleRequest = new SecurityRuleRequest();
        tcpSecurityRuleRequest.setModifiable(false);
        tcpSecurityRuleRequest.setPorts(TCP_PORTS);
        tcpSecurityRuleRequest.setProtocol(NetworkProtocol.TCP.name());
        tcpSecurityRuleRequest.setSubnet(cidr);
        return List.of(tcpSecurityRuleRequest);
    }
}
