package com.sequenceiq.cloudbreak.cloud.openstack.securitygroup;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.SecGroupExtension.Rule;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class OpenStackSecurityGroupResourceBuilder extends AbstractOpenStackGroupResourceBuilder {

    private static final int MAX_PORT = 65535;

    private static final int MIN_PORT = 1;

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Group group, Network network, Security security,
            CloudResource resource) {
        try {
            OSClient<?> osClient = createOSClient(auth);
            ComputeSecurityGroupService securityGroupService = osClient.compute().securityGroups();
            SecGroupExtension securityGroup = securityGroupService.create(resource.getName(), "");
            String securityGroupId = securityGroup.getId();
            List<? extends Rule> existingRules = securityGroupService.get(securityGroupId).getRules();
            for (SecurityRule rule : security.getRules()) {
                IPProtocol osProtocol = getProtocol(rule.getProtocol());
                String cidr = rule.getCidr();
                for (PortDefinition portStr : rule.getPorts()) {
                    int from = Integer.parseInt(portStr.getFrom());
                    int to = Integer.parseInt(portStr.getTo());
                    if (!ruleExists(existingRules, osProtocol, cidr, from, to)) {
                        securityGroupService.createRule(createRule(securityGroupId, osProtocol, cidr, from, to));
                    }
                }
            }
            String subnetCidr = network.getSubnet().getCidr();
            if (!ruleExists(existingRules, IPProtocol.TCP, subnetCidr, MIN_PORT, MAX_PORT)) {
                securityGroupService.createRule(createRule(securityGroupId, IPProtocol.TCP, subnetCidr, MIN_PORT, MAX_PORT));
            }
            if (!ruleExists(existingRules, IPProtocol.UDP, subnetCidr, MIN_PORT, MAX_PORT)) {
                securityGroupService.createRule(createRule(securityGroupId, IPProtocol.UDP, subnetCidr, MIN_PORT, MAX_PORT));
            }
            if (!ruleExists(existingRules, IPProtocol.ICMP, "0.0.0.0/0", -1, -1)) {
                securityGroupService.createRule(createRule(securityGroupId, IPProtocol.ICMP, "0.0.0.0/0"));
            }
            return createPersistedResource(resource, group.getName(), securityGroup.getId());
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("SecurityGroup creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource, Network network) {
        try {
            OSClient<?> osClient = createOSClient(auth);
            ActionResponse response = osClient.compute().securityGroups().delete(resource.getReference());
            return checkDeleteResponse(response, resourceType(), auth, resource, "SecurityGroup deletion failed");
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("SecurityGroup deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_SECURITY_GROUP;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    private boolean ruleExists(List<? extends Rule> existingRules, IPProtocol protocol, String cidr, int fromPort, int toPort) {
        return existingRules.stream().anyMatch(r ->
                r.getIPProtocol() == protocol
                        && r.getFromPort() == fromPort
                        && r.getToPort() == toPort
                        && r.getRange() != null
                        && Objects.equals(r.getRange().getCidr(), cidr));
    }

    private Rule createRule(String securityGroupId, IPProtocol protocol, String cidr, int fromPort, int toPort) {
        return Builders.secGroupRule()
                .parentGroupId(securityGroupId)
                .protocol(protocol)
                .cidr(cidr)
                .range(fromPort, toPort)
                .build();
    }

    private Rule createRule(String securityGroupId, IPProtocol protocol, String cidr) {
        return Builders.secGroupRule()
                .parentGroupId(securityGroupId)
                .protocol(protocol)
                .cidr(cidr)
                .build();
    }

    private IPProtocol getProtocol(String protocolStr) {
        return IPProtocol.value(protocolStr);
    }

    @Override
    public int order() {
        return 0;
    }
}
