package com.sequenceiq.cloudbreak.cloud.openstack.nativ.group;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackSecurityGroupResourceBuilder extends AbstractOpenStackGroupResourceBuilder {
    private static final int MAX_PORT = 65535;
    private static final int MIN_PORT = 1;

    @Inject
    private OpenStackUtils utils;

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Group group, Network network, Security security,
            CloudResource resource)
            throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            ComputeSecurityGroupService securityGroupService = osClient.compute().securityGroups();
            SecGroupExtension securityGroup = securityGroupService.create(resource.getName(), "");
            String securityGroupId = securityGroup.getId();
            for (SecurityRule rule : security.getRules()) {
                IPProtocol osProtocol = getProtocol(rule.getProtocol());
                String cidr = rule.getCidr();
                for (String portStr : rule.getPorts()) {
                    int port = Integer.valueOf(portStr);
                    securityGroupService.createRule(createRule(securityGroupId, osProtocol, cidr, port, port));
                }
            }
            NeutronNetworkView neutronView = new NeutronNetworkView(network);
            String subnetCidr = neutronView.isExistingSubnet() ? utils.getExistingSubnetCidr(auth, neutronView) : network.getSubnet().getCidr();
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.TCP, subnetCidr, MIN_PORT, MAX_PORT));
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.UDP, subnetCidr, MIN_PORT, MAX_PORT));
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.ICMP, "0.0.0.0/0"));
            return createPersistedResource(resource, group.getName(), securityGroup.getId());
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("SecurityGroup creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
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

    private SecGroupExtension.Rule createRule(String securityGroupId, IPProtocol protocol, String cidr, int fromPort, int toPort) {
        return Builders.secGroupRule()
                .parentGroupId(securityGroupId)
                .protocol(protocol)
                .cidr(cidr)
                .range(fromPort, toPort)
                .build();
    }

    private SecGroupExtension.Rule createRule(String securityGroupId, IPProtocol protocol, String cidr) {
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
