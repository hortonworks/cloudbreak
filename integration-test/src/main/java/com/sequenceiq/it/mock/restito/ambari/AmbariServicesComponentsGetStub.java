package com.sequenceiq.it.mock.restito.ambari;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.RootServiceComponents;
import com.sequenceiq.it.mock.restito.ambari.model.Services;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariServicesComponentsGetStub extends RestitoStub {
    public static final String PATH = "/services/AMBARI/components/AMBARI_SERVER.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.stringContent(convertToJson(createServices())));
    }

    private Services createServices() {
        RootServiceComponents rootServiceComponents = new RootServiceComponents("2.2.2");
        return new Services(rootServiceComponents);
    }
}
