package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;

@Service
public class DefaultSecurityGroupCreator {

    private static final String[] PLATFORMS_WITH_SEC_GROUP_SUPPORT = {CloudConstants.AWS, CloudConstants.AZURE, CloudConstants.GCP, CloudConstants.OPENSTACK};

    private static final String TCP_PROTOCOL = "tcp";

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private SecurityGroupRepository groupRepository;

    @Value("${cb.nginx.port:9443}")
    private int nginxPort;

    private void addSecurityGroup(IdentityUser user, String platform, String name, List<Port> securityGroupPorts, String securityGroupDesc) {
        SecurityGroup onlySshAndSsl = createSecurityGroup(user, platform, name, securityGroupDesc);
        SecurityRule sshAndSslRule = createSecurityRule(concatenatePorts(securityGroupPorts), onlySshAndSsl);
        onlySshAndSsl.setSecurityRules(new HashSet<>(Collections.singletonList(sshAndSslRule)));
        securityGroupService.create(user, onlySshAndSsl);
    }

    private String getPortsOpenDesc(List<Port> portsWithoutAclRules) {
        StringBuilder allPortsOpenDescBuilder = new StringBuilder();
        allPortsOpenDescBuilder.append("Open ports:");
        for (Port port : portsWithoutAclRules) {
            allPortsOpenDescBuilder.append(' ').append(port.getPort()).append(" (").append(port.getName()).append(')');
        }
        return allPortsOpenDescBuilder.toString();
    }

    private SecurityGroup createSecurityGroup(IdentityUser user, String platform, String name, String description) {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setName(name);
        securityGroup.setOwner(user.getUserId());
        securityGroup.setAccount(user.getAccount());
        securityGroup.setDescription(description);
        securityGroup.setPublicInAccount(true);
        securityGroup.setCloudPlatform(platform);
        securityGroup.setStatus(ResourceStatus.DEFAULT);
        return securityGroup;
    }

    private SecurityRule createSecurityRule(String ports, SecurityGroup securityGroup) {
        SecurityRule securityRule = new SecurityRule();
        securityRule.setCidr("0.0.0.0/0");
        securityRule.setModifiable(false);
        securityRule.setPorts(ports);
        securityRule.setProtocol(TCP_PROTOCOL);
        securityRule.setSecurityGroup(securityGroup);
        return securityRule;
    }

    private String concatenatePorts(List<Port> ports) {
        StringBuilder builder = new StringBuilder("");
        Iterator<Port> portsIterator = ports.iterator();
        while (portsIterator.hasNext()) {
            Port port = portsIterator.next();
            builder.append(port.getPort());
            if (portsIterator.hasNext()) {
                builder.append(',');
            }
        }
        return builder.toString();
    }
}
