package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.service.network.ExposedService.ACCUMULO_MASTER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ACCUMULO_TSERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.AMBARI;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ATLAS;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.CONTAINER_LOGS;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ELASTIC_SEARCH;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.FALCON;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.HBASE_MASTER_WEB;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.JOB_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.KIBANA;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.NAMENODE;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.OOZIE;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.RANGER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.RESOURCEMANAGER_WEB;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.SPARK_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.STORM;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ZEPPELIN_UI;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.model.EndpointRule;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;

public final class NetworkUtils {

    private static List<Port> ports = new ArrayList<>();

    static {
        ports.add(new Port(AMBARI, "8080", "tcp"));
        ports.add(new Port(NAMENODE, "50070", "tcp"));
        ports.add(new Port(RESOURCEMANAGER_WEB, "8088", "tcp"));
        ports.add(new Port(JOB_HISTORY_SERVER, "19888", "tcp"));
        ports.add(new Port(HBASE_MASTER_WEB, "60010", "tcp"));
        ports.add(new Port(ACCUMULO_MASTER, "9999", "tcp"));
        ports.add(new Port(ACCUMULO_TSERVER, "9997", "tcp"));
        ports.add(new Port(ATLAS, "21000", "tcp"));
        ports.add(new Port(FALCON, "15000", "tcp"));
        ports.add(new Port(STORM, "8744", "tcp"));
        ports.add(new Port(OOZIE, "11000", "tcp"));
        ports.add(new Port(SPARK_HISTORY_SERVER, "18080", "tcp"));
        ports.add(new Port(CONTAINER_LOGS, "8042", "tcp"));
        ports.add(new Port(ZEPPELIN_UI, "9995", "tcp"));
        ports.add(new Port(KIBANA, "3080", "tcp"));
        ports.add(new Port(ELASTIC_SEARCH, "9200", "tcp"));
        ports.add(new Port(RANGER, "6080", "tcp"));
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
