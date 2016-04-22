package com.sequenceiq.it.mock.restito.ambari;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.mock.restito.CustomRestitoFunction;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.Requests;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariClustersRequestsGetStub extends RestitoStub {

    private enum AmbariClusterStatus {
        STOPPED,
        IN_PROGRESS,
        STARTED
    }

    private AmbariClusterStatus status = AmbariClusterStatus.STOPPED;
    public static final String PATH = "/clusters/.*/requests/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        Action customAction = Action.custom(new CustomRestitoFunction() {
            @Override
            protected String getContent() {
                ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

                Requests requests;
                switch (status) {
                    case STOPPED:
                        requests = new Requests("STARTED", 0);
                        status = AmbariClusterStatus.IN_PROGRESS;
                        break;
                    case IN_PROGRESS:
                        requests = new Requests("STARTED", 50);
                        status = AmbariClusterStatus.STARTED;
                        break;
                    default:
                        requests = new Requests("SUCCESSFUL", 100);
                        break;
                }

                return rootNode.set("Requests", getObjectMapper().valueToTree(requests)).toString();
            }
        });
        return Action.composite(Action.ok(), customAction);
    }
}
