package com.sequenceiq.it.mock.restito.ambari;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.Clusters;
import com.sequenceiq.it.mock.restito.ambari.model.Hosts;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariClustersGetStub extends RestitoStub {
    public static final String PATH = "/clusters";
    private int serverNumber;

    public AmbariClustersGetStub(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    private ObjectNode createClustersResponse() {
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        List<String> ambariServers = new ArrayList<>();
        for (int i = 1; i <= sizeOfAmbariServers(serverNumber); i++) {
            ambariServers.add("127.0.0." + i);
        }

        rootNode.putObject("hosts")
                .set("Hosts", getObjectMapper().valueToTree(new Hosts(ambariServers, "HEALTHY")));

        ArrayNode items = rootNode.putArray("items");
        items.addObject()
                .set("Clusters", getObjectMapper().valueToTree(new Clusters("ambari_cluster")));

        return rootNode;
    }

    private int sizeOfAmbariServers(int serverNumber) {
        return serverNumber - 1;
    }

    @Override
    public Condition getCondition() {
        return Condition.get(AMBARI_API_ROOT + PATH);
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.stringContent(createClustersResponse().toString()));
    }
}
