package com.sequenceiq.it.mock.restito.ambari;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariBlueprintsGetStub extends RestitoStub {

    public static final String PATH = "/blueprints/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.putObject("host_groups")
                .putObject("components")
                .putArray("name");

        return Action.composite(Action.ok(), Action.stringContent(rootNode.toString()));
    }
}
