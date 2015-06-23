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
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SPARK_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SSH;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.STORM;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELIN_UI;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELIN_WEB_SOCKET;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;

public final class NetworkUtils {

    private static List<Port> ports = new ArrayList<>();

    static {
        ports.add(new Port(SSH, "22", "tcp"));
        ports.add(new Port(GATEWAY, "443", "tcp"));
        ports.add(new Port(AMBARI, "8080", "tcp"));
        ports.add(new Port(CONSUL, "8500", "tcp"));
        ports.add(new Port(NAMENODE, "50070", "tcp"));
        ports.add(new Port(RESOURCEMANAGER_WEB, "8088", "tcp"));
        ports.add(new Port(RESOURCEMANAGER_SCHEDULER, "8030", "tcp"));
        ports.add(new Port(RESOURCEMANAGER_IPC, "8050", "tcp"));
        ports.add(new Port(JOB_HISTORY_SERVER, "19888", "tcp"));
        ports.add(new Port(HBASE_MASTER, "60010", "tcp"));
        ports.add(new Port(FALCON, "15000", "tcp"));
        ports.add(new Port(STORM, "8744", "tcp"));
        ports.add(new Port(OOZIE, "11000", "tcp"));
        ports.add(new Port(SPARK_HISTORY_SERVER, "18080", "tcp"));
        ports.add(new Port(CONTAINER_LOGS, "8042", "tcp"));
        ports.add(new Port(ZEPPELIN_WEB_SOCKET, "9996", "tcp"));
        ports.add(new Port(ZEPPELIN_UI, "9995", "tcp"));
        ports.add(new Port(KIBANA, "3080", "tcp"));
        ports.add(new Port(ELASTIC_SEARCH, "9200", "tcp"));
    }

    private NetworkUtils() {
        throw new IllegalStateException();
    }

    public static List<Port> getPortsWithoutAclRules() {
        return ports;
    }

    public static List<Port> getPorts(Optional<Stack> stack) {
        List<Port> result = new ArrayList<>();

        if (stack.isPresent()) {
            Stack stackInstance = stack.get();
            List<EndpointRule> aclRules = createACLRules(stackInstance);
            for (SecurityRule rule : stackInstance.getSecurityGroup().getSecurityRules()) {
                for (String portNumber : rule.getPorts()) {
                    Port port = getPortByPortNumberAndProtocol(portNumber, rule.getProtocol());
                    if (port != null) {
                        result.add(new Port(port.getExposedService(), portNumber, portNumber, rule.getProtocol(), aclRules));
                    }
                }
            }
        } else {
            result.addAll(ports);
        }

        return result;
    }

    private static List<EndpointRule> createACLRules(Stack stack) {
        List<EndpointRule> rules = new LinkedList<>();
        for (SecurityRule rule : stack.getSecurityGroup().getSecurityRules()) {
            rules.add(new EndpointRule(EndpointRule.Action.PERMIT.getText(), rule.getCidr()));
        }
        EndpointRule internalRule = new EndpointRule(EndpointRule.Action.PERMIT.toString(), stack.getNetwork().getSubnetCIDR());
        rules.add(internalRule);
        rules.add(EndpointRule.DENY_RULE);
        return rules;
    }

    private static Port getPortByPortNumberAndProtocol(String portNumber, String protocol) {
        for (Port port : ports) {
            if (portNumber.equals(port.getPort()) && protocol.equals(port.getProtocol())) {
                return port;
            }
        }
        return null;
    }
}
