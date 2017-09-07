package com.sequenceiq.cloudbreak.service.securitygroup;

import static com.sequenceiq.cloudbreak.api.model.ExposedService.GATEWAY;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.HTTPS;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.SSH;
import static com.sequenceiq.cloudbreak.common.type.ResourceStatus.DEFAULT_DELETED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.util.NameUtil;

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

    public void createDefaultSecurityGroups(IdentityUser user) {
        Set<SecurityGroup> defaultSecurityGroups = groupRepository.findAllDefaultInAccount(user.getAccount());
        List<String> defSecGroupNames = defaultSecurityGroups.stream()
                .map(g -> g.getStatus() == DEFAULT_DELETED ? NameUtil.cutTimestampPostfix(g.getName()) : g.getName())
                .collect(Collectors.toList());
        for (String platform : PLATFORMS_WITH_SEC_GROUP_SUPPORT) {
            String securityGroupName = "default-" + platform.toLowerCase() + "-only-ssh-and-ssl";
            if (!defSecGroupNames.contains(securityGroupName)) {
                createDefaultStrictSecurityGroup(user, platform, securityGroupName);
            }
        }
    }

    private void createDefaultStrictSecurityGroup(IdentityUser user, String platform, String securityGroupName) {
        List<Port> strictSecurityGroupPorts = new ArrayList<>();
        strictSecurityGroupPorts.add(new Port(SSH, "22", "tcp"));
        strictSecurityGroupPorts.add(new Port(HTTPS, "443", "tcp"));
        strictSecurityGroupPorts.add(new Port(GATEWAY, Integer.toString(nginxPort), "tcp"));
        String strictSecurityGroupDesc = getPortsOpenDesc(strictSecurityGroupPorts);
        addSecurityGroup(user, platform, securityGroupName, strictSecurityGroupPorts, strictSecurityGroupDesc);
    }

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
