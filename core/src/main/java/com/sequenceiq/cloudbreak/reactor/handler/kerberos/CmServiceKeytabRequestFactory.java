package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;

@Component
public class CmServiceKeytabRequestFactory {

    public ServiceKeytabRequest create(Stack stack, GatewayConfig primaryGatewayConfig) {
        ServiceKeytabRequest request = new ServiceKeytabRequest();
        request.setEnvironmentCrn(stack.getEnvironmentCrn());
        request.setClusterCrn(stack.getResourceCrn());
        String fqdn = primaryGatewayConfig.getHostname();
        request.setServerHostName(fqdn);
        String hostname = StringUtils.substringBefore(fqdn, ".");
        if (!fqdn.equals(hostname)) {
            request.setServerHostNameAlias(hostname);
        }
        request.setServiceName("CM");
        request.setDoNotRecreateKeytab(Boolean.TRUE);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName("hadoopadminrole-" + stack.getName());
        roleRequest.setPrivileges(Set.of("Service Administrators", "Certificate Administrators", "Host Administrators", "CA Administrator"));
        request.setRoleRequest(roleRequest);
        return request;
    }
}
