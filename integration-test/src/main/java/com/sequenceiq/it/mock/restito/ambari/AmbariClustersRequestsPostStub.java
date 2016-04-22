package com.sequenceiq.it.mock.restito.ambari;

import java.util.Collections;
import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.Requests;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariClustersRequestsPostStub extends RestitoStub {
    public static final String PATH = "/clusters/.*/requests";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.POST), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        Requests requests = new Requests("SUCCESSFUL", 100);
        requests.setId(66);

        return Action.composite(Action.ok(), Action.stringContent(convertToJson(Collections.singletonMap("Requests", requests))));
    }
}
