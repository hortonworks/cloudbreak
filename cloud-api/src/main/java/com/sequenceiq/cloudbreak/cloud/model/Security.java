package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.service.network.EndpointRule;

public class Security {

    private List<Subnet> allowedSubnets;

    public Security() {
        this.allowedSubnets = new ArrayList<>();
    }

    public List<Subnet> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void addAllowedSubnet(Subnet subnet) {
        allowedSubnets.add(subnet);
    }

    public List<Port> getPorts() {
        List<EndpointRule> aclRules = createACLRules();
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("SSH", "22", "22", "tcp", aclRules));
        ports.add(new Port("Gateway", "80", "80", "tcp", aclRules));
        ports.add(new Port("Ambari", "8080", "8080", "tcp", aclRules));
        ports.add(new Port("Consul", "8500", "8500", "tcp", aclRules));
        ports.add(new Port("NameNode", "50070", "50070", "tcp", aclRules));
        ports.add(new Port("RM Web", "8088", "8088", "tcp", aclRules));
        ports.add(new Port("RM Scheduler", "8030", "8030", "tcp", aclRules));
        ports.add(new Port("RM IPC", "8050", "8050", "tcp", aclRules));
        ports.add(new Port("Job History Server", "19888", "19888", "tcp", aclRules));
        ports.add(new Port("HBase Master", "60010", "60010", "tcp", aclRules));
        ports.add(new Port("Falcon", "15000", "15000", "tcp", aclRules));
        ports.add(new Port("Storm", "8744", "8744", "tcp", aclRules));
        ports.add(new Port("Oozie", "11000", "11000", "tcp", aclRules));
        ports.add(new Port("Container logs", "8042", "8042", "tcp", aclRules));
        ports.add(new Port("Zeppelin web socket", "9999", "9999", "tcp", aclRules));
        ports.add(new Port("Zeppelin ui", "9998", "9998", "tcp", aclRules));
        return ports;
    }

    public List<String> getRawPorts(final String protocol) {
        List<String> ports = new ArrayList<>();
        for (Port port : getPortsByProtocol(protocol)) {
            ports.add(port.getLocalPort());
        }
        return ports;
    }

    public List<Port> getPortsByProtocol(final String protocol) {
        return FluentIterable.from(getPorts()).filter(new Predicate<Port>() {
            @Override
            public boolean apply(Port port) {
                return protocol.equals(port.getProtocol());
            }
        }).toList();
    }

    private List<EndpointRule> createACLRules() {
        List<EndpointRule> rules = new LinkedList<>();
        for (Subnet net : allowedSubnets) {
            rules.add(new EndpointRule(EndpointRule.Action.PERMIT.getText(), net.getCidr()));
        }
        rules.add(EndpointRule.INTERNAL_RULE);
        rules.add(EndpointRule.DENY_RULE);
        return rules;
    }
}
