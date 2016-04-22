package com.sequenceiq.it.mock.restito.ambari;

import java.util.Collections;
import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.Hosts;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariClustersHostsGetStub extends RestitoStub {

    public static final String PATH = "/clusters/.*/hosts";
    private int serverNumber;

    public AmbariClustersHostsGetStub(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    private ObjectNode createClustersResponse() {
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ArrayNode items = rootNode.putArray("items");

        for (int i = 1; i <= serverNumber; i++) {
            Hosts hosts = new Hosts(Collections.singletonList("host" + i), "HEALTHY");
            ObjectNode item = items.addObject();
            item.set("Hosts", getObjectMapper().valueToTree(hosts));

            item.putArray("host_components")
                    .addObject()
                        .putArray("HostRoles")
                            .addObject()
                                .put("component_name", "component-name")
                                .put("state", "SUCCESSFUL");
        }
        return rootNode;
    }

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.stringContent(createClustersResponse().toString()));
    }
}
