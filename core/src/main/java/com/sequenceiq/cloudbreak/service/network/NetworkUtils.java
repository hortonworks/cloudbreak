package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.api.model.ExposedService.AMBARI;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.ATLAS;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.HIVE_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.JOB_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.NAMENODE;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.RANGER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.RESOURCEMANAGER_WEB;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.SPARK_HISTORY_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.ZEPPELIN_UI;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.ZEPPELIN_WEB_SOCKET;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.EndpointRule;
import com.sequenceiq.cloudbreak.api.model.EndpointRule.Action;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public final class NetworkUtils {

    private static final List<Port> PORTS = new ArrayList<>(30);

    static {
        PORTS.add(new Port(AMBARI, "8080", "tcp"));
        PORTS.add(new Port(NAMENODE, "50070", "tcp"));
        PORTS.add(new Port(RESOURCEMANAGER_WEB, "8088", "tcp"));
        PORTS.add(new Port(JOB_HISTORY_SERVER, "19888", "tcp"));
        PORTS.add(new Port(HIVE_SERVER, "10000", "tcp"));
        PORTS.add(new Port(ATLAS, "21000", "tcp"));
        PORTS.add(new Port(SPARK_HISTORY_SERVER, "18080", "tcp"));
        PORTS.add(new Port(ZEPPELIN_WEB_SOCKET, "9996", "tcp"));
        PORTS.add(new Port(ZEPPELIN_UI, "9995", "tcp"));
        PORTS.add(new Port(RANGER, "6080", "tcp"));
    }

    private NetworkUtils() {
        throw new IllegalStateException();
    }

    public static List<Port> getPortsWithoutAclRules() {
        return PORTS;
    }

    public static List<Port> getPorts(Optional<Stack> stack) {
        List<Port> result = new ArrayList<>();

        if (stack.isPresent()) {
            Stack stackInstance = stack.get();
            List<EndpointRule> aclRules = createACLRules(stackInstance);
            for (InstanceGroup instanceGroup : stackInstance.getInstanceGroups()) {
                for (SecurityRule rule : instanceGroup.getSecurityGroup().getSecurityRules()) {
                    for (String portNumber : rule.getPorts()) {
                        Port port = getPortByPortNumberAndProtocol(portNumber, rule.getProtocol());
                        if (port != null) {
                            result.add(new Port(port.getExposedService(), portNumber, portNumber, rule.getProtocol(), aclRules));
                        }
                    }
                }
            }
        } else {
            result.addAll(PORTS);
        }

        return result;
    }

    public static List<Port> getAllPorts() {
        return new ArrayList<>(PORTS);
    }

    private static List<EndpointRule> createACLRules(Stack stack) {
        List<EndpointRule> rules = new LinkedList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (SecurityRule rule : instanceGroup.getSecurityGroup().getSecurityRules()) {
                rules.add(new EndpointRule(Action.PERMIT.getText(), rule.getCidr()));
            }
        }
        EndpointRule internalRule = new EndpointRule(Action.PERMIT.toString(), stack.getNetwork().getSubnetCIDR());
        rules.add(internalRule);
        rules.add(EndpointRule.DENY_RULE);
        return rules;
    }

    private static Port getPortByPortNumberAndProtocol(String portNumber, String protocol) {
        for (Port port : PORTS) {
            if (portNumber.equals(port.getPort()) && protocol.equals(port.getProtocol())) {
                return port;
            }
        }
        return null;
    }

    public static Port getPortByServiceName(ExposedService exposedService) {
        for (Port port : PORTS) {
            if (port.getExposedService().equals(exposedService)) {
                return port;
            }
        }
        return null;
    }
}
