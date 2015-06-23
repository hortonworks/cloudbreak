package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;

@Service
public class DefaultSecurityGroupCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSecurityGroupCreator.class);
    private static final String TCP_PROTOCOL = "tcp";

    @Inject
    private SecurityGroupRepository groupRepository;

    @Inject
    private SecurityRuleRepository ruleRepository;


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
        SecurityGroup onlySshAndSsl = createSecurityGroup(user, "only-ssh-and-ssl", "Open ports: 22 (SSH) 443 (HTTPS)");
        SecurityRule sshAndSslRule = createSecurityRule("22,443", onlySshAndSsl);
        onlySshAndSsl.setSecurityRules(new HashSet<>(Arrays.asList(sshAndSslRule)));
        groupRepository.save(onlySshAndSsl);
        ruleRepository.save(sshAndSslRule);
        securityGroups.add(onlySshAndSsl);

        //create default security group which opens all of the known services' ports
        String allPortsOpenDesc = "Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) "
                + "19888(Job history server) 60010(HBase master) 15000(Falcon) 8744(Storm) 11000(Oozie) 18080(Spark HS) 8042(NM Web) "
                + "9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch)";
        SecurityGroup allServicesPort = createSecurityGroup(user, "all-services-port", allPortsOpenDesc);
        SecurityRule allPortsRule = createSecurityRule(concatenateAllPortsKnownByCloudbreak(), allServicesPort);
        allServicesPort.setSecurityRules(new HashSet<>(Arrays.asList(allPortsRule)));
        groupRepository.save(allServicesPort);
        ruleRepository.save(allPortsRule);
        securityGroups.add(allServicesPort);

        return securityGroups;
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

    private String concatenateAllPortsKnownByCloudbreak() {
        StringBuilder builder = new StringBuilder("");
        List<Port> ports = NetworkUtils.getPortsWithoutAclRules();
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
