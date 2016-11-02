package com.sequenceiq.cloudbreak.service.securitygroup;

import static com.sequenceiq.cloudbreak.service.network.ExposedService.GATEWAY;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.HTTPS;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SSH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;

@Service
public class DefaultSecurityGroupCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSecurityGroupCreator.class);

    private static final String TCP_PROTOCOL = "tcp";

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private SecurityGroupRepository groupRepository;

    @Value("${cb.nginx.port:9443}")
    private int nginxPort;

    public Set<SecurityGroup> createDefaultSecurityGroups(CbUser user) {
        Set<SecurityGroup> securityGroups = new HashSet<>();
        Set<SecurityGroup> defaultNetworks = groupRepository.findAllDefaultInAccount(user.getAccount());
        if (defaultNetworks.isEmpty()) {
            LOGGER.info("Creating default security groups and rules.");
            securityGroups = createDefaultSecurityGroupInstances(user);
        }
        return securityGroups;
    }

    private Set<SecurityGroup> createDefaultSecurityGroupInstances(CbUser user) {
        Set<SecurityGroup> securityGroups = new HashSet<>();
        //create default strict security group
        createDefaultStringSecurityGroup(user, securityGroups);
        //create default security group which opens all of the known services' ports
        createDefaultAllKnownServicesSecurityGroup(user, securityGroups);
        return securityGroups;
    }

    private void createDefaultStringSecurityGroup(CbUser user, Set<SecurityGroup> securityGroups) {
        List<Port> strictSecurityGroupPorts = new ArrayList<>();
        strictSecurityGroupPorts.add(new Port(SSH, "22", "tcp"));
        strictSecurityGroupPorts.add(new Port(HTTPS, "443", "tcp"));
        strictSecurityGroupPorts.add(new Port(GATEWAY, Integer.toString(nginxPort), "tcp"));
        String strictSecurityGroupDesc = getPortsOpenDesc(strictSecurityGroupPorts);

        addSecurityGroup(user, securityGroups, "only-ssh-and-ssl", strictSecurityGroupPorts, strictSecurityGroupDesc);
    }

    private void createDefaultAllKnownServicesSecurityGroup(CbUser user, Set<SecurityGroup> securityGroups) {
        //new ArrayList -> otherwise the list will be the static 'ports' list from NetworkUtils and we don't want to add nginx port to 'ports' static list.
        List<Port> portsWithoutAclRules = new ArrayList<>(NetworkUtils.getPortsWithoutAclRules());
        portsWithoutAclRules.add(0, new Port(GATEWAY, Integer.toString(nginxPort), "tcp"));
        String allPortsOpenDesc = getPortsOpenDesc(portsWithoutAclRules);
        addSecurityGroup(user, securityGroups, "all-services-port", portsWithoutAclRules, allPortsOpenDesc);
    }

    private void addSecurityGroup(CbUser user, Set<SecurityGroup> securityGroups, String name, List<Port> securityGroupPorts, String securityGroupDesc) {
        SecurityGroup onlySshAndSsl = createSecurityGroup(user, name, securityGroupDesc);
        SecurityRule sshAndSslRule = createSecurityRule(concatenatePorts(securityGroupPorts), onlySshAndSsl);
        onlySshAndSsl.setSecurityRules(new HashSet<>(Collections.singletonList(sshAndSslRule)));
        securityGroups.add(securityGroupService.create(user, onlySshAndSsl));
    }

    private String getPortsOpenDesc(List<Port> portsWithoutAclRules) {
        StringBuilder allPortsOpenDescBuilder = new StringBuilder();
        allPortsOpenDescBuilder.append("Open ports:");
        for (Port port : portsWithoutAclRules) {
            allPortsOpenDescBuilder.append(" ").append(port.getPort()).append(" (").append(port.getName()).append(")");
        }
        return allPortsOpenDescBuilder.toString();
    }

    private SecurityGroup createSecurityGroup(CbUser user, String name, String description) {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setName(name);
        securityGroup.setOwner(user.getUserId());
        securityGroup.setAccount(user.getAccount());
        securityGroup.setDescription(description);
        securityGroup.setPublicInAccount(true);
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
                builder.append(",");
            }
        }
        return builder.toString();
    }
}
