package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.service.network.ExposedService.AMBARI;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.CONSUL;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.CONTAINER_LOGS;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ELASTIC_SEARCH;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.FALCON;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.GATEWAY;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_MASTER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.JOB_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.KIBANA;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.NAMENODE;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.OOZIE;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_IPC;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_SCHEDULER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_WEB;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SSH;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SPARK_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.STORM;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELI_UI;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELI_WEB_SOCKET;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;

public final class NetworkUtils {

    private NetworkUtils() {
        throw new IllegalStateException();
    }

    public static List<Port> getPorts(Optional<Stack> stack) {
        List<EndpointRule> aclRules = new ArrayList<>();
        if (stack.isPresent()) {
            aclRules = createACLRules(stack.get());
        }
        List<Port> ports = new ArrayList<>();
        ports.add(new Port(SSH, "22", "22", "tcp", aclRules));
        ports.add(new Port(GATEWAY, "80", "80", "tcp", aclRules));
        ports.add(new Port(AMBARI, "8080", "8080", "tcp", aclRules));
        ports.add(new Port(CONSUL, "8500", "8500", "tcp", aclRules));
        ports.add(new Port(NAMENODE, "50070", "50070", "tcp", aclRules));
        ports.add(new Port(RESOURCEMANAGER_WEB, "8088", "8088", "tcp", aclRules));
        ports.add(new Port(RESOURCEMANAGER_SCHEDULER, "8030", "8030", "tcp", aclRules));
        ports.add(new Port(RESOURCEMANAGER_IPC, "8050", "8050", "tcp", aclRules));
        ports.add(new Port(JOB_HISTORY_SERVER, "19888", "19888", "tcp", aclRules));
        ports.add(new Port(HBASE_MASTER, "60010", "60010", "tcp", aclRules));
        ports.add(new Port(FALCON, "15000", "15000", "tcp", aclRules));
        ports.add(new Port(STORM, "8744", "8744", "tcp", aclRules));
        ports.add(new Port(OOZIE, "11000", "11000", "tcp", aclRules));
        ports.add(new Port(SPARK_HISTORY_SERVER, "18080", "18080", "tcp", aclRules));
        ports.add(new Port(CONTAINER_LOGS, "8042", "8042", "tcp", aclRules));
        ports.add(new Port(ZEPPELI_WEB_SOCKET, "9996", "9996", "tcp", aclRules));
        ports.add(new Port(ZEPPELI_UI, "9995", "9995", "tcp", aclRules));
        ports.add(new Port(KIBANA, "3080", "3080", "tcp", aclRules));
        ports.add(new Port(ELASTIC_SEARCH, "9200", "9200", "tcp", aclRules));
        return ports;
    }

    public static List<String> getRawPorts(Stack stack, final String protocol) {
        List<String> ports = new ArrayList<>();
        for (Port port : getPortsByProtocol(stack, protocol)) {
            ports.add(port.getLocalPort());
        }
        return ports;
    }

    public static List<Port> getPortsByProtocol(Stack stack, final String protocol) {
        return FluentIterable.from(getPorts(Optional.fromNullable(stack))).filter(new Predicate<Port>() {
            @Override
            public boolean apply(Port port) {
                return protocol.equals(port.getProtocol());
            }
        }).toList();
    }

    private static List<EndpointRule> createACLRules(Stack stack) {
        List<EndpointRule> rules = new LinkedList<>();
        for (Subnet net : stack.getAllowedSubnets()) {
            rules.add(new EndpointRule(EndpointRule.Action.PERMIT.getText(), net.getCidr()));
        }
        rules.add(EndpointRule.INTERNAL_RULE);
        rules.add(EndpointRule.DENY_RULE);
        return rules;
    }
}
