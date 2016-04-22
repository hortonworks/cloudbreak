package com.sequenceiq.it.mock.restito.consul;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class ConsulKeyValuePutStub extends RestitoStub {

    public static final String PATH = "/kv/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.PUT), Condition.matchesUri(Pattern.compile(CONSUL_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(Boolean.TRUE)));
    }
}
