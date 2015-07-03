package com.sequenceiq.cloudbreak.orchestrator.swarm;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;

public class OrchestratorTestUtil {

    public static GatewayConfig gatewayConfig() {
        return new GatewayConfig("10.0.0.0", "/tmp/certs");
    }

    public static Set<Node> generateNodes(int count) {
        Set<Node> nodes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            nodes.add(node(Long.valueOf(i)));
        }
        return nodes;
    }

    public static ExitCriteriaModel exitCriteriaModel() {
        class SimpleExitCriteriaModel extends ExitCriteriaModel {

        }
        return new SimpleExitCriteriaModel();
    }

    public static Node node(Long id) {
        Set<String> strings = new HashSet<>();
        strings.add("df" + id);
        return new Node("10.0.0." + id, "11.0.0." + id, id.toString(), strings);
    }
}
